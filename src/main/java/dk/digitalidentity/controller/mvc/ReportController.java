package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.report.DocsReportGeneratorComponent;
import dk.digitalidentity.report.ReportISO27002XlsView;
import dk.digitalidentity.report.ReportNSISXlsView;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.TaskService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.Constants.ARTICLE_30_REPORT_TEMPLATE_DOC;
import static dk.digitalidentity.Constants.ISO27001_REPORT_TEMPLATE_DOC;
import static dk.digitalidentity.Constants.ISO27002_REPORT_TEMPLATE_DOC;
import static dk.digitalidentity.Constants.RISK_ASSESSMENT_TEMPLATE_DOC;
import static dk.digitalidentity.report.DocxService.PARAM_RISK_ASSESSMENT_ID;

@Slf4j
@Controller
@RequireUser
@RequestMapping("reports")
public class ReportController {

    @Autowired
    private StandardTemplateDao standardTemplateDao;
    @Autowired
    private TagDao tagDao;
    @Autowired
    private DocsReportGeneratorComponent docsReportGeneratorComponent;
    @Autowired
    private TaskService taskService;

    @GetMapping
    public String reportList(final Model model) {
        model.addAttribute("tags", tagDao.findAll());
        return "reports/index";
    }


    @GetMapping("tags")
    public String tagReport(final Model model, @RequestParam(name = "tag") final Long tagId,
                            @RequestParam(value = "from", required = false) final LocalDate from,
                            @RequestParam(value = "to", required = false) final LocalDate to) {
        final List<Task> attributeValue = taskService.allTasksWithTag(tagId);
        model.addAttribute("tasks", attributeValue);
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
                                                final HttpServletResponse response) {
        if ("article30".equals(identifier)) {
            generateDocument(response, ARTICLE_30_REPORT_TEMPLATE_DOC, "artikel30.docx", Collections.emptyMap());
        } else if ("iso27001".equals(identifier)) {
            generateDocument(response, ISO27001_REPORT_TEMPLATE_DOC, "iso27001.docx", Collections.emptyMap());
        } else if ("iso27002_2022".equals(identifier)) {
            generateDocument(response, ISO27002_REPORT_TEMPLATE_DOC, "iso27002.docx", Collections.emptyMap());
        } else if ("risk".equals(identifier)) {
            generateDocument(response, RISK_ASSESSMENT_TEMPLATE_DOC, "risikovurdering.docx",
                Map.of(PARAM_RISK_ASSESSMENT_ID, "" + riskId));
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private void generateDocument(final HttpServletResponse response, final String inputFilename, final String outputFilename,
                                  final Map<String, String> parameters) {
        try {
            final long start = System.currentTimeMillis();
            try (final XWPFDocument myDocument = docsReportGeneratorComponent.generateDocument(inputFilename, parameters)) {

                // Set the content type and attachment header.
                response.addHeader("Content-disposition", "attachment;filename=" + outputFilename);
                response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

                // Copy the stream to the response's output stream.
                myDocument.write(response.getOutputStream());
                response.flushBuffer();
            }
            final long end = System.currentTimeMillis();
            log.info("Elapsed Time in milli seconds: "+ (end-start));
        } catch (final IOException e) {
            log.error("Unable to generate docx file. ", e);
        }
    }
}
