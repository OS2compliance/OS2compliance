package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dk.digitalidentity.report.XlsUtil.createCell;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@SuppressWarnings("Convert2MethodRef")
public class YearWheelView extends AbstractXlsView  {
    @Override
    protected void buildExcelDocument(final Map<String, Object> model, final Workbook workbook, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Sheet sheet = workbook.createSheet("Årshjul " + LocalDate.now().getYear() + "+");
        createMainHeader(workbook, sheet);

        final CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);

        final CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat((short) 14);

        //noinspection unchecked
        final Map<Task, List<Relatable>> taskMap = (Map<Task, List<Relatable>>) model.get("taskMap");
        int rowCount = 1;
        for (final Map.Entry<Task, List<Relatable>> entry : taskMap.entrySet()) {
            final Task task = entry.getKey();
            final Row row = sheet.createRow(rowCount++);
            final String relatedAssets = entry.getValue().stream()
                .filter(e -> e.getRelationType() == RelationType.ASSET)
                .map(e -> e.getName())
                .collect(Collectors.joining(","));
            createCell(row, 0, task.getName(), style);
            createCell(row, 1, task.getTaskType().getMessage(), style);
            createCell(row, 2, nullSafe(() -> task.getResponsibleUser().getUserId(), ""), style);
            createCell(row, 3, nullSafe(() -> task.getResponsibleUser().getName(), ""), style);
            createCell(row, 4, nullSafe(() -> task.getResponsibleOu().getName(), ""), style);
            createCell(row, 5, nullSafe(() -> task.getNextDeadline()), dateStyle);
            createCell(row, 6, nullSafe(() -> task.getTags().stream().map(Tag::getValue)
                .collect(Collectors.joining(",")), ""), style);
            createCell(row, 7, relatedAssets, style);
            createCell(row, 8, nullSafe(() -> task.getDescription(), ""), style);
        }
        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(1, 9 * 256);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.setColumnWidth(5, 11 * 256);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);
        sheet.autoSizeColumn(8);
    }


    private void createMainHeader(final Workbook workbook, final Sheet sheet) {
        final Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        final Row header = sheet.createRow(0);
        createCell(header, 0, "Titel", headerStyle);
        createCell(header, 1, "Type", headerStyle);
        createCell(header, 2, "Bruger id", headerStyle);
        createCell(header, 3, "Bruger navn", headerStyle);
        createCell(header, 4, "Afdeling", headerStyle);
        createCell(header, 5, "Deadline", headerStyle);
        createCell(header, 6, "Tags", headerStyle);
        createCell(header, 7, "Aktiv", headerStyle);
        createCell(header, 8, "Beskrivelse", headerStyle);
    }
}
