package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.enums.DeletionProcedure;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.service.model.ThreatDTO;
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
import org.springframework.web.servlet.view.document.AbstractXlsxView;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.report.XlsUtil.createCell;

public class ReportThreatAssessmentXlsView extends AbstractXlsxView {

	private static final int RISK_AREAS_COLUMN_OFFSET = 5;
	private static final int EMPTY_COLUMNS_COUNT = 13;

	// Records
	private record ModelData(String customOwnerName, String customResponsibleName, String customOperationName, String dataAccessPersons, String accessCount, String dataCategories) {}
	private record ExcelStyles(CellStyle headerStyle, CellStyle normalStyle, CellStyle dateStyle) {}

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

		final Map<String, List<ThreatDTO>> threatList = (Map<String, List<ThreatDTO>>) model.get("threatList");
		final boolean showFullMeasureDescription = (boolean) model.get("showFullMeasureDescription");
		createThreatsSheet(workbook, threatList, showFullMeasureDescription, styles);
	}

	private void createStamDataSheet(Workbook workbook, ThreatAssessment threatAssessment,
			ModelData modelData, List<Relatable> relations, Asset riskAsset, Register riskRegister,
			ExcelStyles styles) {

		Sheet sheet = workbook.createSheet("Stamdata");

		// Create header
		int columnCount = createBaseDataHeader(sheet, modelData, styles.headerStyle);

		// Get related tasks
		List<Task> riskAssessmentTasks = relations.stream()
				.filter(t -> t.getRelationType() == RelationType.TASK)
				.map(Task.class::cast)
				.toList();

		// Create data row
		createBaseDataRow(sheet, threatAssessment, riskAsset, riskRegister, riskAssessmentTasks,
				modelData, styles.normalStyle);

		autoSizeColumns(sheet, columnCount);
	}

	private int createBaseDataHeader(Sheet sheet, ModelData modelData, CellStyle headerStyle) {
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
		int columnCount = headers.length;

		for (int i = 0; i < columnCount; i++) {
			createCell(header, i, headers[i], headerStyle);
		}

		return columnCount;
	}

	private void createBaseDataRow(Sheet sheet, ThreatAssessment threatAssessment, Asset riskAsset,
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
			cellNum = fillAssetData(row, cellNum, riskAsset, threatAssessment, modelData, cellStyle);
		} else if (riskRegister != null) {
			cellNum = fillRegisterData(row, cellNum, riskRegister, threatAssessment, modelData, cellStyle);
		} else {
			// Fill 13 empty columns (5-17) because Register and Asset are null
			for (int i = 0; i < EMPTY_COLUMNS_COUNT; i++) {
				// Column 10 (risk areas) - even with asset/register null
				if (i == RISK_AREAS_COLUMN_OFFSET) {
					createCell(row, cellNum + i, String.join(", ", buildRiskAreas(threatAssessment)), cellStyle);
				} else {
					createCell(row, cellNum + i, "", cellStyle);
				}
			}
			cellNum += 13;
		}

		// Column 18: Tasks
		createCell(row, cellNum, getTasksString(tasks), cellStyle);
	}

	private void createThreatsSheet(Workbook workbook, Map<String, List<ThreatDTO>> threatList, boolean showFullMeasureDescription, ExcelStyles styles) {
		Sheet sheet = workbook.createSheet("Trussler");

		// Create header for threats sheet
		int columnCount = createThreatsHeader(sheet, styles);

		// Create data rows
		createThreatsDataRow(sheet, threatList, showFullMeasureDescription, styles, columnCount);

		// Auto-size all columns
		autoSizeColumns(sheet, columnCount);
	}

	private int createThreatsHeader(Sheet sheet, ExcelStyles styles) {
		Row header = sheet.createRow(0);

		String[] headers = {
				"Trussel Type", "Trussel Beskrivelse", "Ikke Relevant", "Sandsynlighed",
				"Konf. Registrerede", "Konf. Organisation", "Konf. Samfund",
				"Integr. Registrerede", "Integr. Organisation", "Integr. Samfund",
				"Tilg. Registrerede", "Tilg. Organisation", "Tilg. Samfund", "Autent. Samfund",
				"Problem", "Eksisterende foranstaltninger", "Supplerende Bemærkninger", "Metode", "Uddybning",
				"Restrisiko Sandsynlighed", "Restrisiko Konsekvens"
		};
		int columnCount = headers.length;

		for (int i = 0; i < columnCount; i++) {
			createCell(header, i, headers[i], styles.headerStyle);
		}

		return columnCount;
	}

	private void createThreatsDataRow(Sheet sheet, Map<String, List<ThreatDTO>> threatList, boolean showFullMeasureDescription, ExcelStyles styles, int columnCount) {
		int currentRow = 1;
		for (List<ThreatDTO> threats : threatList.values()) {
			for (ThreatDTO threat : threats) {
				Row row = sheet.createRow(currentRow++);
				if (threat.getResponseId() == 0) {
					fillBasicThreatRow(row, threat, styles, columnCount);
				} else {
					fillCombinedThreatRow(row, threat, showFullMeasureDescription, styles);
				}
			}
		}

		if (currentRow == 1) {
			Row emptyRow = sheet.createRow(1);
			createCell(emptyRow, 0, "Ingen trusselsvurderinger fundet", styles.normalStyle);
		}
	}

	private void fillBasicThreatRow(Row row, ThreatDTO threat, ExcelStyles styles, int columnCount) {
		createCell(row, 0, safeString(threat.getType()), styles.normalStyle);
		createCell(row, 1, safeString(threat.getThreat()), styles.normalStyle);

		for (int i = 2; i < columnCount; i++) {
			createCell(row, i, "", styles.normalStyle);
		}
	}

	private void fillCombinedThreatRow(Row row, ThreatDTO threat, boolean showFullMeasureDescription, ExcelStyles styles) {
		int cellNum = 0;
		createCell(row, cellNum++, safeString(threat.getType()), styles.normalStyle);
		createCell(row, cellNum++, safeString(threat.getThreat()), styles.normalStyle);
		createCell(row, cellNum++, threat.isNotRelevant() ? "Ja" : "Nej", styles.normalStyle);
		createCell(row, cellNum++, score(threat.getProbability()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getRf()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getOf()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getSf()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getRi()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getOi()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getSi()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getRt()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getOt()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getSt()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getSa()), styles.normalStyle);
		createCell(row, cellNum++, safeString(threat.getProblem()), styles.normalStyle);
		createCell(row, cellNum++, getPrecautionsString(threat.getRelatedPrecautions(), showFullMeasureDescription), styles.normalStyle);
		createCell(row, cellNum++, safeString(threat.getAdditionalMeasures()), styles.normalStyle);
		createCell(row, cellNum++, threat.getMethod().getMessage(), styles.normalStyle);
		createCell(row, cellNum++, safeString(threat.getElaboration()), styles.normalStyle);
		createCell(row, cellNum++, score(threat.getResidualRiskProbability()), styles.normalStyle);
		createCell(row, cellNum, score(threat.getResidualRiskConsequence()), styles.normalStyle);
	}

	private String getPrecautionsString(List<Relatable> precautions, boolean showFullMeasureDescription) {
		return precautions.stream()
				.map(Precaution.class::cast)
				.map(p -> showFullMeasureDescription && p.getDescription() != null ? p.getName() + ": " + p.getDescription() : p.getName())
				.collect(Collectors.joining("\n"));
	}

	private String score(int value) {
		return value < 0 ? "" : String.valueOf(value);
	}

	private int fillAssetData(Row row, int startCell, Asset riskAsset, ThreatAssessment threatAssessment, ModelData modelData, CellStyle cellStyle) {
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
		createCell(row, cellNum++, String.join(", ", buildRiskAreas(threatAssessment)), cellStyle);

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
		cellNum += 3;

		return cellNum;
	}

	private int fillRegisterData(Row row, int startCell, Register riskRegister, ThreatAssessment threatAssessment, ModelData modelData, CellStyle cellStyle) {
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
		createCell(row, cellNum++, String.join(", ", buildRiskAreas(threatAssessment)), cellStyle);

		// Supplier (column 11) - Not applicable for Register
		createCell(row, cellNum++, "", cellStyle);

		// Deletion procedure fields (columns 12-13)
		fillDeletionProcedureData(row, cellNum, riskRegister.getDataProcessing(), cellStyle);
		cellNum += 2;

		// Socially critical (column 14) - Not applicable for Register
		createCell(row, cellNum++, "", cellStyle);

		// Data access fields (columns 15-17)
		fillDataAccessFields(row, cellNum, modelData, cellStyle);
		cellNum += 3;

		return cellNum;
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

	private void fillDataAccessFields(Row row, int cellNum, ModelData modelData, CellStyle cellStyle) {
		createCell(row, cellNum++, safeString(modelData.dataAccessPersons, "Ikke udfyldt"), cellStyle);
		createCell(row, cellNum++, safeString(modelData.accessCount, "0"), cellStyle);
		createCell(row, cellNum, safeString(modelData.dataCategories, "Ikke udfyldt"), cellStyle);
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
		return (value == null || "null".equals(value)) ? "" : value;
	}

	private String safeString(String value, String defaultValue) {
		return (value != null && !value.isBlank()) ? value : defaultValue;
	}

	private void autoSizeColumns(Sheet sheet, int columnCount) {
		for (int i = 0; i < columnCount; i++) {
			sheet.autoSizeColumn(i);
		}
	}

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
}
