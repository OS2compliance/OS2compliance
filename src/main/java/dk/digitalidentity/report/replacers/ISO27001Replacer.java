package dk.digitalidentity.report.replacers;

import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.model.PlaceHolder;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.report.DocxUtil.addHtmlRun;
import static dk.digitalidentity.report.DocxUtil.addTextRun;
import static dk.digitalidentity.report.DocxUtil.advanceCursor;
import static dk.digitalidentity.report.DocxUtil.findParagraphToReplace;
import static dk.digitalidentity.report.DocxUtil.setCursorToNextStartToken;

@Slf4j
@Component
public class ISO27001Replacer implements PlaceHolderReplacer {
    private static final String HEADING1 = "Heading1";
    private static final String HEADING2 = "Heading2";
    private static final String HEADING3 = "Heading3";

    private final StandardTemplateDao standardTemplateDao;

    public ISO27001Replacer(final StandardTemplateDao standardTemplateDao) {
        this.standardTemplateDao = standardTemplateDao;
    }

    @Override
    public boolean supports(final PlaceHolder placeHolder) {
        return placeHolder == PlaceHolder.STANDARDS;
    }

    @Override
    @Transactional
    public void replace(final PlaceHolder placeHolder, final XWPFDocument document, final Map<String, String> parameters) {
        final XWPFParagraph paragraph = findParagraphToReplace(document, placeHolder.getPlaceHolder());
        if (paragraph != null) {
            replaceParagraph(paragraph, placeHolder.getPlaceHolder());
        }
    }

    private void replaceParagraph(final XWPFParagraph p, final String placeHolder) {
        p.getRuns().stream()
            .filter(r -> placeHolder.equalsIgnoreCase(r.getText(0)))
            .findFirst()
            .ifPresent(xwpfRun -> insertISO27001(p, xwpfRun));
    }

    private void insertISO27001(final XWPFParagraph p, final XWPFRun placeHolderRun) {
        final StandardTemplate standardTemplate = standardTemplateDao.findByIdentifier("iso27001");
        placeHolderRun.setText("", 0); // clear existing text(placeholder)
        try (final XmlCursor cursor = setCursorToNextStartToken(p.getCTP())) {

            final List<StandardTemplateSection> stdTemplateSectionsOrdered = standardTemplate.getStandardTemplateSections();

            stdTemplateSectionsOrdered.sort(Comparator.comparing(StandardTemplateSection::getSortKey));
            for (final StandardTemplateSection section : stdTemplateSectionsOrdered) {
                insertStandard(p.getDocument(), cursor, section);
            }
        }
    }

    private void insertStandard(final XWPFDocument document, final XmlCursor cursor, final StandardTemplateSection section) {
        final XWPFParagraph title = document.insertNewParagraph(cursor);
        title.setStyle(HEADING1);
        addTextRun("Kap. " + section.getSection() + ": " + section.getDescription(), title);
        advanceCursor(cursor);

        if (section.getChildren() != null) {
            final List<StandardTemplateSection> children = new ArrayList<>(section.getChildren());
            children.sort(Comparator.comparing(StandardTemplateSection::getSortKey));
            for (final StandardTemplateSection childStandard : children) {
                final XWPFParagraph subTitle = document.insertNewParagraph(cursor);
                subTitle.setStyle(HEADING2);
                addTextRun(childStandard.getSection() + ". " + childStandard.getDescription(), subTitle);
                advanceCursor(cursor);
                addHtmlRun(childStandard.getStandardSection().getDescription(), document, cursor);
            }
        }
    }

}
