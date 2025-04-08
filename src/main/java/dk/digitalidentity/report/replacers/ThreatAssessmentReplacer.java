package dk.digitalidentity.report.replacers;

import dk.digitalidentity.model.PlaceHolder;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.ScaleService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.model.RiskProfileDTO;
import dk.digitalidentity.service.model.ThreatDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.TableRowHeightRule;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTextDirection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.report.DocxService.PARAM_RISK_ASSESSMENT_ID;
import static dk.digitalidentity.report.DocxUtil.addBoldTextRun;
import static dk.digitalidentity.report.DocxUtil.addTextRun;
import static dk.digitalidentity.report.DocxUtil.advanceCursor;
import static dk.digitalidentity.report.DocxUtil.findParagraphToReplace;
import static dk.digitalidentity.report.DocxUtil.getCell;
import static dk.digitalidentity.util.NullSafe.nullSafe;

/**
 * Handles generating thread assessment report.
 * @implNote This class has grown rather large, consider refactoring and doing the word operations more generic.
 */
@Service
@RequiredArgsConstructor
public class ThreatAssessmentReplacer implements PlaceHolderReplacer {
    private final static int TWIPS_PER_INCH =  1440;
    private static final String HEADING1 = "Heading1";
    private static final String HEADING2 = "Heading2";
    private static final String HEADING3 = "Heading3";
    private static final String SMALL_TEXT = "Small";

    private final ThreatAssessmentService threatAssessmentService;
    private final ScaleService scaleService;
    private final RelationService relationService;
    private final TaskService taskService;
	private final ChoiceService choiceService;

    private static class ThreatContext {
        Asset asset;
        Register register;
        ThreatAssessment threatAssessment;
        List<RiskProfileDTO> riskProfileDTOList;
        List<Task> riskAssessmentTasks;
        List<Task> otherTasks;
		List<String> personsWithDataAccessList = new ArrayList<>();
    }

    @Override
    public boolean supports(final PlaceHolder placeHolder) {
        return placeHolder == PlaceHolder.RISK_ASSESSMENT;
    }

    @Override
    public void replace(final PlaceHolder placeHolder, final XWPFDocument document, final Map<String, String> parameters) {
        final XWPFParagraph paragraph = findParagraphToReplace(document, placeHolder.getPlaceHolder());
        if (paragraph != null) {
            if (!parameters.containsKey(PARAM_RISK_ASSESSMENT_ID)) {
                throw new RuntimeException("Risk assessment identifier missing");
            }
            final String assessmentId = parameters.get(PARAM_RISK_ASSESSMENT_ID);
            final ThreatContext context = buildThreatContext(assessmentId);
            replaceParagraph(paragraph);
            insertRiskAssessment(paragraph, context);
        }
    }

    private ThreatContext buildThreatContext(final String assessmentId) {
        final ThreatContext context = new ThreatContext();
        context.threatAssessment = threatAssessmentService.findById(Long.valueOf(assessmentId))
            .orElseThrow((() -> new RuntimeException("Risk assessment not found")));
        final List<Relatable> relations = relationService.findAllRelatedTo(context.threatAssessment);
        context.riskAssessmentTasks = relations.stream().filter(t -> t.getRelationType() == RelationType.TASK)
            .map(Task.class::cast)
            .collect(Collectors.toList());
        final List<Long> riskAssessmentTasksIds = context.riskAssessmentTasks.stream().map(Relatable::getId).toList();

        if (ThreatAssessmentType.ASSET == context.threatAssessment.getThreatAssessmentType()) {
			//Asset specific
            final Optional<Asset> asset = relations.stream().filter(r -> r.getRelationType() == RelationType.ASSET)
                .map(Asset.class::cast)
                .findFirst();
            asset.ifPresent(a -> context.otherTasks = relationService.findAllRelatedTo(a).stream()
                .filter(related -> related.getRelationType() == RelationType.TASK)
                .filter(related -> !riskAssessmentTasksIds.contains(related.getId()))
                .map(Task.class::cast)
                .filter(t -> !taskService.isTaskDone(t))
                .collect(Collectors.toList())
			);
            context.asset = asset.orElse(null);

			if (context.asset != null) {
				context.personsWithDataAccessList = context.asset.getDataProcessing().getAccessWhoIdentifiers().stream()
						.map(identifier ->
						{
							Optional<ChoiceValue> value = choiceService.getValue(identifier);
							if (value.isPresent()) {
								return value.get().getCaption();
							}
							return "Ikke udfyldt";
						}).toList();
			}

        } else if (ThreatAssessmentType.REGISTER == context.threatAssessment.getThreatAssessmentType()) {
			//Register specific
            final Optional<Register> register = relations.stream().filter(r -> r.getRelationType() == RelationType.REGISTER)
                .map(Register.class::cast)
                .findFirst();
            register.ifPresent(r -> context.otherTasks = relationService.findAllRelatedTo(r).stream()
                .filter(related -> related.getRelationType() == RelationType.TASK)
                .filter(related -> !riskAssessmentTasksIds.contains(related.getId()))
                .map(Task.class::cast)
                .filter(t -> !taskService.isTaskDone(t))
                .collect(Collectors.toList()));
            context.register = register.orElse(null);

			if (context.register != null) {
				context.personsWithDataAccessList = context.register.getDataProcessing().getAccessWhoIdentifiers().stream()
						.map(identifier ->
						{
							Optional<ChoiceValue> value = choiceService.getValue(identifier);
							if (value.isPresent()) {
								return value.get().getCaption();
							}
							return "Ikke udfyldt";
						}).toList();
			}

        }
        context.riskProfileDTOList = threatAssessmentService.buildRiskProfileDTOs(context.threatAssessment);


        return context;
    }

    private void replaceParagraph(final XWPFParagraph p) {
        p.getRuns().forEach(r -> r.setText("", 0));
    }

    private void insertRiskAssessment(final XWPFParagraph p, final ThreatContext context) {
        final XWPFDocument document = p.getDocument();

        try (final XmlCursor cursor = p.getCTP().newCursor()) {
            final XWPFParagraph title = document.insertNewParagraph(cursor);
            title.setStyle(HEADING1);
            title.setAlignment(ParagraphAlignment.CENTER);
            addTextRun("Ledelsesrapport for risikovurdering af " + context.threatAssessment.getName(), title);
            advanceCursor(cursor);

            final XWPFParagraph subTitle = document.insertNewParagraph(cursor);
            subTitle.setStyle(HEADING2);
            subTitle.setAlignment(ParagraphAlignment.CENTER);
            addTextRun(subHeading(context), subTitle);
            advanceCursor(cursor);

			addGeneralInfoSection(document, cursor, context);

            addPresentAddMeeting(document, cursor, context);
            addCriticality(document, cursor, context);
            addRiskProfile(document, cursor, context);
            addRiskExplanations(document, cursor);

            addThreatAssessmentTable(document, cursor, context);
            addTasksTable(document, cursor, context.riskAssessmentTasks, "Opgaver oprettet under risikovurderingen");
            addTasksTable(document, cursor, context.otherTasks, "Øvrige igangværende opgaver");
        }
    }

    private void addTasksTable(final XWPFDocument document, final XmlCursor cursor,
                               final List<Task> tasks, final String heading) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        createHeading3(document, cursor, heading);
        final XWPFParagraph tableParagraph = document.insertNewParagraph(cursor);
        advanceCursor(cursor);
        final XWPFTable table = tableParagraph.getBody().insertNewTbl(cursor);
        createTableCells(table, tasks.size()+1, 6);
        final XWPFTableRow headerRow = table.getRow(0);
        setCellHeaderTextSmall(headerRow, 0, "Opgave navn");
        setCellHeaderTextSmall(headerRow, 1, "Beskrivelse");
        setCellHeaderTextSmall(headerRow, 2, "Frekvens");
        setCellHeaderTextSmall(headerRow, 3, "Deadline");
        setCellHeaderTextSmall(headerRow, 4, "Ansvarlig");
        setCellHeaderTextSmall(headerRow, 5, "Afdeling");

        final int[] idx = { 1 };
        tasks.forEach(
            task -> {
                final XWPFTableRow row = table.getRow(idx[0]);
                setCellTextSmall(row, 0, task.getName());
                setCellTextSmall(row, 1, task.getDescription());
                setCellTextSmall(row, 2, task.getTaskType().getMessage());
                setCellTextSmall(row, 3, DK_DATE_FORMATTER.format(task.getNextDeadline()));
                setCellTextSmall(row, 4, nullSafe(() -> task.getResponsibleUser().getName()));
                setCellTextSmall(row, 5, nullSafe(() -> task.getResponsibleOu().getName()));
                idx[0]++;
            }
        );
        advanceCursor(cursor);
    }

    private void addThreatAssessmentTable(final XWPFDocument document, final XmlCursor cursor, final ThreatContext context) {
        createHeading3(document, cursor, threatAssessmentHeading(context));

        final XWPFParagraph tableParagraph = document.insertNewParagraph(cursor);
        advanceCursor(cursor);
        final XWPFTable table = tableParagraph.getBody().insertNewTbl(cursor);
        final Map<String, List<ThreatDTO>> threatList = threatAssessmentService.buildThreatList(context.threatAssessment);
        createTableCells(table, context.riskProfileDTOList.size() + 1, 10);

        final XWPFTableRow headerRow = table.getRow(0);
        setCellHeaderTextSmall(headerRow, 0, "Nr.");
        setCellHeaderTextSmall(headerRow, 1, "Type af trussel");
        setCellHeaderTextSmall(headerRow, 2, "Trussel");
        setCellHeaderTextSmall(headerRow, 3, "Sandsyn\nlighed");
        setCellHeaderTextSmall(headerRow, 4, "Højeste\nkonsekv\nens");
        setCellHeaderTextSmall(headerRow, 5, "Risiko\nscore");
        setCellHeaderTextSmall(headerRow, 6, "Problemstilling");
        setCellHeaderTextSmall(headerRow, 7, "Eksisterende foranstaltninger");
        setCellHeaderTextSmall(headerRow, 8, "Risikohåndtering");
        setCellHeaderTextSmall(headerRow, 9, "Uddybning af risikohåndtering");

        final Map<String, String> colorMap = scaleService.getScaleRiskScoreColorMap();

        final int[] idx = { 1 };
        threatList.forEach((threatType, threats) -> {
            threats.forEach(t -> {
                final XWPFTableRow row = table.getRow(idx[0]);
                final RiskProfileDTO profile = context.riskProfileDTOList.stream()
                    .filter(rp -> rp.getIndex() == t.getIndex())
                    .findFirst().orElse(null);
                if (profile != null) {
                    setCellTextSmall(row, 0, "" + (t.getIndex() + 1));
                    setCellTextSmall(row, 1, threatType);
                    setCellTextSmall(row, 2, t.getThreat());

                    final String color = colorMap.get(profile.getConsequence() + "," + profile.getProbability());
                    final int score = profile.getProbability() * profile.getConsequence();
                    setCellTextSmallCentered(row, 3, "" + profile.getProbability());
                    setCellTextSmallCentered(row, 4, "" + profile.getConsequence());
                    setCellTextSmallCentered(row, 5, "" + score);
                    setCellBackgroundColor(row.getCell(5), color);
                    setCellTextSmall(row, 6, t.getProblem());
                    setCellTextSmall(row, 7, t.getExistingMeasures());
                    setCellTextSmall(row, 8, t.getMethod() != null ? t.getMethod().getMessage() : "");
                    setCellTextSmall(row, 9, t.getElaboration());
                    idx[0]++;
                }
            });
        });
        advanceCursor(cursor);
    }

    private void createHeading3(final XWPFDocument document, final XmlCursor cursor, final String text) {
        final XWPFParagraph spacerParagraph = document.insertNewParagraph(cursor);
        addTextRun("", spacerParagraph);
        advanceCursor(cursor);
        final XWPFParagraph headerParagraph = document.insertNewParagraph(cursor);
        headerParagraph.setStyle(HEADING3);
        addTextRun(text, headerParagraph);
        advanceCursor(cursor);
    }

    private static void setCellHeaderTextSmall(final XWPFTableRow headerRow, final int cellIdx, final String text) {
        final XWPFParagraph paragraph = getCell(headerRow, cellIdx).getParagraphs().get(0);
        paragraph.setStyle(SMALL_TEXT);
        addTextRun(text, paragraph).setBold(true);
    }

    private static XWPFRun setCellTextSmall(final XWPFTableRow row, final int cellIdx, final String text) {
        final XWPFTableCell cell = getCell(row, cellIdx);
        final XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.setStyle(SMALL_TEXT);
        return addTextRun(text, paragraph);
    }

    private static void setCellTextSmallCentered(final XWPFTableRow row, final int cellIdx, final String text) {
        final XWPFTableCell cell = getCell(row, cellIdx);
        final XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setStyle(SMALL_TEXT);
        addTextRun(text, paragraph);
    }

    private void addRiskExplanations(final XWPFDocument document, final XmlCursor cursor) {
        final XWPFParagraph tableParagraph = document.insertNewParagraph(cursor);
        tableParagraph.setAlignment(ParagraphAlignment.CENTER);
        final ScaleService.ScaleSetting scaleExplainers =
            ScaleService.scaleSettingsForType(scaleService.getScaleType());
        advanceCursor(cursor);
        final XWPFTable table = tableParagraph.getBody().insertNewTbl(cursor);
        table.setTableAlignment(TableRowAlign.CENTER);
        createTableCells(table, 1, 5);
        final XWPFTableRow row = table.getRow(0);
        addExplainerBox(getCell(row, 0), "Sandsynlighed skala", scaleExplainers.getProbabilityScore());
        getCell(row, 1).setText("");
        addExplainerBox(getCell(row, 2), "Konsekvens skala", scaleExplainers.getConsequenceNumber());
        getCell(row, 3).setText("");
        addExplainerBox(getCell(row, 4), "Risikoscore skala", scaleExplainers.getRiskScore());
        setTableBorders(table, XWPFTable.XWPFBorderType.NONE);
        advanceCursor(cursor);
    }

    private void addExplainerBox(final XWPFTableCell cell, final String header, final List<String> lines) {
        try (final XmlCursor cursor1 = cell.getParagraphs().get(0).getCTP().newCursor()) {
            final XWPFTable table1 = cell.insertNewTbl(cursor1);
            table1.removeRow(0); // workaround poi bug
            final XWPFTableRow row = table1.createRow();

            final XWPFParagraph paragraph = getCell(row, 0).getParagraphs().get(0);
            paragraph.setStyle(SMALL_TEXT);
            final XWPFRun headerRun = addBoldTextRun(header, paragraph);
            headerRun.setBold(true);
            headerRun.addBreak();
            lines.forEach(p -> addTextRun(p, paragraph).addBreak());

            setTableBorders(table1, XWPFTable.XWPFBorderType.THICK);
        }

    }

    private void addRiskProfile(final XWPFDocument document, final XmlCursor cursor,
                                final ThreatContext context) {
        final XWPFParagraph heading = document.insertNewParagraph(cursor);
        heading.setStyle(HEADING3);
        addTextRun(riskProfileHeading(context), heading);
        advanceCursor(cursor);

        final XWPFParagraph tableParagraph = document.insertNewParagraph(cursor);
        advanceCursor(cursor);
        final XWPFTable outerTable = tableParagraph.getBody().insertNewTbl(cursor);
        outerTable.removeRow(0);
        createTableCells(outerTable, 3, 4);
        final XWPFTableCell headerCell1 = getCell(outerTable.getRow(0), 1);
        headerCell1.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        addTextRun("Risikoprofil før risikohåndtering", headerCell1.getParagraphs().get(0)).setBold(true);
        // Att som breaks in the corners to better center the heading and bottom legend
        addTextRun("", getCell(outerTable.getRow(0), 0).getParagraphs().get(0)).addBreak();
        addTextRun("", getCell(outerTable.getRow(2), 0).getParagraphs().get(0)).addBreak();
        final XWPFTableCell leftCell = getCell(outerTable.getRow(1), 1);
        final XWPFParagraph leftParagraph = leftCell.getParagraphs().get(0);
        try (final XmlCursor leftCursor = leftParagraph.getCTP().newCursor()) {
            final XWPFTable beforeTable = leftCell.insertNewTbl(leftCursor);
            createRiskGrid(beforeTable);
            addThreatValues(beforeTable, context.riskProfileDTOList, false);
        }
        final XWPFTableCell headerCell2 = getCell(outerTable.getRow(0), 3);
        headerCell2.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        addTextRun("Risikoprofil efter risikohåndtering", headerCell2.getParagraphs().get(0)).setBold(true);

        final XWPFTableCell rightCell = getCell(outerTable.getRow(1), 3);
        final XWPFParagraph rightParagraph = rightCell.getParagraphs().get(0);
        try (final XmlCursor rightCursor = rightParagraph.getCTP().newCursor()) {
            final XWPFTable afterTable = rightCell.insertNewTbl(rightCursor);
            createRiskGrid(afterTable);
            addThreatValues(afterTable, context.riskProfileDTOList, true);
        }
        addLegends(outerTable);
        setTableBorders(outerTable, XWPFTable.XWPFBorderType.NONE);
        advanceCursor(cursor);
    }

    private static void createTableCells(final XWPFTable table, final int rows, final int columns) {
        IntStream.range(0, rows)
            .mapToObj(i -> table.getRow(i) != null ? table.getRow(i) : table.createRow())
            .forEach(row -> IntStream.range(0, columns).forEach(i -> getCell(row, i)));
    }

    private static void addLegends(final XWPFTable table) {
        final XWPFTableRow bottomRow = table.getRows().get(table.getNumberOfRows() - 1);
        final XWPFTableCell leftLegendCell = getCell(table.getRow(1), 0);
        final XWPFTableCell rightLegendCell = getCell(table.getRow(1), 2);
        final XWPFTableCell bottomLeftLegendCell = getCell(bottomRow, 1);
        final XWPFTableCell bottomRightLegendCell = getCell(bottomRow, 3);
        setVerticalCellText(leftLegendCell);
        setVerticalCellText(rightLegendCell);
        final XWPFParagraph bottomLeftLegendParagraph = bottomLeftLegendCell.getParagraphs().get(0);
        bottomLeftLegendParagraph.setAlignment(ParagraphAlignment.CENTER);
        bottomLeftLegendCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        bottomLeftLegendCell.setText("Sandsynlighed");

        final XWPFParagraph bottomRightLegendParagraph = bottomRightLegendCell.getParagraphs().get(0);
        bottomRightLegendParagraph.setAlignment(ParagraphAlignment.CENTER);
        bottomRightLegendCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        bottomRightLegendCell.setText("Sandsynlighed");
        leftLegendCell.setWidthType(TableWidthType.DXA);
        leftLegendCell.setWidth("" + (TWIPS_PER_INCH / 10));
        rightLegendCell.setWidthType(TableWidthType.DXA);
        rightLegendCell.setWidth("" + (TWIPS_PER_INCH / 10));
    }

    private static void setVerticalCellText(final XWPFTableCell leftLegendCell) {
        leftLegendCell.getCTTc().addNewTcPr().addNewTextDirection().setVal(STTextDirection.BT_LR);
        final XWPFParagraph paragraph = leftLegendCell.getParagraphArray(0);
        final XWPFRun run = paragraph.createRun();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        run.setText("Konsekvens");
    }

    private void addThreatValues(final XWPFTable table, final List<RiskProfileDTO> profiles, final boolean afterMitigation) {
        final List<XWPFTableRow> rows = table.getRows();
        for (final RiskProfileDTO riskProfileDTO : profiles) {
            int probability = riskProfileDTO.getProbability() ;
            int consequence = riskProfileDTO.getConsequence();
            String postFix = "";
            if (afterMitigation && riskProfileDTO.getResidualProbability() > -1) {
                postFix = "*";
                probability = riskProfileDTO.getResidualProbability();
            }
            if (afterMitigation && riskProfileDTO.getResidualConsequence() > -1) {
                postFix = "*";
                consequence = riskProfileDTO.getResidualConsequence();
            }
            final int rowIndex = (rows.size() - 1) - consequence;
            final XWPFTableRow row = rows.get(rowIndex);
            final XWPFTableCell cell = row.getCell(probability);
            if (StringUtils.length(cell.getText()) > 0) {
                setCellTextSmall(row, probability, ", " + (riskProfileDTO.getIndex()+1) + postFix).setBold(true);
            } else {
                setCellTextSmall(row, probability, (riskProfileDTO.getIndex()+1) + postFix).setBold(true);
            }
        }
    }

    private static void setTableBorders(final XWPFTable table, final XWPFTable.XWPFBorderType borderType) {
        table.setInsideHBorder(borderType, 1, 0, "000000");
        table.setInsideVBorder(borderType, 1, 0, "000000");
        table.setBottomBorder(borderType, 1, 0, "000000");
        table.setLeftBorder(borderType, 1, 0, "000000");
        table.setTopBorder(borderType, 1, 0, "000000");
        table.setRightBorder(borderType, 1, 0, "000000");
    }

    private void createRiskGrid(final XWPFTable table) {
        final Map<String, String> colorMap = scaleService.getScaleRiskScoreColorMap();
        /*
         Generate a table that look like this
         |4|  |  |  |  |
         |3|  |  |  |  |
         |2|  |  |  |  |
         |1|  |  |  |  |
         | | 1| 2| 3| 4|
        */
        // Insert the first row which should be 5 or 11 merged cells (depending on settings)
        final int gridSize = scaleService.getConsequenceScale().size() + 1;
        // There is a BUG somewhere in POI.
        // The table starts with a row in its internal list, but is not in the xml, this cause issues
        // It turns out removing and re adding the row clears the problem.
        table.removeRow(0);
        createTableCells(table, gridSize, gridSize);
        setTableBorders(table, XWPFTable.XWPFBorderType.SINGLE);
        for (int i=0; i < gridSize-1; ++i) {
            final XWPFTableRow row = table.getRow(i);
            row.setHeightRule(TableRowHeightRule.AT_LEAST);
            row.setHeight((int) (TWIPS_PER_INCH / 1.8));
        }

        for (final Map.Entry<String, String> cellEntry : colorMap.entrySet()) {
            final String[] rowCell = StringUtils.split(cellEntry.getKey(), ",");
            final int entryRowCnt = Integer.parseInt(rowCell[0])-1;
            final int entryCellCnt = Integer.parseInt(rowCell[1]);
            final XWPFTableRow currentRow = table.getRow((gridSize - entryRowCnt) - 2);

            final XWPFTableCell cell = getCell(currentRow, entryCellCnt);
            cell.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            cell.setWidthType(TableWidthType.DXA);
            cell.setWidth("" + TWIPS_PER_INCH);
            setCellBackgroundColor(cell, cellEntry.getValue());
        }
        // Add scales
        final XWPFTableRow footerRow = table.getRow(table.getNumberOfRows()-1);
        for (int i = 0; i < gridSize; ++i) {
            final XWPFTableCell footer = getCell(footerRow, i);
            setCellBackgroundColor(footer, "#393939");
            footer.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            footer.setText(i > 0 ? "" + i : "");
            footer.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            final XWPFTableCell leftScaleCell = getCell(table.getRow(i), 0);
            setCellBackgroundColor(leftScaleCell, "#393939");
            leftScaleCell.setText(i == (gridSize-1 ) ? "" : "" + (gridSize - (i+1)));
            leftScaleCell.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
            leftScaleCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        }
    }

    private void setCellBackgroundColor(final XWPFTableCell cell, final String color) {
        cell.getCTTc().addNewTcPr().addNewShd().setFill(StringUtils.removeStart(color, "#"));
    }

    private void addPresentAddMeeting(final XWPFDocument document, final XmlCursor cursor, final ThreatContext context) {
        final List<User> present = context.threatAssessment.getPresentAtMeeting();
        if (present == null || present.isEmpty()) {
            return;
        }
        final XWPFParagraph heading = document.insertNewParagraph(cursor);
        heading.setStyle(HEADING3);
        addTextRun("Tilstede på mødet", heading);
        advanceCursor(cursor);
        final XWPFParagraph plain = document.insertNewParagraph(cursor);
        final String presentJoined = present.stream().map(User::getName).collect(Collectors.joining(", "));
        addTextRun(presentJoined, plain).addBreak();
        advanceCursor(cursor);
    }

    private void addCriticality(final XWPFDocument document, final XmlCursor cursor, final ThreatContext context) {
        if (context.asset != null || context.register != null) {
            final XWPFParagraph heading = document.insertNewParagraph(cursor);
            heading.setStyle(HEADING3);
            addTextRun("Kritikalitet", heading);
            advanceCursor(cursor);
            final XWPFParagraph plain = document.insertNewParagraph(cursor);
            if (context.asset != null) {
                addTextRun("Systemet er: " + (context.asset.getCriticality() != null ?
                    context.asset.getCriticality().getMessage() : "Ikke udfyldt"), plain)
                    .addBreak();
                addTextRun("Nødplan: " + (context.asset.getEmergencyPlanLink() != null ?
                    context.asset.getEmergencyPlanLink() : "Ikke udfyldt"), plain).addBreak();
                advanceCursor(cursor);
            } else {
                addTextRun("Behandlingsaktiviteten er: " + (context.register.getCriticality() != null ?
                    context.register.getCriticality().getMessage() : "Ikke udfyldt"), plain)
                    .addBreak();
                addTextRun("Nødplan: " + (context.register.getEmergencyPlanLink() != null ?
                    context.register.getEmergencyPlanLink() : "Ikke udfyldt"), plain).addBreak();
                advanceCursor(cursor);
            }
        }
    }

	public record registeredDataCategory(String title, List<String> types) {
	}

	private void addGeneralInfoSection(final XWPFDocument document, final XmlCursor cursor, final ThreatContext context) {
		if (context.asset != null || context.register != null) {
			boolean isAsset = context.asset != null;
			DataProcessing dataProcessing = isAsset ? context.asset.getDataProcessing() : context.register.getDataProcessing();
			List<registeredDataCategory> categories = dataProcessing.getRegisteredCategories().stream().map(cat ->
					{
						Optional<ChoiceValue> title = choiceService.getValue(cat.getPersonCategoriesRegisteredIdentifier());
						List<String> types = cat.getPersonCategoriesInformationIdentifiers().stream().map(type -> Objects.requireNonNull(choiceService.getValue(type).orElse(null)).getCaption())
								.filter(Objects::nonNull)
								.toList();
						if (title.isEmpty()) {
							return null;
						}
						return new registeredDataCategory(title.get().getCaption(), types);
					})
					.filter(Objects::nonNull)
					.toList();

			final XWPFParagraph heading = document.insertNewParagraph(cursor);
			heading.setStyle(HEADING3);
			addTextRun("Systeminformation", heading);
			advanceCursor(cursor);

			final XWPFParagraph tableParagraph = document.insertNewParagraph(cursor);
			tableParagraph.setStyle(SMALL_TEXT);
			tableParagraph.setAlignment(ParagraphAlignment.CENTER);

			advanceCursor(cursor);
			final XWPFTable table = tableParagraph.getBody().insertNewTbl(cursor);
			table.setTableAlignment(TableRowAlign.LEFT);

			createTableCells(table, 8 + categories.size(), 2);
			final XWPFTableRow row = table.getRow(0);

			//System type
			setCellTextSmall(row, 0, "Systemtype:");
			setCellTextSmall(row, 1, isAsset ? context.asset.getAssetType().getMessage() : "Fortegnelse");

			//System owners
			final XWPFTableRow row1 = table.getRow(1);
			String systemOwners = isAsset ?
					context.asset.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "))
					: context.register.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "));
			;
			setCellTextSmall(row1, 0, "Systemejere:");
			setCellTextSmall(row1, 1, systemOwners.isBlank() ? "Ikke udfyldt" : systemOwners);

			//System responsible
			final XWPFTableRow row2 = table.getRow(2);
			setCellTextSmall(row2, 0, "Systemansvarlige:");
			setCellTextSmall(row2, 1, isAsset ? context.asset.getManagers().stream().map(User::getName).collect(Collectors.joining(", ")) : "");

			//Suppliers
			final XWPFTableRow row3 = table.getRow(3);
			setCellTextSmall(row3, 0, "Leverandør:");
			setCellTextSmall(row3, 1, isAsset && context.asset.getSupplier() != null ? context.asset.getSupplier().getName() : "");

			//Who has access?
			final XWPFTableRow row4 = table.getRow(4);
			setCellTextSmall(row4, 0, "Hvem har adgang til personoplysningerne?:");
			setCellTextSmall(row4, 1, String.join(", ", context.personsWithDataAccessList));

			//Access count
			final XWPFTableRow row5 = table.getRow(5);
			setCellTextSmall(row5, 0, "Hvor mange har adgang til personoplysningerne?:");
			var accessCount = choiceService.getValue(dataProcessing.getAccessCountIdentifier());
			setCellTextSmall(row5, 1, accessCount.isPresent() ? accessCount.get().getCaption() : "");

			//Deletion procedure?
			final XWPFTableRow row6 = table.getRow(6);
			setCellTextSmall(row6, 0, "Sletteprocedure udarbejdet?:");
			setCellTextSmall(row6, 1, dataProcessing.getDeletionProcedure() != null ? dataProcessing.getDeletionProcedure().getMessage() : "Ikke udfyldt");

			//Deletion procedure link
			final XWPFTableRow row7 = table.getRow(7);
			setCellTextSmall(row7, 0, "Link til sletteprocedure");
			setCellTextSmall(row7, 1, dataProcessing.getDeletionProcedureLink() != null ? dataProcessing.getDeletionProcedureLink() : "");

			// Registered data categories
			for (int i = 0; i < categories.size(); i++) {
				final XWPFTableRow catRow = table.getRow(i + 8); // Add magic number of previous rows to start at the current row
				if (i == 0) {
					setCellTextSmall(catRow, 0, "Registrerede persondatakategorier:");
				}
				setCellTextSmall(catRow, 1, categories.get(i).title);
				final XWPFTableCell cell2 = getCell(catRow, 2);
				final XWPFParagraph paragraph = cell2.getParagraphs().getFirst();
				paragraph.setStyle(SMALL_TEXT);
				for(var type : categories.get(i).types){
					XWPFRun run = paragraph.createRun();
					run.setText(type.trim());
					run.addBreak();
				}
			}
			if (categories.isEmpty()) {
				final XWPFTableRow row8 = table.createRow();
				setCellTextSmall(row8, 0, "Registrerede persondatakategorier:");
			}

			setTableBorders(table, XWPFTable.XWPFBorderType.NONE);
			advanceCursor(cursor);
			advanceCursor(cursor);
		}
	}

    private String riskProfileHeading(final ThreatContext context) {
        return "Risikoprofil for "  + context.threatAssessment.getName();
    }

    private String threatAssessmentHeading(final ThreatContext context) {
        return "Trusselsvurdering for "  + context.threatAssessment.getName();
    }

    private String subHeading(final ThreatContext context) {
        if (context.asset != null && context.asset.getResponsibleUsers() != null && !context.asset.getResponsibleUsers().isEmpty()) {
            return "Systemejere: " + context.asset.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "));
        } else if (context.register != null && context.register.getResponsibleUsers() != null && !context.register.getResponsibleUsers().isEmpty()) {
            return "Behandlingsansvarlige: " + context.register.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "));
        }
        if (context.threatAssessment.getResponsibleUser() != null) {
            return "Risikoejer: " + context.threatAssessment.getResponsibleUser().getName();
        }
        return "Risikoejer ikke udfyldt";
    }
}
