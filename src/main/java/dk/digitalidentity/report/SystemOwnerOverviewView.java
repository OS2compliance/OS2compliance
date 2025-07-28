package dk.digitalidentity.report;

import dk.digitalidentity.model.dto.enums.StatusColor;
import dk.digitalidentity.report.systemowneroverview.dto.AssetRow;
import dk.digitalidentity.report.systemowneroverview.dto.RegisterRow;
import dk.digitalidentity.report.systemowneroverview.dto.TaskRow;
import dk.digitalidentity.report.systemowneroverview.dto.ThreatAssessmentRow;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
@Log4j2
public class SystemOwnerOverviewView extends AbstractXlsView {

	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {

		createTaskSheet(workbook, model);
		createAssetSheet(workbook, model);
		createRegisterSheet(workbook, model);
		createThreatAssessmentSheet(workbook, model);

		log.info("Finished creating system owner overview spreadsheet");
	}

	private void createTaskSheet(Workbook workbook, Map<String, Object> model) {
		Object taskModel = model.get("tasks");
		if (taskModel instanceof Set<?> rawSet) {
			String sheetName = "Opgaver";

			Map<String, CellValueSetter<TaskRow, Cell>> converterMap = new LinkedHashMap<>();
			converterMap.put("Opgavenavn", (t, c) -> c.setCellValue(t.name()));
			converterMap.put("Aktiv navn", (t, c) -> c.setCellValue(t.assetName()));
			converterMap.put("Type", (t, c) -> c.setCellValue(t.type()));
			converterMap.put("Afdeling", (t, c) -> c.setCellValue(t.ouName()));
			converterMap.put("Deadline", (t, c) -> setCellValueForLocalDate(t.deadline(), c));
			converterMap.put("Gentages", (t, c) -> c.setCellValue(t.repeats()));
			converterMap.put("Status", (t, c) -> setCellValueWithColor(t.status().text(), t.status().color(), c));
			converterMap.put("Tags", (t, c) -> c.setCellValue(t.tags()));

			if (!rawSet.isEmpty() && rawSet.iterator().next() instanceof TaskRow) {
				@SuppressWarnings("unchecked")
				Set<TaskRow> tasks = (Set<TaskRow>) rawSet;
				createSheet(sheetName, tasks.stream().toList(), converterMap, workbook);

			}
			else {
				createEmptySheet(sheetName, converterMap.keySet().stream().toList(), workbook);
			}

		}
	}

	private void createAssetSheet(Workbook workbook, Map<String, Object> model) {
		Object assetModel = model.get("assets");
		if (assetModel instanceof Set<?> rawSet) {
			String sheetName = "Aktiver";

			Map<String, CellValueSetter<AssetRow, Cell>> converterMap = new LinkedHashMap<>();
			converterMap.put("Navn", (t, c) -> c.setCellValue(t.name()));
			converterMap.put("Leverandør", (t, c) -> c.setCellValue(t.supplier()));
			converterMap.put("Type", (t, c) -> c.setCellValue(t.type()));
			converterMap.put("Opdateret", (t, c) -> setCellValueForLocalDate(t.updatedAt(), c));
			converterMap.put("Risikovurdering", (t, c) -> setCellValueWithColor(t.riskAssessment().text(), t.riskAssessment().color(), c));
			converterMap.put("Status", (t, c) -> setCellValueWithColor(t.status().text(), t.status().color(), c));

			if (!rawSet.isEmpty() && rawSet.iterator().next() instanceof AssetRow) {
				@SuppressWarnings("unchecked")
				Set<AssetRow> assets = (Set<AssetRow>) rawSet;
				createSheet(sheetName, assets.stream().toList(), converterMap, workbook);

			}
			else {
				createEmptySheet(sheetName, converterMap.keySet().stream().toList(), workbook);
			}

		}
	}

	private void createRegisterSheet(Workbook workbook, Map<String, Object> model) {
		Object registerModel = model.get("registers");
		if (registerModel instanceof Set<?> rawSet) {
			String sheetName = "Fortegnelser";

			Map<String, CellValueSetter<RegisterRow, Cell>> converterMap = new LinkedHashMap<>();
			converterMap.put("Titel", (t, c) -> c.setCellValue(t.name()));
			converterMap.put("Ansvarlig afdeling", (t, c) -> c.setCellValue(t.responsibleOuName()));
			converterMap.put("Senest redigeret", (t, c) -> setCellValueForLocalDate(t.updatedAt(), c));
			converterMap.put("Konsekvensvurdering", (t, c) -> setCellValueWithColor(t.consequenceEstimate().text(), t.consequenceEstimate().color(), c));
			converterMap.put("Status", (t, c) -> setCellValueWithColor(t.status().text(), t.status().color(), c));

			if (!rawSet.isEmpty() && rawSet.iterator().next() instanceof RegisterRow) {
				@SuppressWarnings("unchecked")
				Set<RegisterRow> registers = (Set<RegisterRow>) rawSet;
				createSheet(sheetName, registers.stream().toList(), converterMap, workbook);

			}
			else {
				createEmptySheet(sheetName, converterMap.keySet().stream().toList(), workbook);
			}

		}
	}

	private void createThreatAssessmentSheet(Workbook workbook, Map<String, Object> model) {
		Object threatAssessmentModel = model.get("threatAssessments");
		if (threatAssessmentModel instanceof Set<?> rawSet) {
			String sheetName = "Risikovurderinger";

			Map<String, CellValueSetter<ThreatAssessmentRow, Cell>> converterMap = new LinkedHashMap<>();
			converterMap.put("Titel", (t, c) -> c.setCellValue(t.name()));
			converterMap.put("Aktiv navn", (t, c) -> c.setCellValue(t.assetName()));
			converterMap.put("Type", (t, c) -> c.setCellValue(t.type()));
			converterMap.put("Fagområde", (t, c) -> c.setCellValue(t.subjectArea()));
			converterMap.put("Risikoejer", (t, c) -> c.setCellValue(t.riskOwner()));
			converterMap.put("Dato", (t, c) -> setCellValueForLocalDate(t.date().toLocalDate(), c));
			converterMap.put("Status", (t, c) -> c.setCellValue(t.status()));
			converterMap.put("Risikovurdering", (t, c) -> setCellValueWithColor(t.riskAssessment().text(), t.riskAssessment().color(), c));


			if (!rawSet.isEmpty() && rawSet.iterator().next() instanceof ThreatAssessmentRow) {
				@SuppressWarnings("unchecked")
				Set<ThreatAssessmentRow> threatAssessments= (Set<ThreatAssessmentRow>) rawSet;
				createSheet(sheetName, threatAssessments.stream().toList(), converterMap, workbook);

			}
			else {
				createEmptySheet(sheetName, converterMap.keySet().stream().toList(), workbook);
			}

		}
	}


	private void createEmptySheet(String name, List<String> headers, Workbook workbook) {
		Sheet sheet = workbook.createSheet(name);
		createHeaderRow(sheet, headers);
	}

	private <T> void createSheet(String name, List<T> objects, Map<String, CellValueSetter<T, Cell>> converterMap, Workbook workbook) {
		Sheet sheet = workbook.createSheet(name);

		// Create header
		List<String> headers = new ArrayList<>(converterMap.keySet());
		createHeaderRow(sheet, headers);

		List<Map.Entry<String, CellValueSetter<T, Cell>>> converterMapEntriesAsList = converterMap.entrySet().stream().toList();

		// Create rows
		for (int rowIndex = 0; rowIndex < objects.size(); rowIndex++) {
			Row row = sheet.createRow(rowIndex+1); // plus one because of header row

			// Create cells based on the specific cell creation function defined in the convertermap
			for (int columnIndex = 0; columnIndex < converterMapEntriesAsList.size(); columnIndex++) {
				Cell cell = row.createCell(columnIndex);
				converterMapEntriesAsList.get(columnIndex).getValue()
						.apply(objects.get(rowIndex), cell);
			}

		}

		// Autosize columns
		for (int i = 0; i < headers.size(); i++) {
			sheet.autoSizeColumn(i);
		}

	}

	private void createHeaderRow(Sheet sheet, List<String> headers) {
		Row headerRow = sheet.createRow(0);
		CellStyle style = headerRow.getSheet().getWorkbook().createCellStyle();

		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderBottom(BorderStyle.MEDIUM);

		Font headerFont = headerRow.getSheet().getWorkbook().createFont();
		headerFont.setBold(true);
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		headerFont.setFontHeightInPoints((short) 14);
		style.setFont(headerFont);

		for (int i = 0; i < headers.size(); i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellStyle(style);
			cell.setCellValue(headers.get(i));
		}

		headerRow.setHeightInPoints((short) 20);
	}

	private void setCellValueWithColor(String text, StatusColor color, Cell cell) {
		cell.setCellValue(text);
		CellStyle style = cell.getSheet().getWorkbook().createCellStyle();
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setFillForegroundColor(stringToColor(color));
		cell.setCellStyle(style);
	}

	private void setCellValueForLocalDate(LocalDate localDate, Cell cell) {
		cell.setCellValue(localDate);
		CellStyle style = cell.getSheet().getWorkbook().createCellStyle();
		style.setDataFormat(cell.getSheet().getWorkbook().getCreationHelper().createDataFormat().getFormat("dd/mm-yyyy"));
		cell.setCellStyle(style);
	}

	private short stringToColor(StatusColor color) {
		return switch (color) {
			case StatusColor.RED -> IndexedColors.RED.getIndex();
			case StatusColor.YELLOW -> IndexedColors.YELLOW.getIndex();
			case StatusColor.GREEN -> IndexedColors.GREEN.getIndex();
			case StatusColor.ORANGE -> IndexedColors.ORANGE.getIndex();
			case StatusColor.LIGHT_GREEN -> IndexedColors.LIGHT_GREEN.getIndex();
			case StatusColor.GREY -> IndexedColors.GREY_25_PERCENT.getIndex();
		};
	}

	@FunctionalInterface
	interface CellValueSetter<T, C> {
		void apply(T one, C two);
	}

}

