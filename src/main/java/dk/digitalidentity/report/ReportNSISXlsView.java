package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.report.XlsUtil.createCell;

public class ReportNSISXlsView extends AbstractXlsView {
    @Override
    protected void buildExcelDocument(final Map<String, Object> model, final Workbook workbook, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final StandardTemplate template = (StandardTemplate) model.get("template");
        final Sheet sheet = workbook.createSheet("NSIS vers. 2.0.2a");
        createMainHeader(workbook, sheet);

        final CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);

        final Document.OutputSettings outputSettings = new Document.OutputSettings();

        // Create data cells
        int rowCount = 1;
        for (final StandardTemplateSection section : template.getStandardTemplateSections()) {
            final List<StandardTemplateSection> sortedList = section.getChildren().stream()
                .sorted(Comparator.comparing(StandardTemplateSection::getSortKey))
                .toList();
            for (final StandardTemplateSection templateSection : sortedList) {
                final Row row = sheet.createRow(rowCount++);
                final String nsisPractice = templateSection.getStandardSection().getNsisPractice();
                final String nsisSmart = templateSection.getStandardSection().getNsisSmart();

                createCell(row, 0, section.getSection(), style);
                createCell(row, 1, section.getDescription(), style);
                createCell(row, 2, templateSection.getSecurityLevel(), style);
                createCell(row, 3, templateSection.getDescription(), style);
                createCell(row, 4, htmlToPlainText(outputSettings, nsisPractice), style);
                createCell(row, 5, htmlToPlainText(outputSettings, nsisSmart), style);
            }
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
    }

    private static String htmlToPlainText(final Document.OutputSettings outputSettings, final String html) {
        if (html == null) {
            return "";
        }
        final Document doc = Jsoup.parse(html);
        outputSettings.prettyPrint(false);
        doc.outputSettings(outputSettings);
        doc.select("br").before("\\n");
        doc.select("p").before("\\n");
        doc.select("li").before("\\n");
        final String str = doc.html().replaceAll("\\\\n", "\n").replaceAll("&nbsp;", " ");
        return Jsoup.clean(str, "",  Safelist.none(), outputSettings);
    }

    private void createMainHeader(final Workbook workbook, final Sheet sheet) {
        final Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        final Row header = sheet.createRow(0);
        createCell(header, 0, "NSIS afsnit", headerStyle);
        createCell(header, 1, "Område", headerStyle);
        createCell(header, 2, "Sikringsniveau", headerStyle);
        createCell(header, 3, "NSIS Krav", headerStyle);
        createCell(header, 4, "Anmelders beskrivelse af opfyldelse (Praksis)", headerStyle);
        createCell(header, 5, "Anmelders beskrivelse af kontrolmål (SMART)", headerStyle);

    }
}
