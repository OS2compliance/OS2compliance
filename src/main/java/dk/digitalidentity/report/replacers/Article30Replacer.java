package dk.digitalidentity.report.replacers;

import dk.digitalidentity.model.PlaceHolder;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Setting;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.InformationObligationStatus;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.ReportSetting;
import dk.digitalidentity.report.DocxUtil;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.SettingsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.report.DocxUtil.*;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@SuppressWarnings("Convert2MethodRef")
@Slf4j
@Component
public class Article30Replacer implements PlaceHolderReplacer {
    private final RegisterService registerService;
    private final ChoiceService choiceService;
    private final AssetService assetService;
	private final SettingsService settingsService;
	private final RelationService relationService;

	public Article30Replacer(final RegisterService registerService, final ChoiceService choiceService, final AssetService assetService, SettingsService settingsService, RelationService relationService) {
        this.registerService = registerService;
        this.choiceService = choiceService;
        this.assetService = assetService;
		this.settingsService = settingsService;
		this.relationService = relationService;
	}

    @Override
    public boolean supports(final PlaceHolder placeHolder) {
        return switch (placeHolder) {
			case PlaceHolder.ACTIVITIES,
				 PlaceHolder.DATARESPONSIBLE_SETTINGS
					-> true;
			default -> false;
		};
    }

    @Override
    @Transactional
    public void replace(final PlaceHolder placeHolder, final XWPFDocument document, final Map<String, String> parameters) {
        final XWPFParagraph paragraph = findParagraphToReplace(document, placeHolder.getPlaceHolder());
        if (paragraph != null) {
            replaceParagraph(paragraph, placeHolder);
        }
    }

	private XWPFParagraph findParagraphToReplace(final XWPFDocument document, final String placeholder) {
		// Only searches paragraphs. For tables or other, extend method
		for (XWPFParagraph paragraph : document.getParagraphs()) {
			if (containsPlaceholder(paragraph, placeholder)) {
				return paragraph;
			}
		}

		return null;
	}

	private boolean containsPlaceholder(final XWPFParagraph paragraph, final String placeholder) {
		String paragraphText = paragraph.getText();
		return paragraphText != null && paragraphText.contains(placeholder);
	}

	private void replaceParagraph(final XWPFParagraph paragraph, final PlaceHolder placeHolder) {
		clearAllRuns(paragraph);

		switch (placeHolder) {
			case ACTIVITIES -> insertArticle30(paragraph);
			case DATARESPONSIBLE_SETTINGS -> insertDataresponsibleSettings(paragraph);
			default -> throw new IllegalArgumentException("Unsupported placeholder: " + placeHolder);
		}
	}

	private void clearAllRuns(final XWPFParagraph paragraph) {
		for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
			paragraph.removeRun(i);
		}
	}

	private void insertDataresponsibleSettings(final XWPFParagraph p) {

		Map<String, String> settings = settingsService.getByAssociationAndEditable("report").stream()
				.collect(Collectors.toMap(Setting::getSettingKey, Setting::getSettingValue));

		// Dataresponsible
		String dataResponsibleLabel ="Dataansvarlig";
		String dataResponsibleText = settings.get(ReportSetting.DATARESPONSIBLE.getKey());
		XWPFRun dataResponsiblelabelRun = createTextRun(p, dataResponsibleLabel);
		dataResponsiblelabelRun.setBold(true);
		dataResponsiblelabelRun.addTab();
		dataResponsiblelabelRun.addTab();
		dataResponsiblelabelRun.addTab();
		XWPFRun dataResponsibleTextRun = createTextRun(p, dataResponsibleText);
		dataResponsibleTextRun.addBreak();

		// SecurityPrecautions
		String securityLabel ="Sikkerhedsforanstaltninger";
		String securityText = settings.get(ReportSetting.SECURITY_PRECAUTIONS.getKey());
		XWPFRun securitylabelRun =createTextRun(p, securityLabel);
		securitylabelRun.setBold(true);
		securitylabelRun.addTab();
		securitylabelRun.addTab();
		XWPFRun securityTextRun = createTextRun(p, securityText);
		securityTextRun.addBreak();

		// data receivers
		String receiverLabel ="Modtagere af persondata";
		String receiverText = settings.get(ReportSetting.PERSONAL_DATA_RECEIVERS.getKey());
		XWPFRun receiverlabelRun =createTextRun(p, receiverLabel);
		receiverlabelRun.setBold(true);
		receiverlabelRun.addTab();
		receiverlabelRun.addTab();
		XWPFRun receiverTextRun =createTextRun(p, receiverText);
		receiverTextRun.addBreak();

		// Contact information section
		XWPFRun contactHeaderRun =createTextRun(p, "Kontaktoplysninger");
		contactHeaderRun.setBold(true);
		contactHeaderRun.addBreak();

		// contact data resp.
		String conDataRespLabel ="Dataansvarlige";
		String conDataRespText = settings.get(ReportSetting.CONTACT_DATARESPONSIBLE.getKey());
		XWPFRun conDataResplabelRun = createTabbedTextRun(p, conDataRespLabel);
		conDataResplabelRun.addTab();
		conDataResplabelRun.addTab();
		XWPFRun conDataTextRun =createTextRun(p, conDataRespText);
		conDataTextRun.addBreak();

		// contact commoon data resp
		String conCommonLabel ="Fælles dataansvarlige";
		String conCommonText = settings.get(ReportSetting.CONTACT_COMMON_DATARESPONSIBLE.getKey());
		XWPFRun conCommonLabelRun = createTabbedTextRun(p, conCommonLabel);
		conCommonLabelRun.addTab();
		conCommonLabelRun.addTab();
		XWPFRun conCommonTextRun =createTextRun(p, conCommonText);
		conCommonTextRun.addBreak();

		// contact representative
		String conRepLabel ="Dataansvarliges repræsentant";
		String conRepText = settings.get(ReportSetting.CONTACT_DATARESPONSIBLE_REPRESENTATIVE.getKey());
		XWPFRun conRepRun = createTabbedTextRun(p, conRepLabel);
		conRepRun.addTab();
		XWPFRun conRepTextRun =createTextRun(p, conRepText);
		conRepTextRun.addBreak();

		// contact advisor
		String conAdvisorLabel ="Databeskyttelsesrådgiver";
		String conAdvisorText = settings.get(ReportSetting.CONTACT_DATA_PROTECTION_ADVISOR.getKey());
		XWPFRun conAdvisorRun = createTabbedTextRun(p, conAdvisorLabel);
		conAdvisorRun.addTab();
		XWPFRun conAdvisorTextRun =createTextRun(p, conAdvisorText);
		conAdvisorTextRun.addBreak();

	}

	private XWPFRun createTextRun(final XWPFParagraph paragraph, String text) {
		XWPFRun run = paragraph.createRun();
		run.setText(text);
		return run;
	}
	private XWPFRun createTabbedTextRun(final XWPFParagraph paragraph, String text) {
		XWPFRun run = paragraph.createRun();
		run.addTab();
		run.setText(text);
		return run;
	}


	private void insertArticle30(final XWPFParagraph p) {
		final List<Register> allArticle30 = registerService.findAllOrdered();

		XWPFRun newRun = p.createRun();
		newRun.setText("", 0);
		try (final XmlCursor cursor = setCursorToNextStartToken(p.getCTP())) {
			boolean initial = true;
			for (final Register register : allArticle30) {
				insertRegister(p.getDocument(), cursor, register, initial);
				initial = false;
			}
		}

		List<Long> relatedAssetIds = relationService.findAllIdsRelatedToWithType(allArticle30.stream().map(r -> r.getId()).toList(), RelationType.ASSET);
		List<Asset> relatedAssets = assetService.findAllById(relatedAssetIds);
		insertSupplierAdressSection(p, relatedAssets);
	}

	private void insertSupplierAdressSection(final XWPFParagraph p, List<Asset> relatedAssets) {
		XWPFDocument document = p.getDocument();

		// Add page break to end of file
		XWPFParagraph pageBreakParagraph = document.createParagraph();
		pageBreakParagraph.createRun().addBreak(BreakType.PAGE);

		// Add a header to end of file
		XWPFParagraph supplierAdressParagraph = document.createParagraph();
		XWPFRun headerRun = createTextRun(supplierAdressParagraph, "Leverandøradresser");
		headerRun.setBold(true);

		// Add a table with supplier adresses to end of file
		createSupplierAddressTable(document, relatedAssets);



	}

	private void createSupplierAddressTable(XWPFDocument doc, List<Asset> relatedAssets) {
		// Map to records
		List<AssetSupplier> assetSuppliers = mapToAssetSuppliers(relatedAssets);

		// Create table
		List<String> headerTexts = new ArrayList<>() ;
		headerTexts.add("Aktiv");
		headerTexts.add("Leverandør");
		headerTexts.add("Adresse");

		XWPFTable table = doc.createTable(1, headerTexts.size());

		// Create header
		createSupplierAddressHeader(headerTexts, table);

		// Create rows
		createSupplierAddressRows(assetSuppliers, table);
	}

	private void createSupplierAddressHeader(List<String> headerTexts , XWPFTable table) {
		XWPFTableRow header = table.getRow(0);

		for (int i = 0; i < headerTexts.size() ; i++) {
			XWPFRun run = header.getCell(i).getParagraphs().getFirst().createRun();
			run.setText(headerTexts.get(i));
			run.setBold(true);
		}
	}

	private void createSupplierAddressRows(List<AssetSupplier> assetSuppliers, XWPFTable table) {
		for (AssetSupplier asset : assetSuppliers) {
			XWPFTableRow row = table.createRow();
			row.getCell(0).setText(asset.assetName);

			if (asset.addresses.isEmpty()) {
				// With no adresses, the rest of the cells is left empty and we continue
				continue;
			}

			row.getCell(1).setText(asset.addresses.getFirst().supplierName);
			row.getCell(2).setText(asset.addresses.getFirst().address);

			// With more than one adress in list, we create a new row for each beyond the first, leaving first cell empty
			for (int r = 1; r < asset.addresses.size(); r++) {
				XWPFTableRow adressRow = table.createRow();
				adressRow.getCell(1).setText(asset.addresses.get(r).supplierName);
				adressRow.getCell(2).setText(asset.addresses.get(r).address);
			}
		}
	}

	private record SupplierAddress ( String supplierName, String address){}
	private record AssetSupplier(String assetName, List<SupplierAddress> addresses) {}
	private List<AssetSupplier> mapToAssetSuppliers (List<Asset> relatedAssets) {
		return relatedAssets.stream().map(a -> {
					Supplier mainSupplier = a.getSupplier();
					List<SupplierAddress> suppliers = new HashSet<>(a.getSuppliers())
							.stream()
							.map(m -> m.getSupplier())
							.sorted((supA, supB) -> {
								if (supA == mainSupplier) {
									return 1;
								}
								if (supB == mainSupplier) {
									return -1;
								}
								return supA.getName().compareTo(supB.getName());
							})
							.map(this::mapToSupplierAddress)
							.toList();

					return new AssetSupplier(a.getName(), suppliers);
				})
				.sorted(Comparator.comparing(AssetSupplier::assetName))
				.toList();
	}

	private SupplierAddress mapToSupplierAddress(Supplier s) {
		StringBuilder builder = new StringBuilder();
		if (s.getAddress() != null) {
			builder.append(s.getAddress());
			if (s.getZip() != null && s.getCity() != null) {
				builder.append(", ");
				builder.append(s.getZip());
				builder.append(s.getCity());
			}
			if (s.getCountry() != null) {
				builder.append(", ");
				builder.append(s.getCountry());
			}
		}
		return new SupplierAddress(
				s.getName(),
				builder.toString()
		);
	}

    private void insertRegister(final XWPFDocument document, final XmlCursor cursor,
                                final Register register, final boolean initialBreak) {
        final XWPFParagraph title = document.insertNewParagraph(cursor);
        title.setStyle("Heading1");
        title.setPageBreak(initialBreak);
        addTextRun(register.getName(), title);
        advanceCursor(cursor);

        insertStandard(document, cursor, "Behandlingsansvarlig(e): ",
            nullSafe(() -> register.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", ")), "Ikke angivet"));
        insertStandard(document, cursor, "Ansvarlig forvaltning(er): ",
            nullSafe(() -> register.getDepartments().stream().map(OrganisationUnit::getName).collect(Collectors.joining(", ")), "Ikke angivet"));
        insertStandard(document, cursor, "Ansvarlig afdeling(er): ",
            nullSafe(() -> register.getResponsibleOus().stream().map(OrganisationUnit::getName).collect(Collectors.joining(", ")), "Ikke angivet"));
        insertStandard(document, cursor, "Hvem er ansvarlig for behandling af personoplysningerne: ",
            nullSafe(register::getInformationResponsible, "Ikke angivet"));
		insertStandard(document, cursor, "Navn på DPO: ",
				nullSafe(register::getDataProtectionOfficer, "Ikke angivet"));
        insertStandard(document, cursor, "Fortegnelse over behandlingsaktivitet angående: ",
            nullSafe(() -> register.getRegisterRegarding().stream().map(v -> v.getCaption()).collect(Collectors.joining(", ")), "Ikke angivet"));
		insertStandard(document, cursor, "Sikkerhedsforanstaltninger: ",
				nullSafe(register::getSecurityPrecautions, "Ikke angivet"));
        insertStandard(document, cursor, "Beskriv formålet med behandlingsaktiviteten: ",
            nullSafe(register::getPurpose, "Ikke angivet"));
        insertBoldParagraph(document, cursor, "GDPR lovhjemmel");

        final Set<String> choiceIds = register.getGdprChoices();
        final ChoiceList gdprChoices = choiceService.findChoiceList("register-gdpr").orElseThrow();
        final ChoiceList gdprChoicesP6 = choiceService.findChoiceList("register-gdpr-p6").orElseThrow();
        final ChoiceList gdprChoicesP7 = choiceService.findChoiceList("register-gdpr-p7").orElseThrow();

        XWPFParagraph gdprParagraph = insertNormalParagraph(document, cursor);
        final List<ChoiceValue> selectedChoices = gdprChoices.getValues().stream()
            .filter(c -> choiceIds.contains(c.getIdentifier()))
            .toList();
        for (final ChoiceValue selectedChoice : selectedChoices) {
            final XWPFRun run = addTextRun(selectedChoice.getCaption() + " " + selectedChoice.getDescription(), gdprParagraph);
            if ("register-gdpr-valp6".equals(selectedChoice.getIdentifier())) {
                gdprParagraph = addChoiceListList(document, cursor, choiceIds, gdprChoicesP6);
            } else if ("register-gdpr-valp7".equals(selectedChoice.getIdentifier())) {
                gdprParagraph = addChoiceListList(document, cursor, choiceIds, gdprChoicesP7);
            } else {
                run.addBreak();
            }
        }
        insertStandard(document, cursor, "Opfyldes oplysningspligten: ",
            nullSafe(() -> register.getInformationObligation().getMessage(), "Ikke angivet"));
        if (register.getInformationObligation() == InformationObligationStatus.YES) {
            insertStandard(document, cursor, "Beskriv hvordan oplysningspligten opfyldes: ",
                nullSafe(() -> register.getInformationObligationDesc(), "Ej udfyldt"));
        }

        XWPFParagraph paragraph = insertBoldParagraph(document, cursor, "Aktiver der understøtter behandlingsaktiviteten:");
        XWPFTable table = paragraph.getBody().insertNewTbl(cursor);
        advanceCursor(cursor);
        insertAssetTable(table, assetService.findRelatedTo(register));

        if (register.getDataProcessing() != null) {

            insertBoldParagraph(document, cursor, "Hvem har adgang til personoplysningerne:");
            insertAccessWhoList(document, cursor, register);

            insertBoldParagraph(document, cursor, "Hvor mange har adgang til personoplysningerne:");
            final String accessCountValue = getChoiceCaption(register.getDataProcessing().getAccessCountIdentifier());
            paragraph = insertNormalParagraph(document, cursor);
            addTextRun(accessCountValue, paragraph);

            insertBoldParagraph(document, cursor, "Hvor mange behandles der personoplysninger om:");
            final String personCountCaption = getChoiceCaption(register.getDataProcessing().getPersonCountIdentifier());
            paragraph = insertNormalParagraph(document, cursor);
            addTextRun(personCountCaption, paragraph);

            table = paragraph.getBody().insertNewTbl(cursor);
            advanceCursor(cursor);
            insertInformationCategoriesTable(table, register.getDataProcessing());

            paragraph = insertBoldParagraph(document, cursor, "Hvor længe opbevares personoplysningerne: ");
            final String storageTimeCaption = getChoiceCaption(register.getDataProcessing().getStorageTimeIdentifier());
            addTextRun(storageTimeCaption, paragraph);
            if (register.getDataProcessing().getElaboration() != null) {
                addBoldTextRun(register.getDataProcessing().getElaboration() + ": ", paragraph);
                addTextRun(register.getDataProcessing().getElaboration(), paragraph);
            }

            insertStandard(document, cursor,
                "Er der udarbejdet sletteprocedure: ",
                nullSafe(() -> register.getDataProcessing().getDeletionProcedure().getMessage(), "")
            );
            insertStandard(document, cursor,
                "Link til sletteprocedure: ",
                nullSafe(() -> register.getDataProcessing().getDeletionProcedureLink(), "")
            );
        }
    }

    private String getChoiceCaption(final String valueIdentifier) {
        return Optional.ofNullable(valueIdentifier)
            .map(choiceService::getValue)
            .filter(Optional::isPresent)
            .map(v -> v.get().getCaption())
            .orElse("Ikke valgt");
    }

    private void insertAccessWhoList(final XWPFDocument document, final XmlCursor cursor, final Register register) {
        final ChoiceList accessWhoChoices = choiceService.findChoiceList("dp-access-who-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find accessWhoIdentifiers Choices"));
        final Set<String> selectedAccessWhoIdentifiers = register.getDataProcessing().getAccessWhoIdentifiers();
        addChoiceListList(document, cursor, selectedAccessWhoIdentifiers, accessWhoChoices);
    }


    private void insertInformationCategoriesTable(final XWPFTable table, final DataProcessing dataProcessing) {
        int rowCounter = 0;
        final XWPFTableRow headerRow = table.getRow(rowCounter++);
        final XWPFTableCell cell0 = getCell(headerRow, 0);
        cell0.setWidth("25%");
        addBoldTextRun("Kategorier af registrerede", cell0.getParagraphs().getFirst());
        final XWPFTableCell cell1 = getCell(headerRow, 1);
        cell1.setWidth("25%");
        addBoldTextRun("Typer af personoplysninger", cell1.getParagraphs().getFirst());
        final XWPFTableCell cell2 = getCell(headerRow, 2);
        cell2.setWidth("25%");
        addBoldTextRun("Videregives personoplysningerne til andre som anvender oplysningerne til eget formål?", cell2.getParagraphs().getFirst());
        final XWPFTableCell cell3 = getCell(headerRow, 3);
        cell3.setWidth("25%");
        addBoldTextRun("Modtager", cell3.getParagraphs().getFirst());
        for (final DataProcessingCategoriesRegistered registeredCategory : dataProcessing.getRegisteredCategories()) {
            XWPFTableRow row = table.getRow(rowCounter++);
            if (row == null) {
                row = table.createRow();
            }
            getCell(row, 0).setText(getChoiceCaption(registeredCategory.getPersonCategoriesRegisteredIdentifier()));

            XWPFParagraph paragraph = getCell(row, 1).getParagraphs().getFirst();
            try (final XmlCursor cellCursor = paragraph.getCTP().newCursor()) {
                final List<String> values = registeredCategory.getPersonCategoriesInformationIdentifiers().stream()
                    .map(identifier -> choiceService.getValue(identifier))
                    .filter(c -> c.isPresent())
                    .map(c -> c.get().getCaption())
                    .collect(Collectors.toList());
                addBulletList(paragraph.getDocument(), cellCursor, values);
            }

            getCell(row, 2).setText(registeredCategory.getInformationPassedOn().getMessage());

            paragraph = getCell(row, 3).getParagraphs().getFirst();
            try (final XmlCursor cellCursor = paragraph.getCTP().newCursor()) {
                final List<String> values = registeredCategory.getInformationReceivers().stream()
						.map(ir -> ir.getChoiceValue().getIdentifier())
						.map(choiceService::getValue)
						.filter(c -> c.isPresent())
						.map(c -> c.get().getCaption())
						.toList();
                addBulletList(paragraph.getDocument(), cellCursor, new ArrayList<>(values));
            }
        }
    }

    private void insertAssetTable(final XWPFTable table, final List<Asset> assets) {
        int rowCounter = 0;
        final XWPFTableRow headerRow = table.getRow(rowCounter++);
        addBoldTextRun("Navn", getCell(headerRow, 0).getParagraphs().getFirst());
        addBoldTextRun("Leverandør", getCell(headerRow, 1).getParagraphs().getFirst());
        addBoldTextRun("Er der indgået en databehandleraftale", getCell(headerRow, 2).getParagraphs().getFirst());
        addBoldTextRun("Land", getCell(headerRow, 3).getParagraphs().getFirst());
        addBoldTextRun("Tredjelands- overførsel", getCell(headerRow, 4).getParagraphs().getFirst());
        addBoldTextRun("Acceptgrundlag", getCell(headerRow, 5).getParagraphs().getFirst());

        for (final Asset asset : assets) {
            XWPFTableRow row = table.getRow(rowCounter++);
            if (row == null) {
                row = table.createRow();
            }
            final Optional<AssetSupplierMapping> dataProcessingSupplierEntry = asset.getSuppliers().stream()
                .filter(s -> Objects.equals(s.getSupplier().getId(), nullSafe(() -> asset.getSupplier().getId())))
                .findFirst();
            getCell(row, 0).setText(asset.getName());
            getCell(row, 1).setText(nullSafe(() -> asset.getSupplier().getName(), ""));
            getCell(row, 2).setText(nullSafe(() -> asset.getDataProcessingAgreementStatus().getMessage(), ""));
            getCell(row, 3).setText(nullSafe(() -> asset.getSupplier().getCountry(), ""));

            getCell(row, 4).setText(dataProcessingSupplierEntry
                .map(e -> nullSafe(() -> e.getThirdCountryTransfer().getMessage(), "")).orElse(""));
            getCell(row, 5).setText(dataProcessingSupplierEntry
                .map(e -> nullSafe(() -> e.getAcceptanceBasis(), "")).orElse(""));
        }

    }

    private static XWPFParagraph insertNormalParagraph(final XWPFDocument document, final XmlCursor cursor) {
        final XWPFParagraph gdprParagraph = document.insertNewParagraph(cursor);
        advanceCursor(cursor);
        return gdprParagraph;
    }

    private XWPFParagraph addChoiceListList(final XWPFDocument document, final XmlCursor cursor,
                                            final Set<String> choiceIds, final ChoiceList choiceList) {
        final XWPFParagraph gdprParagraph;
        final List<String> values = choiceList.getValues().stream()
            .filter(s -> choiceIds.contains(s.getIdentifier()))
            .map(Article30Replacer::listItemCaption)
            .toList();
        DocxUtil.addBulletList(document, cursor, values);
        gdprParagraph = document.insertNewParagraph(cursor);
        advanceCursor(cursor);
        return gdprParagraph;
    }

    private static String listItemCaption(final ChoiceValue value) {
        if (value.getCaption() != null && value.getDescription() != null) {
            return value.getCaption() + " - " + value.getDescription();
        } else if (value.getCaption() != null) {
            return value.getCaption();
        } else if (value.getDescription() != null) {
            return value.getDescription();
        }
        return "";
    }

    private static XWPFParagraph insertBoldParagraph(final XWPFDocument document, final XmlCursor cursor,
                                                     final String boldPart) {
        final XWPFParagraph paragraph = document.insertNewParagraph(cursor);
        addBoldTextRun(boldPart, paragraph);
        advanceCursor(cursor);
        return paragraph;
    }

    private static void insertStandard(final XWPFDocument document, final XmlCursor cursor,
                                       final String boldPart, final String text) {
        final XWPFParagraph paragraph = document.insertNewParagraph(cursor);
        addBoldTextRun(boldPart, paragraph);
        addTextRun(text, paragraph);
        advanceCursor(cursor);
    }

}
