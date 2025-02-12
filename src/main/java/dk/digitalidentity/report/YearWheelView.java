package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskType;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.digitalidentity.report.XlsUtil.createCell;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@SuppressWarnings("Convert2MethodRef")
public class YearWheelView extends AbstractXlsView {
    private CellStyle style;
    @Override
    protected void buildExcelDocument(final Map<String, Object> model, final Workbook workbook, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Sheet sheet = workbook.createSheet("Årshjul " + LocalDate.now().getYear() + "+");
        createMainHeader(workbook, sheet);

        style = workbook.createCellStyle();
        style.setWrapText(true);

        final CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat((short) 14);

        //noinspection unchecked
        final Map<Task, List<Relatable>> taskMap = (Map<Task, List<Relatable>>) model.get("taskMap");
        final List<TaskLog> taskLogs = (List<TaskLog>) model.get("taskLogs");

        int rowCount = 1;
        for (final Map.Entry<Task, List<Relatable>> entry : taskMap.entrySet()) {
            final Task task = entry.getKey();
            final Row row = sheet.createRow(rowCount++);
            final String relatedAssets = entry.getValue().stream()
                .filter(e -> e.getRelationType() == RelationType.ASSET)
                .map(e -> e.getName())
                .collect(Collectors.joining(","));
            createTaskColumns(row, task, relatedAssets);
            createCell(row, 5, nullSafe(() -> task.getNextDeadline()), dateStyle);
            if (task.getTaskType() == TaskType.TASK) {
                Optional<TaskLog> taskLog = taskLogs.stream().filter(log -> log.getTask() == task).findFirst();
                createCell(row, 10, taskLog.isPresent() ? "Udført" : "", style);
                createCell(row, 11, taskLog.map(tl -> tl.getComment()).orElse(""), style);
                createCell(row, 12, taskLog.map(tl -> tl.getCompleted()).orElse(null), dateStyle);
            } else if (task.getTaskType() == TaskType.CHECK) {
                List<TaskLog> checkTaskLogs = taskLogs.stream()
                    .filter(log -> log.getTask() == task).toList();
                Row logRow = row;
                int cnt = 0;
                for (TaskLog taskLog : checkTaskLogs) {
                    createTaskColumns(logRow, task, relatedAssets);
                    createCell(logRow, 5, nullSafe(() -> taskLog.getDeadline()), dateStyle);
                    createCell(logRow, 10, taskLog.getLocalizedEnums() , style);
                    createCell(logRow, 11, nullSafe(() -> taskLog.getComment(), ""), style);
                    createCell(logRow, 12, nullSafe(() -> taskLog.getCompleted(),  null), dateStyle);
                    cnt++;
                    if (checkTaskLogs.size() < cnt) {
                        logRow = sheet.createRow(rowCount++);
                    }
                }
            }
        }
        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(1, 9 * 256);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        // Date fields needs a little extra as excel shows it as danish format which takes up a bit more space
        sheet.setColumnWidth(5, 11 * 256);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);
        sheet.autoSizeColumn(8);
        sheet.setColumnWidth(9, 85 * 256);
        sheet.autoSizeColumn(10);
        sheet.setColumnWidth(11, 75 * 256);
        sheet.setColumnWidth(12, 11 * 256);
    }

    private void createTaskColumns(final Row row, final Task task, final String relatedAssets) {
        createCell(row, 0, task.getName(), style);
        createCell(row, 1, task.getTaskType().getMessage(), style);
        createCell(row, 2, nullSafe(() -> task.getResponsibleUser().getUserId(), ""), style);
        createCell(row, 3, nullSafe(() -> task.getResponsibleUser().getName(), ""), style);
        createCell(row, 4, nullSafe(() -> task.getResponsibleOu().getName(), ""), style);
        createCell(row, 6, nullSafe(() -> task.getRepetition().getMessage()), style);
        createCell(row, 7, nullSafe(() -> task.getTags().stream().map(Tag::getValue)
            .collect(Collectors.joining(",")), ""), style);
        createCell(row, 8, relatedAssets, style);
        createCell(row, 9, nullSafe(() -> task.getDescription(), ""), style);
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
        createCell(header, 6, "Gentagelse", headerStyle);
        createCell(header, 7, "Tags", headerStyle);
        createCell(header, 8, "Aktiv", headerStyle);
        createCell(header, 9, "Beskrivelse", headerStyle);
        createCell(header, 10, "Resultat", headerStyle);
        createCell(header, 11, "Kommentar til udførsel", headerStyle);
        createCell(header, 12, "Udført", headerStyle);
    }
}
