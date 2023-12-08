package dk.digitalidentity.report.replacers;

import dk.digitalidentity.model.PlaceHolder;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.enums.InformationObligationStatus;
import dk.digitalidentity.report.DocxUtil;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.RegisterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.BodyType;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static dk.digitalidentity.report.DocxUtil.addBoldTextRun;
import static dk.digitalidentity.report.DocxUtil.addBulletList;
import static dk.digitalidentity.report.DocxUtil.addTextRun;
import static dk.digitalidentity.report.DocxUtil.advanceCursor;
import static dk.digitalidentity.report.DocxUtil.getCell;
import static dk.digitalidentity.report.DocxUtil.setCursorToNextStartToken;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@SuppressWarnings("Convert2MethodRef")
@Slf4j
@Component
public class Article30Replacer implements PlaceHolderReplacer {
    private final RegisterService registerService;
    private final ChoiceService choiceService;
    private final AssetService assetService;

    public Article30Replacer(final RegisterService registerService, final ChoiceService choiceService, final AssetService assetService) {
        this.registerService = registerService;
        this.choiceService = choiceService;
        this.assetService = assetService;
    }

    @Override
    public boolean supports(final PlaceHolder placeHolder) {
        return placeHolder == PlaceHolder.ACTIVITIES;
    }

    @Override
    @Transactional
    public void replace(final PlaceHolder placeHolder, final XWPFDocument document, final Map<String, String> parameters) {
        final XWPFParagraph paragraph = findParagraphToReplace(document, placeHolder.getPlaceHolder());
        if (paragraph != null) {
            replaceParagraph(paragraph, placeHolder.getPlaceHolder());
        }
    }

    private XWPFParagraph findParagraphToReplace(final XWPFDocument document, final String placeHolder) {
        final AtomicReference<XWPFParagraph> result = new AtomicReference<>();
        document.getBodyElementsIterator().forEachRemaining(
            part -> {
                if (result.get() == null) {
                    if (part.getElementType() != BodyElementType.CONTENTCONTROL && part.getPartType() != BodyType.CONTENTCONTROL) {
                        final List<XWPFParagraph> paragraphs = part.getBody().getParagraphs();
                        for (final XWPFParagraph paragraph : paragraphs) {
                            paragraph.getRuns().stream()
                                .filter(r -> placeHolder.equalsIgnoreCase(r.getText(0)))
                                .findFirst().ifPresent(p -> result.set(paragraph));
                        }
                    }
                }
            }
        );
        return result.get();
    }

    private void replaceParagraph(final XWPFParagraph p, final String placeHolder) {

        p.getRuns().stream()
            .filter(r -> placeHolder.equalsIgnoreCase(r.getText(0)))
            .findFirst()
            .ifPresent(xwpfRun -> insertArticle30(p, xwpfRun));
    }

    private void insertArticle30(final XWPFParagraph p, final XWPFRun placeHolderRun) {
        final List<Register> allArticle30 = registerService.findAll();
        placeHolderRun.setText("", 0);
        try (final XmlCursor cursor = setCursorToNextStartToken(p.getCTP())) {
            boolean initial = true;
            for (final Register register : allArticle30) {
                insertRegister(p.getDocument(), cursor, register, initial);
                initial = false;
            }
        }
    }

    private void insertRegister(final XWPFDocument document, final XmlCursor cursor,
                                final Register register, final boolean initialBreak) {
        final XWPFParagraph title = document.insertNewParagraph(cursor);
        title.setStyle("Heading1");
        title.setPageBreak(initialBreak);
        addTextRun(register.getName(), title);
        advanceCursor(cursor);

        insertStandard(document, cursor, "Behandlingsansvarlig: ",
            nullSafe(() -> register.getResponsibleUser().getName(), "Ikke angivet"));
        insertStandard(document, cursor, "Ansvarlig forvaltning: ",
            nullSafe(() -> register.getDepartment().getName(), "Ikke angivet"));
        insertStandard(document, cursor, "Ansvarlig afdeling: ",
            nullSafe(() -> register.getResponsibleOu().getName()));
        insertStandard(document, cursor, "Hvem er ansvarlig for behandling af personoplysningerne: ",
            nullSafe(() -> register.getInformationResponsible(), "Ikke angivet"));
        insertStandard(document, cursor, "Fortegnelse over behandlingsaktivitet angående: ",
            nullSafe(() -> register.getRegisterRegarding(), "Ikke angivet"));
        insertStandard(document, cursor, "Beskriv formålet med behandlingsaktiviteten: ",
            nullSafe(() -> register.getPurpose(), "Ikke angivet"));
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
        addBoldTextRun("Kategorier af registrerede", cell0.getParagraphs().get(0));
        final XWPFTableCell cell1 = getCell(headerRow, 1);
        cell1.setWidth("25%");
        addBoldTextRun("Typer af personoplysninger", cell1.getParagraphs().get(0));
        final XWPFTableCell cell2 = getCell(headerRow, 2);
        cell2.setWidth("25%");
        addBoldTextRun("Videregives personoplysningerne til andre som anvender oplysningerne til eget formål?", cell2.getParagraphs().get(0));
        final XWPFTableCell cell3 = getCell(headerRow, 3);
        cell3.setWidth("25%");
        addBoldTextRun("Modtager", cell3.getParagraphs().get(0));
        for (final DataProcessingCategoriesRegistered registeredCategory : dataProcessing.getRegisteredCategories()) {
            XWPFTableRow row = table.getRow(rowCounter++);
            if (row == null) {
                row = table.createRow();
            }
            getCell(row, 0).setText(getChoiceCaption(registeredCategory.getPersonCategoriesRegisteredIdentifier()));

            XWPFParagraph paragraph = getCell(row, 1).getParagraphs().get(0);
            try (final XmlCursor cellCursor = paragraph.getCTP().newCursor()) {
                final List<String> values = registeredCategory.getPersonCategoriesInformationIdentifiers().stream()
                    .map(identifier -> choiceService.getValue(identifier))
                    .filter(c -> c.isPresent())
                    .map(c -> c.get().getCaption())
                    .collect(Collectors.toList());
                addBulletList(paragraph.getDocument(), cellCursor, values);
            }

            getCell(row, 2).setText(registeredCategory.getInformationPassedOn().getMessage());

            paragraph = getCell(row, 3).getParagraphs().get(0);
            try (final XmlCursor cellCursor = paragraph.getCTP().newCursor()) {
                final List<String> values = registeredCategory.getInformationReceivers().stream()
                    .map(identifier -> choiceService.getValue(identifier))
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
        addBoldTextRun("Navn", getCell(headerRow, 0).getParagraphs().get(0));
        addBoldTextRun("Leverandør", getCell(headerRow, 1).getParagraphs().get(0));
        addBoldTextRun("Er der indgået en databehandleraftale", getCell(headerRow, 2).getParagraphs().get(0));
        addBoldTextRun("Land", getCell(headerRow, 3).getParagraphs().get(0));
        addBoldTextRun("Tredjelands- overførsel", getCell(headerRow, 4).getParagraphs().get(0));
        addBoldTextRun("Acceptgrundlag", getCell(headerRow, 5).getParagraphs().get(0));

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
