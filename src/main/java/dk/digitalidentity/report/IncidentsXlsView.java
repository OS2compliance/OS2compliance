package dk.digitalidentity.report;

import dk.digitalidentity.model.dto.IncidentDTO;
import dk.digitalidentity.model.dto.IncidentFieldResponseDTO;
import dk.digitalidentity.model.entity.IncidentField;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dk.digitalidentity.report.XlsUtil.createCell;

public class IncidentsXlsView extends AbstractXlsView {

    @Override
    protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        //noinspection unchecked
        final List<IncidentDTO> allIncidents = (List<IncidentDTO>) model.get("incidents");
        //noinspection unchecked
        final List<IncidentField> allFields = (List<IncidentField>) model.get("fields");
        final List<IncidentField> sortedFields = allFields.stream().sorted(Comparator.comparing(IncidentField::getSortKey)).toList();
        final LocalDate from = (LocalDate) model.get("from");
        final LocalDate to = (LocalDate) model.get("to");

        final Sheet sheet = workbook.createSheet(sheetName(from, to));
        final CellStyle style = workbook.createCellStyle();

        createMainHeader(workbook, sheet, sortedFields);

        int rowCount = 1;
        for (IncidentDTO incident : allIncidents) {
            final Row row = sheet.createRow(rowCount++);
            createCell(row, 0, incident.getName(), style);
            createCell(row, 1, incident.getDraftText(), style);
            for (int i = 1; i <= sortedFields.size(); i++) {
                final IncidentField field = sortedFields.get(i-1);
                final String columnValue = incident.getResponses().stream()
                    .filter(r -> Objects.equals(r.getFieldId(), field.getId()))
                    .map(IncidentFieldResponseDTO::getAnswerValue)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("");
                createCell(row, i + 1, columnValue, style);
            }
        }

        for (int i = 0; i < allFields.size() + 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Either bound may be absent when the user has not narrowed the range, so the year span is only
     * added when there is one to show.
     */
    private static String sheetName(final LocalDate from, final LocalDate to) {
        if (from == null && to == null) {
            return "Hændelser";
        }
        final String firstYear = from != null ? String.valueOf(from.getYear()) : "";
        final String lastYear = to != null ? String.valueOf(to.getYear()) : "";
        return "Hændelser " + firstYear + "-" + lastYear;
    }

    private void createMainHeader(final Workbook workbook, final Sheet sheet, final List<IncidentField> sortedFields) {
        final Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        final Row header = sheet.createRow(0);
        createCell(header, 0, "Titel", headerStyle);
        createCell(header, 1, "Status", headerStyle);
        for (int i = 1; i <= sortedFields.size(); i++) {
            createCell(header, i + 1, sortedFields.get(i-1).getQuestion(), headerStyle);
        }

    }

}
