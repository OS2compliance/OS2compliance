package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetOversightDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.dao.TaskLogDao;
import dk.digitalidentity.dao.grid.DBSOversightGridDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.NextInspection;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.grid.DBSOversightGrid;
import dk.digitalidentity.samlmodule.config.SamlModuleConfiguration;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.ASSOCIATED_INSPECTION_PROPERTY;
import static dk.digitalidentity.Constants.DBS_SUPERVISION_MODEL_IDENTIFIER_PREFIX;
import static dk.digitalidentity.Constants.DBS_TASK_NAME_MARKER;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssetOversightService {
	/** "Parkeret" kontrol: deadlinen skubbes herud når tilsynet drives af DBS i stedet. */
	public static final LocalDate PARKED_DEADLINE = LocalDate.of(2099, 1, 1);

    private final SamlModuleConfiguration samlConfiguration;
    private final AssetOversightDao assetOversightDao;
    private final TaskService taskService;
    private final RelationService relationService;
	private final ChoiceValueDao choiceValueDao;
	private final DBSOversightGridDao dbsOversightGridDao;
	private final TaskLogDao taskLogDao;


    public List<AssetOversight> findByAssetOrderByCreationDateDesc(final Asset asset) {
        return assetOversightDao.findByAssetOrderByCreationDateDesc(asset);
    }

    public AssetOversight create(final AssetOversight oversight) {
        return assetOversightDao.save(oversight);
    }

    public void delete(AssetOversight assetOversight) {
        assetOversightDao.delete(assetOversight);
    }

    public void setAssetsToDbsOversight(final List<Asset> assets) {
        assets.forEach(asset -> {
            asset.setNextInspection(NextInspection.DBS);
            asset.setNextInspectionDate(null);
            asset.setSupervisoryModel(choiceValueDao.findByIdentifier("supervision-model-dbs-123456").orElse(null));
			// Tilsynsansvarlig udfyldes bevidst IKKE: feltet er et manuelt valg der vinder over
			// den globale indstilling (hjælpeteksten) - auto-udfyldning udpegede en vilkårlig
			// person og blokerede indstillingen. Ansvarskæden ligger i DBSService.
			createOrUpdateAssociatedOversightCheck(asset);
        });
    }

    public void createTaskLogForAssociatedTask(final AssetOversight oversight) {
        final Task task = findTaskForOversightCompletion(oversight);
		if (task == null) {
			return;
		}
        final TaskLog taskLog = new TaskLog();
        taskLog.setTask(task);
        taskLog.setName("Tilsyn udført");
		String comment = "Status: " + oversight.getStatus().getMessage();
		if (oversight.getConclusion() != null && !oversight.getConclusion().isBlank()) {
			comment += "\nKonklusion: " + oversight.getConclusion();
		}
		taskLog.setComment(comment);
        taskLog.setCompleted(oversight.getCreationDate());
        taskLog.setDocumentationLink(samlConfiguration.getSp().getBaseUrl() + "/assets/" + oversight.getAsset().getId());
        // assets_oversight.responsible_uuid has no FK constraint, so a deleted user leaves a dangling
        // reference and getResponsibleUser() returns null. Fall back to the asset's oversight
        // responsible rather than leaving the log unattributed — responsibleUserUserId is @NotNull, so
        // an empty log would only trade the NPE for a ConstraintViolationException at flush.
        final User responsibleUser = oversight.getResponsibleUser() != null
            ? oversight.getResponsibleUser()
            : oversight.getAsset().getOversightResponsibleUser();
        if (responsibleUser != null) {
            taskLog.setResponsibleUserName(responsibleUser.getName());
            taskLog.setResponsibleUserUserId(responsibleUser.getUserId());
        } else {
            taskLog.setResponsibleUserName("Ukendt");
            taskLog.setResponsibleUserUserId("");
        }
        taskLog.setDeadline(task.getNextDeadline());
        taskService.completeTask(task, taskLog);
		if (oversight.getNewInspectionDate() != null) {
			task.setNextDeadline(oversight.getNewInspectionDate());
		}
    }

    public Optional<AssetOversight> findById(Long id) {
        return assetOversightDao.findById(id);
    }


    /**
     * If the asset has an inspection date, this will create a new Task of type CHECK.
     */
    public void createOrUpdateAssociatedOversightCheck(final Asset asset) {
        if (asset.getNextInspection() == null) {
            return;
        }

        final Task task = findAssociatedOversightCheck(asset);
        final ChoiceValue supervisoryModel = asset.getSupervisoryModel();
        if (supervisoryModel == null) {
            // No supervisoryModel make sure the associated task is not repeating anymore.
            asset.setNextInspection(null);
            asset.setNextInspectionDate(null);
            if (task != null) {
                setTaskRevisionInterval(asset, task);
            }
        } else {
            if (task != null) {
                updateAssociatedOversightCheck(asset, task);
            } else {
                createAssociatedOversightCheck(asset);
            }
        }
    }

    private void updateAssociatedOversightCheck(final Asset asset, final Task task) {
        setTaskRevisionInterval(asset, task);
        if (asset.getNextInspectionDate() != null) {
            task.setNextDeadline(asset.getNextInspectionDate());
        } else {
            task.setNextDeadline(PARKED_DEADLINE);

        }
        if (asset.getOversightResponsibleUser() != null) {
            task.setResponsibleUsers(Set.of(asset.getOversightResponsibleUser()));
        }
    }

    private void createAssociatedOversightCheck(final Asset asset) {
        if (asset.getNextInspectionDate() == null) {
            return;
        }
        final Task task = new Task();
        task.setTaskType(TaskType.CHECK);
        task.setName("Udfør tilsyn med leverandøren af " + asset.getName());
        task.setCreatedAt(LocalDateTime.now());
        task.setNextDeadline(asset.getNextInspectionDate());
        task.setNotifyResponsible(false);
        // Tilsynsansvarlig er et manuelt valg og kan mangle - Set.of(null) ville kaste NPE
        if (asset.getOversightResponsibleUser() != null) {
            task.setResponsibleUsers(Set.of(asset.getOversightResponsibleUser()));
        }
        task.setDescription("Gå ind på aktivet " + asset.getName() + " og udfør tilsyn.");
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(ASSOCIATED_INSPECTION_PROPERTY)
            .value("" + asset.getId())
            .build()
        );
        setTaskRevisionInterval(asset, task);
        final Task savedTask = taskService.saveTask(task);
        relationService.addRelation(savedTask, asset);
    }

    /**
     * Parkerer den systemskabte kontrol-opgave (deadline 2099, ingen gentagelse) - og intet
     * andet. createOrUpdateAssociatedOversightCheck duer ikke fra opgavejobbet: dens
     * supervisoryModel==null-gren nuller aktivets tilsynsopsætning som sideeffekt.
     */
    public void parkAssociatedOversightCheck(final Asset asset) {
        final Task check = findAssociatedOversightCheck(asset);
        if (check != null && (check.getNextDeadline() == null || check.getNextDeadline().isBefore(PARKED_DEADLINE))) {
            if (asset.getNextInspection() != NextInspection.DBS) {
                // Aktivets opsætning siger manuel kontrol, men DBS leverer tilsyn for det - gør
                // uoverensstemmelsen synlig, så en bevidst ekstra kontrol kan undtages manuelt
                log.warn("Parking oversight check task {} '{}' although asset {} has nextInspection={} - DBS drives the oversight",
                        check.getId(), check.getName(), asset.getId(), asset.getNextInspection());
            } else {
                log.info("Parking oversight check task {} '{}' for asset {} - tilsynet drives af DBS", check.getId(), check.getName(), asset.getId());
            }
            check.setNextDeadline(PARKED_DEADLINE);
            check.setRepetition(TaskRepetition.NONE);
        }
    }

    private Task findAssociatedOversightCheck(final Asset asset) {
        // Kun CHECK: listen rummer også DBS-opgaver (TASK), og en match på tværs ville kunne
        // parkere en frisk DBS-opgave til 2099. Nyeste ved flere.
        return findAssociatedOversightTasks(asset).stream()
            .filter(t -> t.getTaskType() == TaskType.CHECK)
            .max(TaskService.NEWEST_FIRST)
            .orElse(null);
    }

    /**
     * All tasks related to the asset that represent an oversight ("tilsyn") task, i.e. carry the
     * {@code linked_asset} property. This can be both the generic recurring CHECK task and one or
     * more one-shot "DBS tilsyn" tasks.
     */
    private List<Task> findAssociatedOversightTasks(final Asset asset) {
        return relationService.findAllRelatedTo(asset).stream()
            .filter(r -> r.getRelationType() == RelationType.TASK && r.getProperties().stream()
                .anyMatch(p -> ASSOCIATED_INSPECTION_PROPERTY.equals(p.getKey())))
            .map(Task.class::cast)
            .collect(Collectors.toList());
    }

    /**
     * Selects the task an oversight completion should be booked onto. When an asset carries several
     * oversight tasks (e.g. an older generic tilsyn task and a newer "DBS tilsyn" task) the previous
     * naive {@code findFirst()} would book the completion onto whichever came first — usually the
     * older one — leaving the actual DBS tilsyn task overdue. This picks the task matching the
     * oversight's supervision form instead, preferring the newest open one ({@link TaskService#NEWEST_FIRST}
     * — the same rule the DBS import uses when it appends a new oversight to an existing task).
     */
    Task findTaskForOversightCompletion(final AssetOversight oversight) {
        final List<Task> candidates = findAssociatedOversightTasks(oversight.getAsset());
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        final ChoiceValue model = oversight.getSupervisionModel() != null
            ? oversight.getSupervisionModel()
            : oversight.getAsset().getSupervisoryModel();
        final Predicate<Task> preferred = isDbsModel(model)
            ? AssetOversightService::isDbsTask
            : t -> t.getTaskType() == TaskType.CHECK;
        return candidates.stream().filter(preferred).filter(t -> !taskService.isTaskDone(t))
            .max(TaskService.NEWEST_FIRST)
            .or(() -> candidates.stream().filter(preferred).max(TaskService.NEWEST_FIRST))
            .orElseGet(() -> candidates.get(0));
    }

    private static boolean isDbsModel(final ChoiceValue model) {
        return model != null && model.getIdentifier() != null
            && model.getIdentifier().startsWith(DBS_SUPERVISION_MODEL_IDENTIFIER_PREFIX);
    }

    private static boolean isDbsTask(final Task task) {
        return task.getTaskType() == TaskType.TASK
            && task.getName() != null && task.getName().contains(DBS_TASK_NAME_MARKER);
    }

    /**
     * Retroactively repairs oversights that were booked onto the wrong task. Due to the old selection
     * bug the completion path picked whichever tilsyn task came first in relation order, so a
     * registered tilsyn could land on an older task on the same asset — either the generic CHECK task
     * or an older DBS tilsyn task — leaving the task it actually belonged to overdue. This moves such a
     * log onto the open DBS tilsyn task so it is correctly registered as done.
     * <p>
     * Idempotent: DBS tasks that already carry a log are never receivers, no new rows are created, and
     * a donor is only tapped while it keeps a log of its own ({@link #canDonate}). Returns the number
     * of logs moved.
     */
    public int repairMisbookedDbsOversightLogs(final Asset asset) {
        if (!isDbsModel(asset.getSupervisoryModel())) {
            return 0;
        }
        final List<Task> tasks = findAssociatedOversightTasks(asset);
        if (tasks.size() < 2) {
            return 0;
        }
        // Open (not-yet-completed) DBS tilsyn tasks, newest first — same rule as the live selector.
        final List<Task> openDbsTasks = tasks.stream()
            .filter(AssetOversightService::isDbsTask)
            .filter(t -> t.getLogs().isEmpty())
            .sorted(TaskService.NEWEST_FIRST.reversed())
            .collect(Collectors.toList());
        if (openDbsTasks.isEmpty()) {
            return 0;
        }
        final String assetLinkSuffix = "/assets/" + asset.getId();
        // Candidate misbooked "Tilsyn udført" logs on the asset's other tilsyn tasks, newest first.
        // The link suffix and the name are the signature of the oversight flow: a log created by
        // createTaskLogForAssociatedTask, not one a user wrote by completing a task by hand.
        final List<TaskLog> strayLogs = tasks.stream()
            .flatMap(t -> t.getLogs().stream())
            .filter(l -> "Tilsyn udført".equals(l.getName()))
            .filter(l -> l.getDocumentationLink() != null && l.getDocumentationLink().endsWith(assetLinkSuffix))
            .filter(l -> l.getCompleted() != null)
            .sorted(Comparator.comparing(TaskLog::getCompleted).reversed())
            .collect(Collectors.toCollection(ArrayList::new));
        // How many logs each task still holds, so donating never empties a completed task.
        final Map<Long, Integer> remainingLogs = tasks.stream()
            .collect(Collectors.toMap(Task::getId, t -> t.getLogs().size()));

        int moved = 0;
        for (final Task dbsTask : openDbsTasks) {
            // Only move a log that was completed after the DBS task was created — a tilsyn cannot
            // have fulfilled a task that did not yet exist. This protects genuinely older completions
            // (e.g. legitimate pre-DBS checks on the generic CHECK task).
            final LocalDate createdOn = dbsTask.getCreatedAt() != null
                ? dbsTask.getCreatedAt().toLocalDate() : LocalDate.MIN;
            final Optional<TaskLog> match = strayLogs.stream()
                .filter(l -> !l.getCompleted().isBefore(createdOn))
                .filter(l -> canDonate(l.getTask(), dbsTask, remainingLogs))
                .findFirst();
            if (match.isEmpty()) {
                continue;
            }
            final TaskLog logToMove = match.get();
            strayLogs.remove(logToMove);
            final Task oldTask = logToMove.getTask();
            remainingLogs.merge(oldTask.getId(), -1, Integer::sum);
            // Move via a direct FK update, NOT by mutating Task.logs — that collection uses
            // orphanRemoval, so removing the log there would delete it instead of moving it. This
            // leaves the old (typically parked, deadline 2099) CHECK task without the spurious log.
            taskLogDao.reassignTask(logToMove.getId(), dbsTask);
            log.info("Moved oversight log id={} (completed {}) from task id={} '{}' to DBS task id={} '{}' on asset id={} '{}'",
                logToMove.getId(), logToMove.getCompleted(),
                oldTask != null ? oldTask.getId() : null, oldTask != null ? oldTask.getName() : null,
                dbsTask.getId(), dbsTask.getName(), asset.getId(), asset.getName());
            moved++;
        }
        return moved;
    }

    /**
     * Whether {@code donor} may give up an oversight log to {@code receiver}.
     * <p>
     * A non-DBS tilsyn task can always give one up: on a DBS asset every "Tilsyn udført" belongs to a
     * DBS tilsyn task, so a log sitting on the generic CHECK task is misplaced by definition.
     * <p>
     * An older DBS task may also donate — before the selection was fixed, the completion path picked
     * whichever task came first in relation order, so a tilsyn registered after a newer task already
     * existed was booked onto the older one and left the newer overdue. But only if the donor keeps at
     * least one log afterwards. Emptying it would turn a completed task back into an open, overdue one,
     * and we cannot know from the data whether that task's own tilsyn was ever performed — trading one
     * wrong red task for another is not a repair. Those cases are left for a human to decide.
     */
    private static boolean canDonate(final Task donor, final Task receiver, final Map<Long, Integer> remainingLogs) {
        if (donor == null) {
            return false;
        }
        if (!isDbsTask(donor)) {
            return true;
        }
        if (donor.getCreatedAt() == null || receiver.getCreatedAt() == null
            || !donor.getCreatedAt().isBefore(receiver.getCreatedAt())) {
            return false;
        }
        return remainingLogs.getOrDefault(donor.getId(), 0) > 1;
    }

    private void setTaskRevisionInterval(final Asset asset, final Task task) {
        switch(asset.getNextInspection()) {
            case DATE, DBS -> task.setRepetition(TaskRepetition.NONE);
            case MONTH -> task.setRepetition(TaskRepetition.MONTHLY);
            case QUARTER -> task.setRepetition(TaskRepetition.QUARTERLY);
            case HALF_YEAR -> task.setRepetition(TaskRepetition.HALF_YEARLY);
            case YEAR -> task.setRepetition(TaskRepetition.YEARLY);
            case EVERY_2_YEARS -> task.setRepetition(TaskRepetition.EVERY_SECOND_YEAR);
            case EVERY_3_YEARS -> task.setRepetition(TaskRepetition.EVERY_THIRD_YEAR);
            case null -> task.setRepetition(TaskRepetition.NONE);
        }
    }

	public List<DBSOversightGrid> findDBSGridByIds(List<Long> ids) {
		if (ids == null || ids.isEmpty() || !SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			return List.of();
		}

		return dbsOversightGridDao.findAllById(ids);
	}
}
