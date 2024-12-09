package dk.digitalidentity.controller.mvc;

import com.lowagie.text.DocumentException;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.mapping.IncidentMapper;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.report.DocsReportGeneratorComponent;
import dk.digitalidentity.report.ReportISO27002XlsView;
import dk.digitalidentity.report.ReportNSISXlsView;
import dk.digitalidentity.report.YearWheelView;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.IncidentService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.ThreatAssessmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static dk.digitalidentity.Constants.ARTICLE_30_REPORT_TEMPLATE_DOC;
import static dk.digitalidentity.Constants.ISO27001_REPORT_TEMPLATE_DOC;
import static dk.digitalidentity.Constants.ISO27002_REPORT_TEMPLATE_DOC;
import static dk.digitalidentity.Constants.RISK_ASSESSMENT_TEMPLATE_DOC;
import static dk.digitalidentity.report.DocxService.PARAM_RISK_ASSESSMENT_ID;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;

@Slf4j
@Controller
@RequireUser
@RequestMapping("reports")
@RequiredArgsConstructor
public class ReportController {
    private final RelationService relationService;
    private final StandardTemplateDao standardTemplateDao;
    private final TagDao tagDao;
    private final DocsReportGeneratorComponent docsReportGeneratorComponent;
    private final TaskService taskService;
    private final ThreatAssessmentService threatAssessmentService;
    private final AssetService assetService;
    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;

    @GetMapping
    public String reportList(final Model model) {
        model.addAttribute("tags", tagDao.findAll());
        return "reports/index";
    }

    @GetMapping("incidents")
    public String tagReport(final Model model,
                            @RequestParam(value = "from", required = false) @DateTimeFormat(pattern = "dd/MM-yyyy") final LocalDate from,
                            @RequestParam(value = "to", required = false) @DateTimeFormat(pattern = "dd/MM-yyyy") final LocalDate to) {
        final LocalDateTime fromDT = from != null ? from.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        final LocalDateTime toDT = to != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.of(3000, 1, 1, 0, 0, 0);
        final Page<Incident> allIncidents = incidentService.listIncidents(fromDT, toDT, Pageable.ofSize(1000));
        model.addAttribute("incidents", incidentMapper.toDTOs(allIncidents.getContent()));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "reports/incidentReport";
    }

    @GetMapping("tags")
    public String tagReport(final Model model,
                            @RequestParam(name = "tags") final List<Long> tagIds,
                            @RequestParam(value = "from", required = false) final LocalDate from,
                            @RequestParam(value = "to", required = false) final LocalDate to) {

        //Retrieves tasks and logs for each tag in query
        Set<Pair<Task, List<TaskLog>>> combinedTasksAndLogs = new HashSet<>();
        for (Long tagId : tagIds) {
            final List<Task> attributeValue = taskService.allTasksWithTag(tagId);
            final List<Pair<Task, List<TaskLog>>> tasksAndLogs = attributeValue.stream()
                .map(t -> Pair.of(t, t.getLogs().stream()
                    .sorted(Comparator.comparing(TaskLog::getCreatedAt))
                    .toList()))
                .toList();
            combinedTasksAndLogs.addAll(tasksAndLogs);
        }

        model.addAttribute("tasksAndLogs", combinedTasksAndLogs);
        model.addAttribute("from", from != null ? from : LocalDate.MIN);
        model.addAttribute("to", to != null ? to : LocalDate.MAX);
        return "reports/tagReport";
    }

    @GetMapping("taskLog/{taskId}")
    public String taskLogReport(final Model model, @PathVariable("taskId") final Long taskId,
                                @RequestParam(value = "from", required = false) final LocalDate from,
                                @RequestParam(value = "to", required = false) final LocalDate to) {
        final Task task = taskService.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final List<TaskLog> taskLogs = taskService.logsBetween(task, from, to);
        model.addAttribute("task", task);
        model.addAttribute("taskLogs", taskLogs);
        return "reports/taskLogReport";
    }

    @GetMapping("yearwheel")
    public ModelAndView yearWheel(final HttpServletResponse response) {
        final LocalDate cutOff = LocalDateTime.now().minusYears(1).with(lastDayOfYear()).toLocalDate();
        final Map<Task, List<Relatable>> taskMap = taskService.findAllTasksWithDeadlineAfter(cutOff).stream()
            .collect(Collectors.toMap(t -> t, relationService::findAllRelatedTo));

        response.setContentType("application/ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=\"Aarshjul.xls\"");
        final Map<String, Object> model = new HashMap<>();
        model.put("taskMap", taskMap);

        final List<TaskLog> taskLogs =  taskService.getLogsForTasks(taskMap.keySet().stream().toList());
        model.put("taskLogs", taskLogs);

        return new ModelAndView(new YearWheelView(), model);
    }

    @GetMapping("sheet")
    public ModelAndView sheet(@RequestParam(name = "identifier", required = true) final String identifier, final HttpServletResponse response) {
        final StandardTemplate template = standardTemplateDao.findByIdentifier(identifier);
        if (template == null) {
            log.warn("StandardTemplate for identifier: " + identifier + " not found.");
            return new ModelAndView("redirect:/");
        }

        final Map<String, Object> model = new HashMap<>();
        model.put("template", template);

        response.setContentType("application/ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xls\"");

        View view = null;
        switch (template.getIdentifier()) {
            case "iso27002_2022" -> view = new ReportISO27002XlsView();
            case "nsis_2_0_2a" -> view = new ReportNSISXlsView();
            default -> throw new IllegalArgumentException("Unexpected value: " + template.getIdentifier());
        }

        return new ModelAndView(view, model);
    }

    @GetMapping("word")
    public @ResponseBody ResponseEntity<?> word(@RequestParam(name = "identifier") final String identifier,
                                                @RequestParam(name = "riskId", required = false) final Long riskId,
                                                @RequestParam(name = "type", required = false, defaultValue = "WORD") String type,
                                                final HttpServletResponse response) {
        if ("article30".equals(identifier)) {
            generateDocument(response, ARTICLE_30_REPORT_TEMPLATE_DOC, "artikel30.docx", Collections.emptyMap(), false, null);
        } else if ("iso27001".equals(identifier)) {
            generateDocument(response, ISO27001_REPORT_TEMPLATE_DOC, "iso27001.docx", Collections.emptyMap(), false, null);
        } else if ("iso27002_2022".equals(identifier)) {
            generateDocument(response, ISO27002_REPORT_TEMPLATE_DOC, "iso27002.docx", Collections.emptyMap(), false, null);
        } else if ("risk".equals(identifier)) {
            if (type.equals("WORD")) {
                generateDocument(response, RISK_ASSESSMENT_TEMPLATE_DOC, "risikovurdering.docx",
                    Map.of(PARAM_RISK_ASSESSMENT_ID, "" + riskId), false, riskId);
            } else if (type.equals("PDF")) {
                generateDocument(response, RISK_ASSESSMENT_TEMPLATE_DOC, "risikovurdering.pdf",
                    Map.of(PARAM_RISK_ASSESSMENT_ID, "" + riskId), true, riskId);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("dpia")
    public @ResponseBody ResponseEntity<StreamingResponseBody> dpiaReport(@RequestParam(name = "assetId") final Long assetId,
                                                                          @RequestParam(name = "type", required = false, defaultValue = "PDF") String type,
                                                                          final HttpServletResponse response) throws IOException {
        Asset asset = assetService.findById(assetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (type.equals("PDF")) {
            byte[] byteData = assetService.getDPIAPdf(asset);
            response.addHeader("Content-disposition", "attachment;filename=konsekvensanalyse vedr " + asset.getName() + ".pdf");
            response.setContentType("application/pdf");
            response.getOutputStream().write(byteData);
            response.flushBuffer();
        } else if (type.equals("ZIP")) {
            return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"konsekvensanalyse vedr " + asset.getName() + ".zip\"")
                .body(out -> {
                        var zipOutputStream = new ZipOutputStream(out);

                        // add dpia pdf
                        try {
                            ZipEntry dpiaFile = new ZipEntry("konsekvensanalyse vedr " + asset.getName() + ".pdf");
                            zipOutputStream.putNextEntry(dpiaFile);
                            zipOutputStream.write(assetService.getDPIAPdf(asset));
                        } catch (DocumentException e) {
                            log.warn("Could not generate pdf for dpia for asset with id " + asset.getId() + ". Exeption: "
                                + e.getMessage());
                        }

                        // Add pdfs for selected threatAssessment
                        if (asset.getDpia().getCheckedThreatAssessmentIds() != null) {
                            List<String> selectedIds = Arrays.asList(asset.getDpia().getCheckedThreatAssessmentIds().split(","));
                            for (String threatAssessmentIdAsString : selectedIds) {
                                long threatAssessmentId = Long.parseLong(threatAssessmentIdAsString);
                                ThreatAssessment threatAssessment = threatAssessmentService.findById(threatAssessmentId).orElse(null);
                                if (threatAssessment != null) {
                                    try {
                                        ZipEntry file = new ZipEntry("risikovurdering " + threatAssessment.getName() + ".pdf");
                                        zipOutputStream.putNextEntry(file);
                                        zipOutputStream.write(threatAssessmentService.getThreatAssessmentPdf(threatAssessment));
                                    } catch (DocumentException e) {
                                        log.warn("Could not generate pdf for threat assessment with id " + threatAssessmentId + ". Exeption: "
                                            + e.getMessage());
                                    }
                                }
                            }
                        }

                        zipOutputStream.close();
                    }
                );
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private void generateDocument(final HttpServletResponse response, final String inputFilename, final String outputFilename,
                                  final Map<String, String> parameters, final boolean toPDF, Long riskId) throws ResponseStatusException {
        try {
            final long start = System.currentTimeMillis();
            if (toPDF) {
                ThreatAssessment threatAssessment = threatAssessmentService.findById(riskId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "ThreatAssessment not found"));
                byte[] byteData = threatAssessmentService.getThreatAssessmentPdf(threatAssessment);
                response.addHeader("Content-disposition", "attachment;filename=" + outputFilename);
                response.setContentType("application/pdf");
                response.getOutputStream().write(byteData);
                response.flushBuffer();
            } else {
                try (final XWPFDocument myDocument = docsReportGeneratorComponent.generateDocument(inputFilename, parameters)) {
                    response.addHeader("Content-disposition", "attachment;filename=" + outputFilename);
                    response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    myDocument.write(response.getOutputStream());
                    response.flushBuffer();
                }
            }
            final long end = System.currentTimeMillis();
            log.info("Elapsed Time in milliseconds: " + (end - start));
        } catch (final IOException e) {
            log.error("Unable to generate document. ", e);
        }
    }

}
