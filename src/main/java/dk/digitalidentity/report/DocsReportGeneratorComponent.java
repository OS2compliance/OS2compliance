package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.StandardTemplate;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class DocsReportGeneratorComponent {
    private final DocxService docxService;

    public DocsReportGeneratorComponent(final DocxService docxService) {
        this.docxService = docxService;
    }

    public XWPFDocument generateDocument(final String inputFileName, final Map<String, String> parameters, StandardTemplate template) throws IOException {
        final XWPFDocument document = docxService.readDocument(inputFileName);
        docxService.replacePlaceHolders(document, parameters, template);
        document.enforceUpdateFields();
        return document;
    }

}
