package dk.digitalidentity.integration.dbs;

import dk.digitalidentity.Constants;
import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.dao.DBSOversightDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DBSOversight;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLink;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.NotifyService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.TaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.ASSOCIATED_INSPECTION_PROPERTY;

@Slf4j
@Service
@RequiredArgsConstructor
public class DBSService {
    private final DBSOversightDao dbsOversightDao;
    private final RelationService relationService;
    private final AssetService assetService;
    private final TaskService taskService;
	private final SettingsService settingsService;
	private final NotifyService notifyService;
	private final OS2complianceConfiguration configuration;

	@Transactional
	public void oversightResponsible() {
		final LocalDate now = LocalDate.now();
		final LocalDate nowPlus30Days = now.plusDays(30);
		final String recipientSetting = settingsService.getString(
				Constants.DBS_OVERSIGHT_RECIPIENT_SETTING, "");

		// Look back to backfillFrom when configured (oversights synced from the DBS platform carry their
		// original publish date as created). Fall back to 10 days so we do not create tasks for everything
		// the first time we activate DBS integration without a configured backfill.
		final LocalDate backfillFrom = configuration.getIntegrations().getDbs().getBackfillFrom();
		final LocalDateTime taskWindowStart = backfillFrom != null
				? backfillFrom.atStartOfDay()
				: LocalDateTime.now().minusDays(10);
		final List<DBSOversight> oversights = dbsOversightDao
				.findByCreatedGreaterThanAndTaskCreatedFalse(taskWindowStart);
		log.debug("Found {} oversights that need a task.", oversights.size());

		for (DBSOversight dbsOversight : oversights) {
			// Auditens egne systemer når platform-syncen har koblet dem; ellers alle leverandørens
			// aktiver (rækker fra før koblingen fandtes). Den brede fallback lagde auditlinks og
			// opgaver på systemer auditen ikke dækker, når en leverandør har flere systemer.
			// List.copyOf: vi laver session-arbejde (queries, saves) inde i løkken, og en flush
			// kan røre Hibernate-collections bag iteratoren - iterér derfor et snapshot, aldrig
			// den levende PersistentSet/Bag (gav ConcurrentModificationException).
			final Collection<DBSAsset> oversightAssets = List.copyOf(!dbsOversight.getAssets().isEmpty()
					? dbsOversight.getAssets()
					: dbsOversight.getSupplier().getAssets());
			log.debug("Oversight {} has {} assigned assets ({}).", dbsOversight.getId(), oversightAssets.size(),
					dbsOversight.getAssets().isEmpty() ? "supplier-wide fallback" : "audit systems");
			boolean anyTaskHandled = false;

			for (DBSAsset dbsAsset : oversightAssets) {

				//Only update/create related task if itsystem status changes to published
				if (dbsAsset.getStatus() != null && dbsAsset.getStatus().equals("published")) {

					List<Relation> assetRelations = relationService.findRelatedToWithType(dbsAsset, RelationType.ASSET);
					log.debug("Found {} related assets.", assetRelations.size());

					List<Asset> assets = assetRelations.stream()
							.map(r -> r.getRelationAType().equals(RelationType.ASSET) ? r.getRelationAId() : r.getRelationBId())
							.map(assetService::findById)
							.filter(Optional::isPresent)
							.map(Optional::get)
							.toList();

					for (Asset asset : assets) {
						// Prioritet 1: manuelt sat tilsynsansvarlig på aktivet. Hjælpeteksten lover
						// at den altid vinder over den globale indstilling - feltet auto-udfyldes
						// derfor ikke længere (se AssetOversightService.setAssetsToDbsOversight).
						Set<User> responsibleUsers = asset.getOversightResponsibleUser() != null
								? Set.of(asset.getOversightResponsibleUser())
								: Set.of();
						String notificationEmail = null;

						// Prioritet 2: global indstilling - rolleopslag eller direkte mail
						if (responsibleUsers.isEmpty() && !recipientSetting.isEmpty()) {
							if (recipientSetting.startsWith("ROLE:")) {
								User fromRole = resolveUserFromRole(asset, recipientSetting);
								responsibleUsers = fromRole != null ? Set.of(fromRole) : Set.of();
							} else {
								notificationEmail = recipientSetting;
							}
						}

						// Prioritet 3: systemansvarlige for aktivet - alle, ikke en vilkårlig første
						if (responsibleUsers.isEmpty() && notificationEmail == null
								&& asset.getManagers() != null && !asset.getManagers().isEmpty()) {
							responsibleUsers = new LinkedHashSet<>(asset.getManagers());
						}

						if (responsibleUsers.isEmpty() && notificationEmail == null) {
							log.warn("Skipping Asset: {} for DBSOversight: {} — no responsible user and no notification email configured.",
									asset.getId(), dbsOversight.getId());
							continue;
						}

						final Set<User> taskResponsibles = responsibleUsers;
						final String taskEmail = notificationEmail;

						// Check if there is an open task already
						relationService.findRelatedToWithType(dbsAsset, RelationType.TASK).stream()
								.map(r -> taskService.findById(r.getRelationAType() == RelationType.TASK ? r.getRelationAId() : r.getRelationBId()))
								.filter(Optional::isPresent)
								.map(Optional::get)
								.filter(t -> t.getTaskType() == TaskType.TASK
										&& !taskService.isTaskDone(t)
										&& t.getName().contains(Constants.DBS_TASK_NAME_MARKER))
								// An asset can carry more than one unfinished DBS task (historic duplicates from back
								// when overdue tasks were skipped), so do not let the relation order decide. Same rule
								// as AssetOversightService uses when booking a completion, so the two never disagree
								// about which task is the live one.
								.max(TaskService.NEWEST_FIRST)
								.ifPresentOrElse((task) -> {
											// Task already exists — add oversight to description
											appendOversightIfAbsent(task, dbsOversight);

											// Unfinished tasks now include overdue ones. Reset the deadline when it has
											// passed, so the new oversight is actionable instead of being appended to a
											// task that is already red and forgotten.
											if (task.getNextDeadline() == null || !task.getNextDeadline().isAfter(now)) {
												task.setNextDeadline(nowPlus30Days);
											}

											// Tasks created before the asset link was introduced carry no
											// ASSOCIATED_INSPECTION_PROPERTY, and AssetOversightService finds oversight
											// tasks through exactly that property. Without it the task can never be
											// completed from the oversight flow, so it would stay unfinished forever and
											// — now that we reuse overdue tasks — be reused forever. Backfill it.
											if (task.getProperties().stream().noneMatch(p -> ASSOCIATED_INSPECTION_PROPERTY.equals(p.getKey()))) {
												task.getProperties().add(Property.builder()
														.key(ASSOCIATED_INSPECTION_PROPERTY)
														.value(asset.getId().toString())
														.entity(task)
														.build());
												log.info("Backfilled missing asset link on DBS task id={} for asset id={}", task.getId(), asset.getId());
											}

											//set link to the folder containing the documents
											String url = "https://www.dbstilsyn.dk/document?area=TILSYNSRAPPORTER&supplierId=" + dbsAsset.getSupplier().getDbsId();
											if (task.getLinks().stream().noneMatch(l -> l.getUrl().equals(url))) {
												task.getLinks().add(new TaskLink(null, url, task));
											}

											addAuditLinkIfAbsent(task, dbsOversight);
										},
										() -> {
											// Create a new task
											Task task = new Task();
											task.setName(getTaskName(asset));
											task.setNextDeadline(nowPlus30Days);
											if (!taskResponsibles.isEmpty()) {
												task.setResponsibleUsers(taskResponsibles);
												task.setNotifyResponsible(true);
											}
											task.setTaskType(TaskType.TASK);
											task.setRepetition(TaskRepetition.NONE);
											task.setDescription(baseDBSTaskDescription(dbsOversight) + dbsOversight.getName());
											Property property = Property.builder()
													.key(ASSOCIATED_INSPECTION_PROPERTY)
													.value(asset.getId().toString())
													.entity(task)
													.build();
											task.getProperties().add(property);
											log.debug("Created task: {} responsible: {}", task.getName(),
													!taskResponsibles.isEmpty()
															? taskResponsibles.stream().map(User::getName).collect(Collectors.joining(", "))
															: "email:" + taskEmail);
											taskService.saveTask(task);

											addAuditLinkIfAbsent(task, dbsOversight);

											relationService.addRelation(task, dbsAsset);
											relationService.addRelation(task, asset);

											if (!taskResponsibles.isEmpty()) {
												notifyService.notifyTaskResponsible(task);
											} else if (taskEmail != null) {
												notifyService.notifyOversightByEmail(task, taskEmail);
											}
										});
						anyTaskHandled = true;
					}
				}
			}

			// Per-oversight-tilstand: sæt og gem ÉN gang efter løkkerne. Det tidligere save per
			// aktiv blev til merge() på en managed entity, som i Hibernate 6 re-wrapper entitetens
			// collections - midt i iterationen af assets-settet ovenfor (CME). Semantikken er
			// uændret: taskCreated sættes kun når mindst ét aktiv reelt fik behandlet en opgave.
			if (anyTaskHandled) {
				dbsOversight.setTaskCreated(true);
				dbsOversightDao.save(dbsOversight);
			}
		}
	}

	private User resolveUserFromRole(Asset asset, String roleSetting) {
		List<? extends User> users = switch (roleSetting) {
			case "ROLE:SYSTEM_OWNER" -> asset.getResponsibleUsers();
			case "ROLE:SYSTEM_RESPONSIBLE" -> asset.getManagers();
			case "ROLE:OPERATION_RESPONSIBLE" -> asset.getOperationResponsibleUsers();
			default -> {
				log.warn("Unknown role setting: {}", roleSetting);
				yield List.of();
			}
		};
		return (users != null && !users.isEmpty()) ? users.get(0) : null;
	}

    // Generate task using name format: ”Leverandør” – ”Aktiv” – DBS Tilsyn
    private static String getTaskName(final Asset asset) {
        return (asset.getSupplier() != null ? (asset.getSupplier().getName() +  " - ") : "") + asset.getName() + " " + Constants.DBS_TASK_NAME_MARKER;
    }

    private static String baseDBSTaskDescription(final DBSOversight dbsOversight) {
        return "Udfør tilsyn af " + dbsOversight.getSupplier().getName() + "\n"
            + "Følgende filer kan findes på DBS-portalen:\n";
    }

	/**
	 * Adds the oversight to the task description, unless it is already listed.
	 * <p>
	 * Under the old integration every line was a distinct file name, so appending unconditionally was
	 * safe. An oversight now covers a whole audit and is reused when DBS moves publishedDate forward,
	 * and the audit keeps its name across a republication - without this guard the same line is
	 * appended again every time.
	 */
	private void appendOversightIfAbsent(final Task task, final DBSOversight oversight) {
		final String description = task.getDescription() != null ? task.getDescription() : "";
		final String name = oversight.getName().trim();
		// The first oversight on a freshly created task is written without the " - " prefix
		// (baseDBSTaskDescription + name), later ones with it - strip the prefix so both forms
		// count as listed. Compare whole lines: contains() would consider "Tilsyn 2026" already
		// listed when the description holds "Tilsyn 2026 opdateret".
		final boolean alreadyListed = description.lines()
				.map(String::trim)
				.map(l -> l.startsWith("- ") ? l.substring(2).trim() : l)
				.anyMatch(name::equals);
		if (alreadyListed) {
			return;
		}
		task.setDescription(description + "\n - " + oversight.getName());
	}

	private void addAuditLinkIfAbsent(Task task, DBSOversight oversight) {
		String auditLink = oversight.getAuditLink();
		if (auditLink != null && !auditLink.isBlank()) {
			if (task.getLinks().stream().noneMatch(l -> l.getUrl().equals(auditLink))) {
				task.getLinks().add(new TaskLink(null, auditLink, task));
			}
		}
	}
}
