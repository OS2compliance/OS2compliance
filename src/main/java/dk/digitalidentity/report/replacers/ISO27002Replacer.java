package dk.digitalidentity.report.replacers;

import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.model.PlaceHolder;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.report.DocxUtil;
import dk.digitalidentity.service.RelationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static dk.digitalidentity.model.entity.enums.DocumentType.PROCEDURE;
import static dk.digitalidentity.model.entity.enums.TaskType.CHECK;
import static dk.digitalidentity.report.DocxUtil.addBoldTextRun;
import static dk.digitalidentity.report.DocxUtil.addHtmlRun;
import static dk.digitalidentity.report.DocxUtil.addTextRun;
import static dk.digitalidentity.report.DocxUtil.advanceCursor;
import static dk.digitalidentity.report.DocxUtil.findParagraphToReplace;
import static dk.digitalidentity.report.DocxUtil.setCursorToNextStartToken;

@Slf4j
@Component
public class ISO27002Replacer implements PlaceHolderReplacer {
    private final RelationService relationService;
    private static final String HEADING1 = "Heading1";
    private static final String HEADING2 = "Heading2";

    private final StandardTemplateDao standardTemplateDao;

    public ISO27002Replacer(final RelationService relationService, final StandardTemplateDao standardTemplateDao) {
        this.relationService = relationService;
        this.standardTemplateDao = standardTemplateDao;
    }

    @Override
    public boolean supports(final PlaceHolder placeHolder) {
        return placeHolder == PlaceHolder.ISO27002;
    }

    @Override
    @Transactional
    public void replace(final PlaceHolder placeHolder, final XWPFDocument document) {
        final XWPFParagraph paragraph = findParagraphToReplace(document, placeHolder.getPlaceHolder());
        if (paragraph != null) {
            replaceParagraph(paragraph, placeHolder.getPlaceHolder());
        }
    }

    private void replaceParagraph(final XWPFParagraph p, final String placeHolder) {
        p.getRuns().stream()
            .filter(r -> placeHolder.equalsIgnoreCase(r.getText(0)))
            .findFirst()
            .ifPresent(xwpfRun -> insertISO27002(p, xwpfRun));
    }

    private void insertISO27002(final XWPFParagraph p, final XWPFRun placeHolderRun) {
        final XWPFDocument document = p.getDocument();
        final StandardTemplate standardTemplate = standardTemplateDao.findByIdentifier("iso27002_2022");
        placeHolderRun.setText("", 0); // clear existing text(placeholder)
        try (final XmlCursor cursor = setCursorToNextStartToken(p.getCTP())) {
            final List<StandardTemplateSection> stdTemplateSectionsOrdered = standardTemplate.getStandardTemplateSections();
            stdTemplateSectionsOrdered.sort(Comparator.comparing(StandardTemplateSection::getSortKey));
            for (final StandardTemplateSection section : stdTemplateSectionsOrdered) {
                if (section.getParent() == null) {
                    final XWPFParagraph title = document.insertNewParagraph(cursor);
                    title.setStyle(HEADING1);
                    addTextRun(section.getSection() + " " + section.getDescription(), title);
                    advanceCursor(cursor);
                    final List<StandardTemplateSection> subSections = section.getChildren()
                        .stream().sorted(Comparator.comparingInt(StandardTemplateSection::getSortKey)).toList();
                    for (final StandardTemplateSection subSection : subSections) {
                        insertSection(document, cursor, subSection);
                    }
                }
            }
        }
    }

    private void insertSection(final XWPFDocument document, final XmlCursor cursor, final StandardTemplateSection section) {
        if (section.getStandardSection() == null || !section.getStandardSection().isSelected()) {
            return;
        }
        final XWPFParagraph title = document.insertNewParagraph(cursor);
        title.setStyle(HEADING2);
        addTextRun(section.getStandardSection().getName(), title);
        advanceCursor(cursor);
        insertBoldParagraph(document, cursor, "Efterlevelse af kravet:");
        addHtmlRun(section.getStandardSection().getDescription(), document, cursor);

        final List<Relatable> allRelatedTo = relationService.findAllRelatedTo(section.getStandardSection());
        final List<Relatable> procedures = allRelatedTo.stream().filter(r -> RelationType.DOCUMENT.equals(r.getRelationType()))
            .filter(r -> PROCEDURE.equals(((Document)r).getDocumentType()))
            .toList();
        insertBoldParagraph(document, cursor, "Procedurer");
        addBulletList(document, cursor, procedures, "Ingen procedure til dette krav");

        final List<Relatable> controls = allRelatedTo.stream().filter(r -> RelationType.TASK.equals(r.getRelationType()))
            .filter(r -> CHECK.equals(((Task)r).getTaskType()))
            .toList();
        insertBoldParagraph(document, cursor, "Kontroller");
        addBulletList(document, cursor, controls, "Ingen kontroller til dette krav");

        final List<Relatable> relations = allRelatedTo.stream().filter(r -> RelationType.STANDARD_SECTION.equals(r.getRelationType()))
            .toList();
        insertBoldParagraph(document, cursor, "Relationer");
        addBulletList(document, cursor, relations, "Ingen relationer");
        document.insertNewParagraph(cursor);
        advanceCursor(cursor);
    }

    private static XWPFParagraph insertBoldParagraph(final XWPFDocument document, final XmlCursor cursor,
                                                     final String boldPart) {
        final XWPFParagraph paragraph = document.insertNewParagraph(cursor);
        addBoldTextRun(boldPart, paragraph);
        advanceCursor(cursor);
        return paragraph;
    }

    private void addBulletList(final XWPFDocument document, final XmlCursor cursor,
                               final List<Relatable> items, final String emptyText) {
        if (items.isEmpty()) {
            DocxUtil.addBulletList(document, cursor, Collections.singletonList(emptyText));
        } else {
            DocxUtil.addBulletList(document, cursor, items.stream().map(Relatable::getName).collect(Collectors.toList()));
        }
    }


}
