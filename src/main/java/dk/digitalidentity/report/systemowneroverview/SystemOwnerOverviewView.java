package dk.digitalidentity.report.systemowneroverview;

import dk.digitalidentity.model.dto.enums.StatusColor;
import dk.digitalidentity.report.XlsUtil;
import dk.digitalidentity.report.XlsUtil.CellValueSetter;
import dk.digitalidentity.report.systemowneroverview.dto.AssetRow;
import dk.digitalidentity.report.systemowneroverview.dto.RegisterRow;
import dk.digitalidentity.report.systemowneroverview.dto.TaskRow;
import dk.digitalidentity.report.systemowneroverview.dto.ThreatAssessmentRow;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static dk.digitalidentity.report.XlsUtil.*;

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

			Map<String, XlsUtil.CellValueSetter<RegisterRow, Cell>> converterMap = new LinkedHashMap<>();
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

	private void setCellValueWithColor(String text, StatusColor color, Cell cell) {
		cell.setCellValue(text);
		CellStyle style = cell.getSheet().getWorkbook().createCellStyle();
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setFillForegroundColor(stringToColor(color));
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


}

