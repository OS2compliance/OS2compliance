package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.report.DocsReportGeneratorComponent;
import dk.digitalidentity.report.ReportISO27002XlsView;
import dk.digitalidentity.report.ReportNSISXlsView;
import dk.digitalidentity.security.RequireUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequireUser
@RequestMapping("reports")
public class ReportController {

    @Autowired
    private StandardTemplateDao standardTemplateDao;
    @Autowired
    private DocsReportGeneratorComponent docsReportGeneratorComponent;

    @GetMapping
    public String reportList() {
        return "reports/index";
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
    public @ResponseBody ResponseEntity<?> word(@RequestParam(name = "identifier", required = true) final String identifier, final HttpServletResponse response) {
        if ("article30".equals(identifier)) {
            generateDocument(response, "reports/article30/main.docx", "artikel30.docx");
        } else if ("iso27001".equals(identifier)) {
            generateDocument(response, "reports/ISO27001/ISO27001.docx", "iso27001.docx");
        } else if ("iso27002_2022".equals(identifier)) {
            generateDocument(response, "reports/ISO27002/ISO27002.docx", "iso27002.docx");
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private void generateDocument(final HttpServletResponse response, final String inputFilename, final String outputFilename) {
        try {
            final long start = System.currentTimeMillis();
            try (final XWPFDocument myDocument = docsReportGeneratorComponent.generateDocument(inputFilename)) {

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
