package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.enums.DeletionProcedure;
import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.report.XlsUtil.createCell;

public class ReportThreatAssessmentXlsView extends AbstractXlsView {

	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook,
			HttpServletRequest request, HttpServletResponse response) {

		final ThreatAssessment threatAssessment = (ThreatAssessment) model.get("threatAssessment");
		if (threatAssessment == null) {
			throw new IllegalArgumentException("ThreatAssessment not found in model");
		}

		// Extract model data
		final ModelData modelData = extractModelData(model);
		final List<Relatable> relations = (List<Relatable>) model.get("relations");
		final Asset riskAsset = (Asset) model.get("riskAsset");
		final Register riskRegister = (Register) model.get("riskRegister");

		// Create styles
		final ExcelStyles styles = createExcelStyles(workbook);

		// Create "Stamdata" sheet
		createStamDataSheet(workbook, threatAssessment, modelData, relations, riskAsset, riskRegister, styles);

		// Create "Trussler" sheet for CustomThreats
		createTrusslerSheet(workbook, threatAssessment, styles);
	}

	private void createStamDataSheet(Workbook workbook, ThreatAssessment threatAssessment,
			ModelData modelData, List<Relatable> relations, Asset riskAsset, Register riskRegister,
			ExcelStyles styles) {

		Sheet sheet = workbook.createSheet("Stamdata");

		// Create header
		createStamDataHeader(sheet, modelData, styles.headerStyle);

		// Get related tasks
		List<Task> riskAssessmentTasks = relations.stream()
				.filter(t -> t.getRelationType() == RelationType.TASK)
				.map(Task.class::cast)
				.toList();

		// Create data row
		createStamDataRow(sheet, threatAssessment, riskAsset, riskRegister, riskAssessmentTasks,
				modelData, styles.normalStyle);

		// Auto-size columns
		autoSizeColumns(sheet, 19);
	}

	private void createTrusslerSheet(Workbook workbook, ThreatAssessment threatAssessment, ExcelStyles styles) {
		Sheet sheet = workbook.createSheet("Trussler");

		// Create header for custom threats
		createTrusslerHeader(sheet, styles.headerStyle);

		// Create data rows for custom threats
		createTrusslerRows(sheet, threatAssessment, styles);

		// Auto-size columns
		autoSizeColumns(sheet, 2);
	}

	private void createStamDataHeader(Sheet sheet, ModelData modelData, CellStyle headerStyle) {
		final Row header = sheet.createRow(0);
		final String[] headers = {
				"Titel", "Kommentarer", "Undertitel", "Tilstede på mødet", "Kritikalitet",
				modelData.customOwnerName != null ? modelData.customOwnerName : "Systemejer",
				modelData.customResponsibleName != null ? modelData.customResponsibleName : "Systemansvarlig",
				modelData.customOperationName != null ? modelData.customOperationName : "Driftsansvarlig",
				"Systemtype", "Formål", "Medtagne konsekvensområder", "Leverandør",
				"Sletteprocedure udarbejdet", "Link til Sletteprocedure", "Samfundskritisk",
				"Hvem har adgang til personoplysningerne", "Hvor mange har adgang til personoplysningerne?",
				"Kategorier af registrerede og typer af personoplysninger", "Opgaver oprettet under risikovurderingen"
		};

		for (int i = 0; i < headers.length; i++) {
			createCell(header, i, headers[i], headerStyle);
		}
	}

	private void createTrusslerHeader(Sheet sheet, CellStyle headerStyle) {
		final Row header = sheet.createRow(0);
		final String[] headers = {
				"Trussel type", "Beskrivelse"
		};

		for (int i = 0; i < headers.length; i++) {
			createCell(header, i, headers[i], headerStyle);
		}
	}

	private void createStamDataRow(Sheet sheet, ThreatAssessment threatAssessment, Asset riskAsset,
			Register riskRegister, List<Task> tasks, ModelData modelData, CellStyle cellStyle) {

		final Row row = sheet.createRow(1);
		int cellNum = 0;

		// Basic threat assessment data
		createCell(row, cellNum++, threatAssessment.getName(), cellStyle);
		createCell(row, cellNum++, safeString(threatAssessment.getComment()), cellStyle);
		createCell(row, cellNum++, getSubHeading(threatAssessment, riskAsset, riskRegister), cellStyle);
		createCell(row, cellNum++, getPresentAtMeetingString(threatAssessment), cellStyle);
		createCell(row, cellNum++, getCriticality(riskAsset, riskRegister), cellStyle);

		// Asset or Register specific data (columns 5-17)
		if (riskAsset != null) {
			fillAssetData(row, cellNum, riskAsset, modelData, cellStyle);
		} else if (riskRegister != null) {
			fillRegisterData(row, cellNum, riskRegister, modelData, cellStyle);
		} else {
			// Fill with empty values
			for (int i = 0; i < 13; i++) {
				if (i == 5) { // Column 10 (risk areas) - even when no asset/register
					createCell(row, cellNum + i, String.join(", ", buildRiskAreas(threatAssessment)), cellStyle);
				} else {
					createCell(row, cellNum + i, "", cellStyle);
				}
			}
		}
		cellNum += 13;

		// Column 18: Tasks
		createCell(row, cellNum, getTasksString(tasks), cellStyle);
	}

	private void createTrusslerRows(Sheet sheet, ThreatAssessment threatAssessment, ExcelStyles styles) {
		if (threatAssessment.getCustomThreats() == null || threatAssessment.getCustomThreats().isEmpty()) {
			// Create empty row with message
			Row emptyRow = sheet.createRow(1);
			createCell(emptyRow, 0, "Ingen tilpassede trusler fundet", styles.normalStyle);
			return;
		}

		int rowNum = 1;
		for (CustomThreat customThreat : threatAssessment.getCustomThreats()) {
			Row row = sheet.createRow(rowNum++);
			int cellNum = 0;

			createCell(row, cellNum++, safeString(customThreat.getThreatType()), styles.normalStyle);
			createCell(row, cellNum++, safeString(customThreat.getDescription()), styles.normalStyle);
		}
	}

	private void fillAssetData(Row row, int startCell, Asset riskAsset, ModelData modelData, CellStyle cellStyle) {
		int cellNum = startCell;

		// Owners (column 5)
		createCell(row, cellNum++, getUsersString(riskAsset.getResponsibleUsers(), "Ikke udfyldt"), cellStyle);

		// Managers (column 6)
		createCell(row, cellNum++, getUsersString(riskAsset.getManagers(), "Ikke udfyldt"), cellStyle);

		// Operation responsible (column 7)
		createCell(row, cellNum++, getUsersString(riskAsset.getOperationResponsibleUsers(), "Ikke udfyldt"), cellStyle);

		// Asset type (column 8)
		String assetType = Optional.ofNullable(riskAsset.getAssetType())
				.map(ChoiceValue::getCaption)
				.orElse("Ikke angivet");
		createCell(row, cellNum++, assetType, cellStyle);

		// Purpose (column 9) - Not available for Asset
		createCell(row, cellNum++, "Ikke udfyldt", cellStyle);

		// Risk areas (column 10) - Will be filled later
		createCell(row, cellNum++, "", cellStyle);

		// Supplier (column 11)
		String supplierName = Optional.ofNullable(riskAsset.getSupplier())
				.map(Relatable::getName)
				.orElse("Ukendt");
		createCell(row, cellNum++, supplierName, cellStyle);

		// Deletion procedure fields (columns 12-13)
		fillDeletionProcedureData(row, cellNum, riskAsset.getDataProcessing(), cellStyle);
		cellNum += 2;

		// Socially critical (column 14)
		createCell(row, cellNum++, riskAsset.isSociallyCritical() ? "Ja" : "Nej", cellStyle);

		// Data access fields (columns 15-17)
		fillDataAccessFields(row, cellNum, modelData, cellStyle);
	}

	private void fillRegisterData(Row row, int startCell, Register riskRegister, ModelData modelData, CellStyle cellStyle) {
		int cellNum = startCell;

		// Owners (column 5)
		createCell(row, cellNum++, getUsersString(riskRegister.getResponsibleUsers(), "Ikke udfyldt"), cellStyle);

		// Columns 6-7: Not applicable for Register
		createCell(row, cellNum++, "", cellStyle);
		createCell(row, cellNum++, "", cellStyle);

		// System type (column 8)
		createCell(row, cellNum++, "Fortegnelse", cellStyle);

		// Purpose (column 9)
		createCell(row, cellNum++, safeString(riskRegister.getPurpose()), cellStyle);

		// Risk areas (column 10) - Will be filled later
		createCell(row, cellNum++, "", cellStyle);

		// Supplier (column 11) - Not applicable for Register
		createCell(row, cellNum++, "", cellStyle);

		// Deletion procedure fields (columns 12-13)
		fillDeletionProcedureData(row, cellNum, riskRegister.getDataProcessing(), cellStyle);
		cellNum += 2;

		// Socially critical (column 14) - Not applicable for Register
		createCell(row, cellNum++, "", cellStyle);

		// Data access fields (columns 15-17)
		fillDataAccessFields(row, cellNum, modelData, cellStyle);
	}

	// Helper methods
	private Set<String> buildRiskAreas(ThreatAssessment threatAssessment) {
		Set<String> result = new HashSet<>();
		if (threatAssessment.isOrganisation()) {
			result.add("Organisationen");
		}
		if (threatAssessment.isRegistered()) {
			result.add("Den registrerede");
		}
		if (threatAssessment.isSociety()) {
			result.add("Samfundet");
		}
		return result;
	}

	private ModelData extractModelData(Map<String, Object> model) {
		return new ModelData(
				(String) model.get("customOwnerName"),
				(String) model.get("customResponsibleName"),
				(String) model.get("customOperationName"),
				(String) model.get("dataAccessPersons"),
				(String) model.get("accessCount"),
				(String) model.get("dataCategories")
		);
	}

	private ExcelStyles createExcelStyles(Workbook workbook) {
		return new ExcelStyles(
				createHeaderStyle(workbook),
				createNormalStyle(workbook),
				createDateStyle(workbook)
		);
	}

	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setColor(IndexedColors.BLACK.getIndex());
		headerFont.setFontName("Arial");
		headerFont.setFontHeightInPoints((short) 11);

		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		addBorders(headerStyle);
		return headerStyle;
	}

	private CellStyle createNormalStyle(Workbook workbook) {
		CellStyle normalStyle = workbook.createCellStyle();
		normalStyle.setWrapText(true);
		normalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		normalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		addBorders(normalStyle);
		return normalStyle;
	}

	private CellStyle createDateStyle(Workbook workbook) {
		CellStyle dateStyle = workbook.createCellStyle();
		CreationHelper createHelper = workbook.getCreationHelper();
		dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));
		dateStyle.setWrapText(true);
		dateStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		dateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		addBorders(dateStyle);
		return dateStyle;
	}

	private void addBorders(CellStyle style) {
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
	}

	private void fillDeletionProcedureData(Row row, int startCell, DataProcessing dataProcessing, CellStyle cellStyle) {
		String deletionProcedure = Optional.ofNullable(dataProcessing)
				.map(DataProcessing::getDeletionProcedure)
				.map(DeletionProcedure::getMessage)
				.orElse("Ikke udfyldt");
		createCell(row, startCell, deletionProcedure, cellStyle);

		String deletionLink = Optional.ofNullable(dataProcessing)
				.map(DataProcessing::getDeletionProcedureLink)
				.orElse("Ikke angivet");
		createCell(row, startCell + 1, deletionLink, cellStyle);
	}

	private void fillDataAccessFields(Row row, int startCell, ModelData modelData, CellStyle cellStyle) {
		createCell(row, startCell, safeString(modelData.dataAccessPersons, "Ikke udfyldt"), cellStyle);
		createCell(row, startCell + 1, safeString(modelData.accessCount, "0"), cellStyle);
		createCell(row, startCell + 2, safeString(modelData.dataCategories, "Ikke udfyldt"), cellStyle);
	}

	private String getUsersString(List<User> users, String defaultValue) {
		if (users == null || users.isEmpty()) {
			return defaultValue;
		}
		String result = users.stream()
				.map(User::getName)
				.filter(name -> name != null && !name.isBlank())
				.collect(Collectors.joining(", "));
		return result.isBlank() ? defaultValue : result;
	}

	private String getPresentAtMeetingString(ThreatAssessment threatAssessment) {
		return Optional.ofNullable(threatAssessment.getPresentAtMeeting())
				.filter(users -> !users.isEmpty())
				.map(users -> users.stream()
						.map(User::getName)
						.filter(name -> name != null && !name.isBlank())
						.collect(Collectors.joining(", ")))
				.orElse("Ingen tilstede");
	}

	private String getTasksString(List<Task> tasks) {
		return Optional.ofNullable(tasks)
				.map(taskList -> taskList.stream()
						.map(Task::getName)
						.filter(name -> name != null && !name.isBlank())
						.collect(Collectors.joining(", ")))
				.orElse("");
	}

	private String safeString(String value) {
		return value != null ? value : "";
	}

	private String safeString(String value, String defaultValue) {
		return (value != null && !value.isBlank()) ? value : defaultValue;
	}

	private void autoSizeColumns(Sheet sheet, int columnCount) {
		for (int i = 0; i < columnCount; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	// Keep existing methods that are still relevant
	private String getSubHeading(final ThreatAssessment threatAssessment, final Asset asset, final Register register) {
		if (asset != null) {
			String owners = getUsersString(asset.getResponsibleUsers(), null);
			if (owners != null) {
				return "Systemejere: " + owners;
			}
		} else if (register != null) {
			String owners = getUsersString(register.getResponsibleUsers(), null);
			if (owners != null) {
				return "Behandlingsansvarlige: " + owners;
			}
		}
		if (threatAssessment.getResponsibleUser() != null) {
			return "Risikoejer: " + threatAssessment.getResponsibleUser().getName();
		}
		return "Risikoejer ikke udfyldt";
	}

	private String getCriticality(final Asset asset, final Register register) {
		StringBuilder sb = new StringBuilder();
		if (asset != null) {
			sb.append("Systemet er: ");
			sb.append(asset.getCriticality() != null ? asset.getCriticality().getMessage() : "Ikke udfyldt");
			sb.append(" | Nødplan: ");
			sb.append(asset.getEmergencyPlanLink() != null && !asset.getEmergencyPlanLink().isEmpty() ?
					asset.getEmergencyPlanLink() : "Ikke udfyldt");
		} else if (register != null) {
			sb.append("Behandlingsaktiviteten er: ");
			sb.append(register.getCriticality() != null ? register.getCriticality().getMessage() : "Ikke udfyldt");
			sb.append(" | Nødplan: ");
			sb.append(register.getEmergencyPlanLink() != null && !register.getEmergencyPlanLink().isEmpty() ?
					register.getEmergencyPlanLink() : "Ikke udfyldt");
		}
		return asset == null && register == null ? "Ikke angivet" : sb.toString();
	}

	// Records
	private record ModelData(String customOwnerName, String customResponsibleName, String customOperationName, String dataAccessPersons, String accessCount, String dataCategories) {}
	private record ExcelStyles(CellStyle headerStyle, CellStyle normalStyle, CellStyle dateStyle) {}
}