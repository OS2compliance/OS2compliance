package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Setting;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.RelationService;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.integration.kitos.KitosConstants.*;
import static dk.digitalidentity.report.XlsUtil.createCell;

@Component
public class ReportThreatAssessmentXlsView extends AbstractXlsView {

	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		final ThreatAssessment threatAssessment = (ThreatAssessment) model.get("threatAssessment");
		ChoiceService choiceService = (ChoiceService) model.get("choiceService");
		SettingsService settingsService = (SettingsService) model.get("settingsService");
		RelationService relationService = (RelationService) model.get("relationService");


		if (threatAssessment == null) {
			throw new IllegalArgumentException("ThreatAssessment not found in model");
		}

		Sheet sheet = workbook.createSheet("ThreatAssessment");
		Font headerFont = createExcelFont(workbook);
		CellStyle headerStyle = setSheetStyle(workbook, headerFont);

		createHeader(workbook, sheet, headerStyle, settingsService);

		// Create normal cell style for data rows
		CellStyle normalStyle = workbook.createCellStyle();
		normalStyle.setWrapText(true);

		final List<Relatable> relations = relationService.findAllRelatedTo(threatAssessment);

		Asset riskAsset = null;
		Register riskRegister = null;

		if (ThreatAssessmentType.ASSET == threatAssessment.getThreatAssessmentType()) {
			final Optional<Asset> asset = relations.stream()
					.filter(r -> r.getRelationType() == RelationType.ASSET)
					.map(Asset.class::cast)
					.findFirst();
			riskAsset = asset.orElse(null);
		} else if (ThreatAssessmentType.REGISTER == threatAssessment.getThreatAssessmentType()) {
			final Optional<Register> register = relations.stream()
					.filter(r -> r.getRelationType() == RelationType.REGISTER)
					.map(Register.class::cast)
					.findFirst();
			riskRegister = register.orElse(null);
		}
		List<Task> riskAssessmentTasks = relations.stream().filter(t -> t.getRelationType() == RelationType.TASK)
				.map(Task.class::cast)
				.toList();

		inputThreatAssessment(sheet, normalStyle, threatAssessment, riskAsset, riskRegister, riskAssessmentTasks, choiceService);

		// Auto-size columns after data is added
		for (int i = 0; i < 18; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	private void createHeader(Workbook workbook, Sheet sheet, CellStyle headerStyle, SettingsService settingsService) {
		final Row header = sheet.createRow(0);
		Setting customOwnerName = settingsService.findBySettingKey(KITOS_OWNER_ROLE_SETTING_INPUT_FIELD_NAME);
		Setting customResponsibleName = settingsService.findBySettingKey(KITOS_RESPONSIBLE_ROLE_SETTING_INPUT_FIELD_NAME);
		Setting customOperationName = settingsService.findBySettingKey(KITOS_OPERATION_RESPONSIBLE_ROLE_SETTING_INPUT_FIELD_NAME);

		createCell(header, 0, "Titel", headerStyle);
		createCell(header, 1, "Kommentarer", headerStyle);
		createCell(header, 2, "Undertitel", headerStyle);
		createCell(header, 3, "Tilstede på mødet", headerStyle);
		createCell(header, 4, "Kritikalitet", headerStyle);
		createCell(header, 5, customOwnerName != null ? customOwnerName.getSettingValue() : "Systemejer", headerStyle);
		createCell(header, 6, customResponsibleName != null ? customResponsibleName.getSettingValue() : "Systemansvarlig", headerStyle);
		createCell(header, 7, customOperationName != null ? customOperationName.getSettingValue() : "Driftsansvarlig", headerStyle);
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
		createCell(header, 18, "Opgaver oprettet under risikovurderingen", headerStyle);
	}

	private void inputThreatAssessment(Sheet sheet, CellStyle cellStyle, ThreatAssessment threatAssessment,
			Asset riskAsset, Register riskRegister, List<Task> tasks, ChoiceService choiceService) {
		final Row row = sheet.createRow(1);

		// Column 0: Titel
		createCell(row, 0, threatAssessment.getName(), cellStyle);

		// Column 1: Kommentarer
		createCell(row, 1, threatAssessment.getComment() != null ? threatAssessment.getComment() : "", cellStyle);

		// Column 2: Undertitel
		createCell(row, 2, getSubHeading(threatAssessment, riskAsset, riskRegister), cellStyle);

		// Column 3: Tilstede på mødet
		String presentAtMeeting = (threatAssessment.getPresentAtMeeting() != null && !threatAssessment.getPresentAtMeeting().isEmpty())
				? threatAssessment.getPresentAtMeeting().stream().map(User::getName).collect(Collectors.joining(", "))
				: "Ingen tilstede";
		createCell(row, 3, presentAtMeeting, cellStyle);

		// Column 4: Kritikalitet
		createCell(row, 4, getCriticality(riskAsset, riskRegister), cellStyle);

		// Fill remaining columns based on Asset or Register
		if (riskAsset != null) {
			fillAssetData(row, cellStyle, riskAsset, choiceService);
		} else if (riskRegister != null) {
			fillRegisterData(row, cellStyle, riskRegister, choiceService);
		} else {
			// Fill with empty values if neither asset nor register exists
			for (int i = 5; i < 18; i++) {
				createCell(row, i, "", cellStyle);
			}
		}
		// Column 10: Risk areas
		createCell(row, 10, String.join(",", buildRiskAreas(threatAssessment)), cellStyle);
		// Column 18: Related tasks
		createCell(row, 18, tasks.stream().map(Task::getName).collect(Collectors.joining(",")), cellStyle);
	}

	private void fillAssetData(Row row, CellStyle cellStyle, Asset riskAsset, ChoiceService choiceService) {
		// Column 5: System owners
		String systemOwners = riskAsset.getResponsibleUsers().stream()
				.map(User::getName)
				.collect(Collectors.joining(", "));
		createCell(row, 5, systemOwners.isBlank() ? "Ikke udfyldt" : systemOwners, cellStyle);

		// Column 6: System responsible
		String systemResponsible = riskAsset.getManagers().stream()
				.map(User::getName)
				.collect(Collectors.joining(", "));
		createCell(row, 6, systemResponsible, cellStyle);

		// Column 7: Operation responsible
		String operationResponsible = riskAsset.getOperationResponsibleUsers().stream()
				.map(User::getName)
				.collect(Collectors.joining(", "));
		createCell(row, 7, operationResponsible, cellStyle);

		// Column 8: Systemtype
		createCell(row, 8, riskAsset.getAssetType().getCaption(), cellStyle);

		// Column 9: Formål
		createCell(row, 9, "Ikke udfyldt", cellStyle);

		// Column 11: Leverandør
		createCell(row, 11, riskAsset.getSupplier() != null ? riskAsset.getSupplier().getName() : "Ukendt", cellStyle);

		// Column 12: Sletteprocedure udarbejdet
		String deletionProcedure = riskAsset.getDataProcessing().getDeletionProcedure() != null
				? riskAsset.getDataProcessing().getDeletionProcedure().getMessage()
				: "Ikke udfyldt";
		createCell(row, 12, deletionProcedure, cellStyle);

		// Column 13: Link til Sletteprocedure
		String deletionLink = riskAsset.getDataProcessing().getDeletionProcedureLink();
		createCell(row, 13, deletionLink != null ? deletionLink : "Ikke angivet", cellStyle);

		// Column 14: Samfundskritisk
		createCell(row, 14, riskAsset.isSociallyCritical() ? "Ja" : "Nej", cellStyle);

		// Column 15: Hvem har adgang til personoplysningerne
		String dataAccessPersons = riskAsset.getDataProcessing().getAccessWhoIdentifiers().stream()
				.map(identifier -> {
					Optional<ChoiceValue> value = choiceService.getValue(identifier);
					return value.isPresent() ? value.get().getCaption() : "Ikke udfyldt";
				})
				.collect(Collectors.joining(", "));
		createCell(row, 15, dataAccessPersons.isBlank() ? "Ikke udfyldt" : dataAccessPersons, cellStyle);

		// Column 16: Hvor mange har adgang til personoplysningerne?
		var accessCount = choiceService.getValue(riskAsset.getDataProcessing().getAccessCountIdentifier());
		createCell(row, 16, accessCount.isPresent() ? accessCount.get().getCaption() : "0", cellStyle);

		// Column 17: Kategorier af registrerede og typer af personoplysninger
		String dataCategories = buildDataCategoriesString(riskAsset.getDataProcessing().getRegisteredCategories(), choiceService);
		createCell(row, 17, dataCategories, cellStyle);
	}

	private void fillRegisterData(Row row, CellStyle cellStyle, Register riskRegister, ChoiceService choiceService) {
		// Column 5: System owners
		String systemOwners = riskRegister.getResponsibleUsers().stream()
				.map(User::getName)
				.collect(Collectors.joining(", "));
		createCell(row, 5, systemOwners.isBlank() ? "Ikke udfyldt" : systemOwners, cellStyle);

		// Column 6-7: Not applicable for Register
		createCell(row, 6, "", cellStyle);
		createCell(row, 7, "", cellStyle);

		// Column 8: Systemtype
		createCell(row, 8, "Fortegnelse", cellStyle);

		// Column 9: Formål
		createCell(row, 9, riskRegister.getPurpose() != null ? riskRegister.getPurpose() : "", cellStyle);

		// Column 11: Leverandør (not applicable for Register)
		createCell(row, 11, "", cellStyle);

		// Column 12: Sletteprocedure udarbejdet
		String deletionProcedure = riskRegister.getDataProcessing().getDeletionProcedure() != null
				? riskRegister.getDataProcessing().getDeletionProcedure().getMessage()
				: "Ikke udfyldt";
		createCell(row, 12, deletionProcedure, cellStyle);

		// Column 13: Link til Sletteprocedure
		String deletionLink = riskRegister.getDataProcessing().getDeletionProcedureLink();
		createCell(row, 13, deletionLink != null ? deletionLink : "", cellStyle);

		// Column 14: Samfundskritisk (not applicable for Register)
		createCell(row, 14, "", cellStyle);

		// Column 15: Hvem har adgang til personoplysningerne
		String dataAccessPersons = riskRegister.getDataProcessing().getAccessWhoIdentifiers().stream()
				.map(identifier -> {
					Optional<ChoiceValue> value = choiceService.getValue(identifier);
					return value.isPresent() ? value.get().getCaption() : "Ikke udfyldt";
				})
				.collect(Collectors.joining(", "));
		createCell(row, 15, dataAccessPersons.isBlank() ? "Ikke udfyldt" : dataAccessPersons, cellStyle);

		// Column 16: Hvor mange har adgang til personoplysningerne?
		var accessCount = choiceService.getValue(riskRegister.getDataProcessing().getAccessCountIdentifier());
		createCell(row, 16, accessCount.isPresent() ? accessCount.get().getCaption() : "", cellStyle);

		// Column 17: Kategorier af registrerede og typer af personoplysninger
		String dataCategories = buildDataCategoriesString(riskRegister.getDataProcessing().getRegisteredCategories(), choiceService);
		createCell(row, 17, dataCategories, cellStyle);
	}

	private String buildDataCategoriesString(List<DataProcessingCategoriesRegistered> registeredCategories, ChoiceService choiceService) {
		return registeredCategories.stream()
				.map(cat -> {
					Optional<ChoiceValue> title = choiceService.getValue(cat.getPersonCategoriesRegisteredIdentifier());
					if (title.isEmpty()) {
						return null;
					}
					List<String> types = cat.getPersonCategoriesInformationIdentifiers().stream()
							.map(type -> choiceService.getValue(type).map(ChoiceValue::getCaption).orElse(null))
							.filter(Objects::nonNull)
							.toList();
					return title.get().getCaption() + ": " + String.join(", ", types);
				})
				.filter(Objects::nonNull)
				.collect(Collectors.joining(" | "));
	}

	private String getSubHeading(final ThreatAssessment threatAssessment, final Asset asset, final Register register) {
		if (asset != null && asset.getResponsibleUsers() != null && !asset.getResponsibleUsers().isEmpty()) {
			return "Systemejere: " + asset.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "));
		} else if (register != null && register.getResponsibleUsers() != null && !register.getResponsibleUsers().isEmpty()) {
			return "Behandlingsansvarlige: " + register.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "));
		}
		if (threatAssessment.getResponsibleUser() != null) {
			return "Risikoejer: " + threatAssessment.getResponsibleUser().getName();
		}
		return "Risikoejer ikke udfyldt";
	}

	private String getCriticality(final Asset asset, final Register register) {
		StringBuilder stringBuilder = new StringBuilder();
		if (asset != null) {
			stringBuilder.append("Systemet er: ");
			stringBuilder.append(asset.getCriticality() != null ? asset.getCriticality().getMessage() : "Ikke udfyldt");
			stringBuilder.append(" | ");
			stringBuilder.append("Nødplan: ");
			stringBuilder.append(asset.getEmergencyPlanLink() != null ? asset.getEmergencyPlanLink() : "Ikke udfyldt");
		} else if (register != null) {
			stringBuilder.append("Behandlingsaktiviteten er: ");
			stringBuilder.append(register.getCriticality() != null ? register.getCriticality().getMessage() : "Ikke udfyldt");
			stringBuilder.append(" | ");
			stringBuilder.append("Nødplan: ");
			stringBuilder.append(register.getEmergencyPlanLink() != null ? register.getEmergencyPlanLink() : "Ikke udfyldt");
		}
		return stringBuilder.toString();
	}

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