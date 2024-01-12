package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.RelatableDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskResult;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.MailService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static dk.digitalidentity.util.NullSafe.nullSafe;
import static java.time.temporal.ChronoUnit.DAYS;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Slf4j
@Controller
@RequestMapping("tasks")
@RequireUser
public class TasksController {
    @Autowired
    private TagDao tagDao;
    @Autowired
    private TaskDao taskDao;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private RelatableDao relatableDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private RelationService relationService;
    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private TaskService taskService;
    @Autowired
    private MailService mailService;
    @Autowired
    private Environment environment;


    @GetMapping
    public String tasksList() {
        return "tasks/index";
    }


    @GetMapping("form")
    public String form(final Model model, @RequestParam(name = "id", required = false) final Long id) {
        if (id == null) {
            model.addAttribute("task", new Task());
            model.addAttribute("formId", "taskCreateForm");
            model.addAttribute("formTitle", "Ny opgave");
        } else {
            final Task task = taskDao.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("task", task);
            model.addAttribute("formId", "taskEditForm");
            model.addAttribute("formTitle", "Rediger opgave");
        }
        return "tasks/form";
    }

    @Transactional
    @PostMapping("create")
    public String formCreate(@Valid @ModelAttribute final Task task,
                           @RequestParam(name = "relations", required = false) final List<Long> relations,
                           @RequestParam(name = "taskRiskId", required = false) final Long riskId) {
        final Task savedTask = taskDao.save(task);
        if (relations != null) {
            relations.stream()
                .map(relatedId -> relatableDao.findById(relatedId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relateret entitet ikke fundet")))
                .map(relatable -> Relation.builder()
                        .relationAId(savedTask.getId())
                        .relationAType(savedTask.getRelationType())
                        .relationBId(relatable.getId())
                        .relationBType(relatable.getRelationType())
                        .build())
                .forEach(relation -> relationDao.save(relation));
        }

        if (riskId != null) {
            final Relatable relatable = relatableDao.findById(riskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relateret risikovurdering ikke fundet"));

            final ThreatAssessment threatAssessment = (ThreatAssessment) relatable;
            if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET)) {
                final List<Relatable> relatedAssets = relationService.findAllRelatedTo(threatAssessment).stream()
                    .filter(t -> t.getRelationType().equals(RelationType.ASSET)).toList();
                addRelations(savedTask, relatedAssets);
            }
            else if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.REGISTER)) {
                final List<Relatable> relatedRegisters = relationService.findAllRelatedTo(threatAssessment).stream()
                    .filter(t -> t.getRelationType().equals(RelationType.REGISTER)).toList();
                addRelations(savedTask, relatedRegisters);
            }

            final Relation relation = Relation.builder()
                .relationAId(savedTask.getId())
                .relationAType(savedTask.getRelationType())
                .relationBId(relatable.getId())
                .relationBType(relatable.getRelationType())
                .build();
            relationDao.save(relation);
            return "redirect:/risks/" + riskId;
        }

        return "redirect:/tasks/"+savedTask.getId();
    }

    private void addRelations(final Task savedTask, final List<Relatable> relatables) {
        for (final Relatable relatable : relatables) {
            final Relation relation = Relation.builder()
                .relationAId(savedTask.getId())
                .relationAType(savedTask.getRelationType())
                .relationBId(relatable.getId())
                .relationBType(relatable.getRelationType())
                .build();
            relationDao.save(relation);
        }
    }

    @Transactional
    @PostMapping("edit")
    public String formEdit(@ModelAttribute final Task task) {
        final Task existingTask = taskDao.findById(task.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (calculateCompleted(task)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opgaven er allerede udført");
        }

        if (task.getResponsibleUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en ansvarlig bruger");
        }
        existingTask.setNotifyResponsible(task.getNotifyResponsible());
        existingTask.setDescription(task.getDescription());
        existingTask.setNextDeadline(task.getNextDeadline());
        existingTask.setResponsibleOu(task.getResponsibleOu());
        existingTask.setResponsibleUser(task.getResponsibleUser());

        if (existingTask.getTaskType().equals(TaskType.CHECK)) {
            existingTask.setRepetition(task.getRepetition());
        }

        taskDao.save(existingTask);

        return "redirect:/tasks/" + existingTask.getId();
    }

    record LogDTO(String comment, String description, String documentationLink, String documentName, Long documentId, String performedBy, LocalDate completedDate, LocalDate deadline, long daysAfterDeadline, TaskResult taskResult) {}
    record CompletionFormDTO(@NotNull Long taskId, @NotNull String comment, String documentLink, Long documentRelation, TaskResult taskResult) {}
    @GetMapping("{id}")
    public String form(final Model model, @PathVariable final long id) {
        final Task task = taskDao.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("task", task);
        model.addAttribute("relations", relationService.findRelationsAsListDTO(task, false));
        model.addAttribute("completionForm", new CompletionFormDTO(task.getId(), "", "", null, null));

        if (task.getTaskType().equals(TaskType.TASK)) {
            final boolean completed = calculateCompleted(task);
            model.addAttribute("completed", completed);
            // only tasks of type TASK can be completed and that type will always only have one tasklog as they can only be completed once
            if (completed) {
                final TaskLog taskLog = task.getLogs().stream().findFirst().orElse(null);
                if (taskLog == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                }
                final long daysAfterDeadline = calculateDaysAfterDeadline(taskLog.getDeadline(), taskLog.getCompleted());
                model.addAttribute("taskLog", new LogDTO(taskLog.getComment(), taskLog.getCurrentDescription(), taskLog.getDocumentationLink(), taskLog.getDocument() == null ? null : taskLog.getDocument().getName(), taskLog.getDocument() == null ? null : taskLog.getDocument().getId(), taskLog.getResponsibleUserUserId() + ", " + taskLog.getResponsibleOUName(), taskLog.getCompleted(), taskLog.getDeadline(), daysAfterDeadline, taskLog.getTaskResult()));
            }
        } else if (task.getTaskType().equals(TaskType.CHECK)) {
            final List<LogDTO> taskLogs = new ArrayList<>();
            for (final TaskLog taskLog : task.getLogs()) {
                final long daysAfterDeadline = calculateDaysAfterDeadline(taskLog.getDeadline(), taskLog.getCompleted());
                taskLogs.add(new LogDTO(taskLog.getComment(), taskLog.getCurrentDescription(), taskLog.getDocumentationLink(),
                    taskLog.getDocument() == null ? null : taskLog.getDocument().getName(),
                    taskLog.getDocument() == null ? null : taskLog.getDocument().getId(),
                    taskLog.getResponsibleUserName() + ", " + taskLog.getResponsibleOUName(), taskLog.getCompleted(), taskLog.getDeadline(), daysAfterDeadline, taskLog.getTaskResult()));
            }

            taskLogs.sort(Comparator.comparing(LogDTO :: completedDate).reversed());
            model.addAttribute("taskLogs", taskLogs);
        }

        return "tasks/view";
    }

    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void taskDelete(@PathVariable final String id) {
        final Long lid = Long.valueOf(id);
        relationService.deleteRelatedTo(lid);
        taskDao.deleteById(lid);
    }

    @Transactional
    @PostMapping("complete")
    public String completeTask(@Valid @ModelAttribute final CompletionFormDTO dto) {
        final Task task = taskDao.findById(dto.taskId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (calculateCompleted(task)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opgaven er allerede udført");
        }

        final String userUuid = SecurityUtil.getLoggedInUserUuid();
        final User user = userDao.findById(userUuid).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ingen bruger logget ind");
        }
        if (StringUtils.isEmpty(dto.comment().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal angives en kommentar ved udførsel.");
        }
        final TaskLog taskLog = new TaskLog();
        taskLog.setName(task.getName());
        taskLog.setCompleted(LocalDate.now());
        taskLog.setDeadline(task.getNextDeadline());
        taskLog.setTask(task);
        taskLog.setCurrentDescription(task.getDescription());
        taskLog.setComment(dto.comment());
        taskLog.setResponsibleOUName(nullSafe(() -> task.getResponsibleOu().getName()));
        taskLog.setResponsibleUserName(user.getName());
        taskLog.setResponsibleUserUserId(user.getUserId());
        taskLog.setTaskResult(dto.taskResult());

        if (!StringUtils.isEmpty(dto.documentLink().trim())) {
            taskLog.setDocumentationLink(dto.documentLink());
        }

        if (task.getTaskType().equals(TaskType.TASK)) {
            if (dto.documentRelation() != null) {
                taskLog.setDocument(documentDao.findById(dto.documentRelation()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det valgte dokument kunne ikke findes.")));
            }
        } else {
            task.setNextDeadline(getNextDeadline(task.getNextDeadline(), task.getRepetition()));
        }

        task.getLogs().add(taskLog);
        taskDao.save(task);

        return "redirect:/tasks";
    }

    @GetMapping("{id}/copy")
    public String taskCopyDialog(final Model model, @PathVariable("id") final long id) {
        final Task task = taskDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final List<Relatable> relations = relationService.findAllRelatedTo(task);

        model.addAttribute("task", task);
        model.addAttribute("relations", relations);
        return "tasks/copyForm";
    }

    @Transactional
    @PostMapping("{id}/copy")
    public String performTaskCopyDialog(final Model model,
                                        @PathVariable("id") final long id,
                                        @Valid @ModelAttribute Task taskForm,
                                        @RequestParam(name = "relations", required = false) final List<Long> relations
                                        ) {
        Task task = taskService.copyTask(taskForm);
        setupRelations(task, relations);
        taskDao.save(task);

        if(task != null && !StringUtils.isEmpty(task.getResponsibleUser().getEmail()) && task.getNotifyResponsible()) {
            mailService.sendMessage(task.getResponsibleUser().getEmail(), "Du er tildelt ansvaret for en ny opgave.", newTaskEmail(task));
        }
        return "redirect:/tasks/" + task.getId();
    }


    private void setupRelations(Task task, List<Long> relations) {
        List<Relatable> relatables = relatableDao.findAllById(relations);
        relatables.forEach(r -> relationService.addRelation(r, task));

    }
    private LocalDate getNextDeadline(final LocalDate deadline, final TaskRepetition repetition) {
        if (repetition == null) {
            return deadline;
        }
        return switch (repetition) {
            case MONTHLY -> deadline.plusMonths(1);
            case QUARTERLY -> deadline.plusMonths(3);
            case HALF_YEARLY -> deadline.plusMonths(6);
            case YEARLY -> deadline.plusYears(1);
            case EVERY_SECOND_YEAR -> deadline.plusYears(2);
            case EVERY_THIRD_YEAR -> deadline.plusYears(3);
            default -> deadline;
        };
    }

    private boolean calculateCompleted(final Task task) {
        boolean completedTask = false;
        if (task.getTaskType().equals(TaskType.TASK)) {
            if (!task.getLogs().isEmpty()) {
                completedTask = true;
            }
        }

        return completedTask;
    }

    private long calculateDaysAfterDeadline(final LocalDate deadline, final LocalDate completed) {
        long days = 0;
        if (deadline.isBefore(completed)) {
            days = DAYS.between(deadline, completed);
        }
        return days;
    }

    private String newTaskEmail(Task task) {
        final String url = environment.getProperty("di.saml.sp.baseUrl") + "/tasks/" +  task.getId();
        return "<p>Kære " + task.getResponsibleUser().getName() + "</p>" +
            "<p>Du er blevet tildelt opgaven med navn: \"" + task.getName() +
            "<p>Du kan finde opgaven her: <a href=\"" + url + "\">" + url + "</a>";
    }
}
