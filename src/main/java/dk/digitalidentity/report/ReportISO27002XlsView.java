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
import org.apache.poi.ss.util.CellRangeAddress;
import org.jsoup.Jsoup;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.report.XlsUtil.createCell;

public class ReportISO27002XlsView extends AbstractXlsView {

    @Override
    protected void buildExcelDocument(final Map<String, Object> model, final Workbook workbook, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final StandardTemplate template = (StandardTemplate) model.get("template");

        // create excel xls sheet
        final Sheet sheet = workbook.createSheet("Rapport");

        // create header row
        createMainHeader(workbook, sheet);

        // Create data cells
        int rowCount = 1;

        for (final StandardTemplateSection section : template.getStandardTemplateSections()) {
            // create section header row
            createSectionHeader(workbook, sheet, section, rowCount++);

            final List<StandardTemplateSection> sortedList = section.getChildren().stream()
                    .sorted(Comparator.comparingInt(StandardTemplateSection::getSortKey))
                    .toList();

            for (final StandardTemplateSection row : sortedList) {
                final Row courseRow = sheet.createRow(rowCount++);
                final String reason = row.getStandardSection().getReason();

                courseRow.createCell(0).setCellValue(row.getSection());
                courseRow.createCell(1).setCellValue(row.getDescription());
                courseRow.createCell(2).setCellValue(row.getStandardSection().isSelected() ? "Valgt" : "Fravalgt");
                courseRow.createCell(3).setCellValue(reason != null ? Jsoup.parse(reason).text() : "");
                courseRow.createCell(4).setCellValue(row.getStandardSection().getStatus().getMessage());
            }
        }

        format(sheet);
    }

    private void format(final Sheet sheet) {
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
    }

    private void createMainHeader(final Workbook workbook, final Sheet sheet) {
        final String header1 = "Afsnit";
        final String header2 = "Kontrol overskrift";
        final String header3 = "Valg / fravalgt";
        final String header4 = "Begrundelse";
        final String header5 = "Status";

        final Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        final Row header = sheet.createRow(0);
        createCell(header, 0, header1, headerStyle);
        createCell(header, 1, header2, headerStyle);
        createCell(header, 2, header3, headerStyle);
        createCell(header, 3, header4, headerStyle);
        createCell(header, 4, header5, headerStyle);
    }

    private void createSectionHeader(final Workbook workbook, final Sheet sheet, final StandardTemplateSection section, final int row) {
        final CellRangeAddress mergedRegion = new CellRangeAddress(row, row, 0, 4); //4 is the number of columns. WARN That could change
        sheet.addMergedRegion(mergedRegion);

        final Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        final Row header = sheet.createRow(row);
        createCell(header, 0, section.getDescription(), headerStyle);
    }

}
