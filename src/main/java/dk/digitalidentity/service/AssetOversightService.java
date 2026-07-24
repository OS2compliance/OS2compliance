package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetOversightDao;
import dk.digitalidentity.dao.ChoiceValueDao;
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
    private final SamlModuleConfiguration samlConfiguration;
    private final AssetOversightDao assetOversightDao;
    private final TaskService taskService;
    private final RelationService relationService;
    private final UserService userService;
	private final ChoiceValueDao choiceValueDao;
	private final DBSOversightGridDao dbsOversightGridDao;


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
			User user = userService.currentUser();
			if (asset.getOversightResponsibleUser() == null) {
				if (asset.getResponsibleUsers() != null && !asset.getResponsibleUsers().isEmpty()) {
					asset.setOversightResponsibleUser(asset.getResponsibleUsers().get(0));
					createOrUpdateAssociatedOversightCheck(asset);
				} else if (user != null) {
					asset.setOversightResponsibleUser(user);
					createOrUpdateAssociatedOversightCheck(asset);
				}
			}
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
        User responsibleUser = oversight.getResponsibleUser();
        taskLog.setResponsibleUserName(responsibleUser.getName());
        taskLog.setResponsibleUserUserId(responsibleUser.getUserId());
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
            task.setNextDeadline(LocalDate.of(2099, 1,1));

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
        task.setResponsibleUsers(Set.of(asset.getOversightResponsibleUser()));
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

    private Task findAssociatedOversightCheck(final Asset asset) {
        return findAssociatedOversightTasks(asset).stream().findFirst().orElse(null);
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
     * oversight's supervision form instead, preferring an open task with the nearest deadline.
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
        final Comparator<Task> byDeadline = Comparator.comparing(Task::getNextDeadline,
            Comparator.nullsLast(Comparator.naturalOrder()));
        return candidates.stream().filter(preferred).filter(t -> !taskService.isTaskDone(t))
            .min(byDeadline)
            .or(() -> candidates.stream().filter(preferred).min(byDeadline))
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
     * Retroactively repairs oversights that were booked onto the wrong task. On a DBS asset every
     * "Tilsyn udført" log belongs to a DBS tilsyn task, but due to the old selection bug (and the
     * earlier {@code seedV41} run that used it) such logs could land on an older tilsyn task on the
     * same asset, leaving the real "DBS tilsyn" task overdue. This moves a misbooked log onto the
     * open DBS tilsyn task so it is correctly registered as done. Idempotent: DBS tasks that already
     * carry a log are left untouched, and no new rows are created. Returns the number of logs moved.
     */
    public int repairMisbookedDbsOversightLogs(final Asset asset) {
        if (!isDbsModel(asset.getSupervisoryModel())) {
            return 0;
        }
        final List<Task> tasks = findAssociatedOversightTasks(asset);
        if (tasks.size() < 2) {
            return 0;
        }
        // Open (not-yet-completed) DBS tilsyn tasks, newest deadline first.
        final List<Task> openDbsTasks = tasks.stream()
            .filter(AssetOversightService::isDbsTask)
            .filter(t -> t.getLogs().isEmpty())
            .sorted(Comparator.comparing(Task::getNextDeadline,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .collect(Collectors.toList());
        if (openDbsTasks.isEmpty()) {
            return 0;
        }
        final String assetLinkSuffix = "/assets/" + asset.getId();
        // Misbooked "Tilsyn udført" logs sitting on OTHER tilsyn tasks of the same asset, newest first.
        final List<TaskLog> strayLogs = tasks.stream()
            .filter(t -> !openDbsTasks.contains(t))
            .flatMap(t -> t.getLogs().stream())
            .filter(l -> "Tilsyn udført".equals(l.getName()))
            .filter(l -> l.getDocumentationLink() != null && l.getDocumentationLink().endsWith(assetLinkSuffix))
            .filter(l -> l.getCompleted() != null)
            .sorted(Comparator.comparing(TaskLog::getCompleted).reversed())
            .collect(Collectors.toCollection(ArrayList::new));

        int moved = 0;
        for (final Task dbsTask : openDbsTasks) {
            // Only move a log that was completed after the DBS task was created — a tilsyn cannot
            // have fulfilled a task that did not yet exist. This protects genuinely older completions.
            final LocalDate createdOn = dbsTask.getCreatedAt() != null
                ? dbsTask.getCreatedAt().toLocalDate() : LocalDate.MIN;
            final Optional<TaskLog> match = strayLogs.stream()
                .filter(l -> !l.getCompleted().isBefore(createdOn))
                .findFirst();
            if (match.isEmpty()) {
                continue;
            }
            final TaskLog logToMove = match.get();
            strayLogs.remove(logToMove);
            final Task oldTask = logToMove.getTask();
            if (oldTask != null) {
                oldTask.getLogs().remove(logToMove);
            }
            logToMove.setTask(dbsTask);
            dbsTask.getLogs().add(logToMove);
            log.info("Moved oversight log id={} (completed {}) from task id={} '{}' to DBS task id={} '{}' on asset id={} '{}'",
                logToMove.getId(), logToMove.getCompleted(),
                oldTask != null ? oldTask.getId() : null, oldTask != null ? oldTask.getName() : null,
                dbsTask.getId(), dbsTask.getName(), asset.getId(), asset.getName());
            moved++;
        }
        return moved;
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
