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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dk.digitalidentity.model.entity.enums.DocumentType.PROCEDURE;
import static dk.digitalidentity.model.entity.enums.TaskType.CHECK;
import static dk.digitalidentity.report.DocxUtil.*;

@Slf4j
@Component
public class StandardReplacer implements PlaceHolderReplacer {
	private final RelationService relationService;
	private final StandardTemplateDao standardTemplateDao;
	private static final String HEADING1 = "Heading1";
	private static final String HEADING2 = "Heading2";

	public StandardReplacer(final RelationService relationService, final StandardTemplateDao standardTemplateDao) {
		this.relationService = relationService;
		this.standardTemplateDao = standardTemplateDao;
	}

	@Override
	public boolean supports(PlaceHolder placeHolder) {
		return placeHolder == PlaceHolder.STANDARD_NAME;
	}

	@Override
	public void replace(PlaceHolder placeHolder, XWPFDocument document, Map<String, String> parameters) {
		final XWPFParagraph paragraph = findParagraphToReplace(document, placeHolder.getPlaceHolder());
		if (paragraph != null) {
			replaceParagraph(paragraph, placeHolder.getPlaceHolder());
		}
	}

	private void replaceParagraph(final XWPFParagraph p, final String placeHolder) {
		p.getRuns().stream()
				.filter(r -> placeHolder.startsWith(r.getText(0)))
				.findFirst()
				.ifPresent(xwpfRun -> insertStandard(p, xwpfRun));
	}

	private void insertStandard(final XWPFParagraph p, final XWPFRun placeHolderRun) {
		log.info("inserting standard");
		final XWPFDocument document = p.getDocument();
		// TODO: Needs to be able to take a provided ID so that it works for every standard
		final StandardTemplate standardTemplate = standardTemplateDao.findByIdentifier("Tesssst");
		// TODO: Needs to replace the starting replacer with the ID of the standard
		// TODO: Does not work :/
		// Replace entire placeholder text with the standard ID
		placeHolderRun.setText("", 0);
		placeHolderRun.setText(standardTemplate.getIdentifier(), 0); // Set full replacement

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
