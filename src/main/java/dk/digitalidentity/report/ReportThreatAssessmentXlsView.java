package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.util.Map;

import static dk.digitalidentity.integration.kitos.KitosConstants.*;
import static dk.digitalidentity.report.XlsUtil.createCell;

@Component
public class ReportThreatAssessmentXlsView extends AbstractXlsView {

	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		final ThreatAssessment threatAssessment = (ThreatAssessment) model.get("threatAssessment");
		final SettingsService settingsService = (SettingsService) model.get("settingsService");

		if (threatAssessment == null) {
			throw new IllegalArgumentException("ThreatAssessment not found in model");
		}

		Sheet sheet = workbook.createSheet("ThreatAssessment");
		Font headerFont = createExcelFont(workbook);
		CellStyle cellStyle = setSheetStyle(workbook, headerFont);

		createHeader(workbook, sheet, cellStyle, settingsService);

		// TODO: The headers are done, pull the data into the excel
		inputThreatAssessment(workbook, sheet, cellStyle, threatAssessment);
	}

	private void inputThreatAssessment(Workbook workbook, Sheet sheet, CellStyle cellStyle, ThreatAssessment threatAssessment) {

	}

	private void createHeader(Workbook workbook, Sheet sheet, CellStyle headerStyle, SettingsService settingsService) {
		final Row header = sheet.createRow(0);

		createCell(header, 0, "Titel", headerStyle);
		createCell(header, 1, "Kommentarer", headerStyle);
		createCell(header, 2, "Undertitel", headerStyle);
		createCell(header, 3, "Tilstede på mødet", headerStyle);
		createCell(header, 4, "Kritikalitet", headerStyle);
		createCell(header, 5, settingsService.findBySettingKey(KITOS_OWNER_ROLE_SETTING_INPUT_FIELD_NAME).getSettingValue(), headerStyle);
		createCell(header, 6, settingsService.findBySettingKey(KITOS_RESPONSIBLE_ROLE_SETTING_INPUT_FIELD_NAME).getSettingValue(), headerStyle);
		createCell(header, 7, settingsService.findBySettingKey(KITOS_OPERATION_RESPONSIBLE_ROLE_SETTING_INPUT_FIELD_NAME).getSettingValue(), headerStyle);
		createCell(header, 8, "Systemtype", headerStyle);
		createCell(header, 9, "Formål", headerStyle);
		createCell(header, 10, "Medtagne konsekvensområder", headerStyle);
		createCell(header, 11, "Leverandør", headerStyle);
		createCell(header, 12, "Sletteprocedure udarbejdet", headerStyle);
		createCell(header, 13, "Link til Sletteprocedure", headerStyle);
		createCell(header, 14, "Samfundskritisk", headerStyle);
		createCell(header, 15, "Hvem har adgang til personoplysningerne", headerStyle);
		createCell(header, 16, "Hvor mange har adgang til personoplysningerne?", headerStyle);
		createCell(header, 17, "Kategorier af registrerede og typer af personoplysninger", headerStyle);

		// Auto-size all columns
		for (int i = 0; i < 18; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	private static Font createExcelFont(Workbook workbook) {
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setColor(IndexedColors.BLACK.getIndex());
		headerFont.setFontName("Arial");
		headerFont.setFontHeightInPoints((short) 11);
		return headerFont;
	}

	private static CellStyle setSheetStyle(Workbook workbook, Font headerFont) {
		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setWrapText(true);
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return headerStyle;
	}
}