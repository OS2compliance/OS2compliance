package dk.digitalidentity.report;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.BodyType;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumbering;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrGeneral;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTheme;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DocxUtil {
    public static String cTAbstractNumBulletXML =
        "<w:abstractNum xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" w:abstractNumId=\"#REPLACE_NUM_ID#\">"
            + "<w:multiLevelType w:val=\"hybridMultilevel\"/>"
            + "<w:lvl w:ilvl=\"0\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"•\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"720\" w:hanging=\"360\"/></w:pPr><w:rPr><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\" w:cs=\"Courier New\" w:hint=\"default\"/></w:rPr></w:lvl>"
            + "<w:lvl w:ilvl=\"1\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"•\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"1440\" w:hanging=\"360\"/></w:pPr><w:rPr><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\" w:cs=\"Courier New\" w:hint=\"default\"/></w:rPr></w:lvl>"
            + "<w:lvl w:ilvl=\"2\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"•\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"2160\" w:hanging=\"360\"/></w:pPr><w:rPr><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\" w:hint=\"default\"/></w:rPr></w:lvl>"
            + "</w:abstractNum>";

    public static String cTAbstractNumDecimalXML =
        "<w:abstractNum xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" w:abstractNumId=\"#REPLACE_NUM_ID#\">"
            + "<w:multiLevelType w:val=\"hybridMultilevel\"/>"
            + "<w:lvl w:ilvl=\"0\"><w:start w:val=\"1\"/><w:numFmt w:val=\"decimal\"/><w:lvlText w:val=\"%1\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"720\" w:hanging=\"360\"/></w:pPr></w:lvl>"
            + "<w:lvl w:ilvl=\"1\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"decimal\"/><w:lvlText w:val=\"%1.%2\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"1440\" w:hanging=\"360\"/></w:pPr></w:lvl>"
            + "<w:lvl w:ilvl=\"2\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"decimal\"/><w:lvlText w:val=\"%1.%2.%3\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"2160\" w:hanging=\"360\"/></w:pPr></w:lvl>"
            + "</w:abstractNum>";

    private record HeadingDefinition(int level, int sizeHalfPoints, boolean bold, boolean italic) {}

    // The report templates ship Word's stock heading ladder where Heading3+ is body-sized (or missing
    // entirely), leaving no visual hierarchy for the CKEditor content headings that HtmlToDocConverter
    // maps to Heading3-6. Heading1/2 are used by the reports' own chapter/section titles.
    // Uniform 2pt steps (20/18/16/14/12/12) so every level is visually distinguishable from the next
    private static final List<HeadingDefinition> HEADING_DEFINITIONS = List.of(
        new HeadingDefinition(1, 40, false, false),
        new HeadingDefinition(2, 36, false, false),
        new HeadingDefinition(3, 32, true, false),
        new HeadingDefinition(4, 28, true, false),
        new HeadingDefinition(5, 24, true, false),
        new HeadingDefinition(6, 24, true, true)
    );

    /**
     * Ensure the document defines all Heading1-6 paragraph styles with a visually distinct
     * size/weight ladder. Existing styles keep their template-defined color and fonts and only have
     * size, bold and italic normalized; missing styles are created with the standard heading look.
     */
    public static void normalizeHeadingStyles(final XWPFDocument document) {
        final XWPFStyles styles = document.createStyles();
        for (final HeadingDefinition definition : HEADING_DEFINITIONS) {
            final String styleId = "Heading" + definition.level();
            final XWPFStyle existingStyle = styles.getStyle(styleId);
            if (existingStyle != null) {
                applyHeadingFormat(existingStyle.getCTStyle(), definition);
            } else {
                styles.addStyle(createHeadingStyle(styleId, definition));
            }
        }
    }

    private static XWPFStyle createHeadingStyle(final String styleId, final HeadingDefinition definition) {
        final CTStyle ctStyle = CTStyle.Factory.newInstance();
        ctStyle.setType(STStyleType.PARAGRAPH);
        ctStyle.setStyleId(styleId);
        ctStyle.addNewName().setVal("heading " + definition.level());
        ctStyle.addNewBasedOn().setVal("Normal");
        ctStyle.addNewNext().setVal("Normal");
        ctStyle.addNewUiPriority().setVal(BigInteger.valueOf(9));
        ctStyle.addNewUnhideWhenUsed();
        ctStyle.addNewQFormat();

        final CTPPrGeneral ppr = ctStyle.addNewPPr();
        ppr.addNewKeepNext();
        ppr.addNewKeepLines();
        final CTSpacing spacing = ppr.addNewSpacing();
        spacing.setBefore(BigInteger.valueOf(240));
        spacing.setAfter(BigInteger.ZERO);
        ppr.addNewOutlineLvl().setVal(BigInteger.valueOf(definition.level() - 1));

        final CTRPr rpr = ctStyle.addNewRPr();
        final CTFonts fonts = rpr.addNewRFonts();
        fonts.setAsciiTheme(STTheme.MAJOR_H_ANSI);
        fonts.setEastAsiaTheme(STTheme.MAJOR_EAST_ASIA);
        fonts.setHAnsiTheme(STTheme.MAJOR_H_ANSI);
        fonts.setCstheme(STTheme.MAJOR_BIDI);
        // same blue as the heading styles already defined in the templates
        rpr.addNewColor().setVal("2F5496");

        applyHeadingFormat(ctStyle, definition);
        return new XWPFStyle(ctStyle);
    }

    private static void applyHeadingFormat(final CTStyle ctStyle, final HeadingDefinition definition) {
        final CTRPr rpr = ctStyle.isSetRPr() ? ctStyle.getRPr() : ctStyle.addNewRPr();
        final BigInteger size = BigInteger.valueOf(definition.sizeHalfPoints());
        while (rpr.sizeOfSzArray() > 0) {
            rpr.removeSz(0);
        }
        rpr.addNewSz().setVal(size);
        while (rpr.sizeOfSzCsArray() > 0) {
            rpr.removeSzCs(0);
        }
        rpr.addNewSzCs().setVal(size);
        while (rpr.sizeOfBArray() > 0) {
            rpr.removeB(0);
        }
        while (rpr.sizeOfBCsArray() > 0) {
            rpr.removeBCs(0);
        }
        if (definition.bold()) {
            rpr.addNewB();
            rpr.addNewBCs();
        }
        while (rpr.sizeOfIArray() > 0) {
            rpr.removeI(0);
        }
        while (rpr.sizeOfICsArray() > 0) {
            rpr.removeICs(0);
        }
        if (definition.italic()) {
            rpr.addNewI();
            rpr.addNewICs();
        }
    }

    public static XWPFParagraph findParagraphToReplace(final XWPFDocument document, final String placeHolder) {
        final AtomicReference<XWPFParagraph> result = new AtomicReference<>();
        document.getBodyElementsIterator().forEachRemaining(
            part -> {
                if (result.get() == null) {
                    if (part.getElementType() != BodyElementType.CONTENTCONTROL && part.getPartType() != BodyType.CONTENTCONTROL && part.getBody() != null) {
                        final List<XWPFParagraph> paragraphs = part.getBody().getParagraphs();
                        for (final XWPFParagraph paragraph : paragraphs) {
                            final String text = getParagraphText(paragraph);
                            if (placeHolder.equalsIgnoreCase(text)) {
                                result.set(paragraph);
                                break;
                            }
                        }
                    }
                }
            }
        );
        return result.get();
    }

    public static String getParagraphText(final XWPFParagraph paragraph) {
        return paragraph.getRuns().stream().map(r -> r.getText(0)).collect(Collectors.joining());
    }

    public static XWPFRun addTextRun(final String text, final XWPFParagraph paragraph) {
        final XWPFRun valueRun = paragraph.createRun();
        valueRun.setText(text);
        return valueRun;
    }

    public static XWPFRun addBoldTextRun(final String boldPart, final XWPFParagraph paragraph) {
        final XWPFRun boldRun = paragraph.createRun();
        boldRun.addBreak();
        boldRun.setText(boldPart);
        boldRun.setBold(true);
        return boldRun;
    }

    public static XmlCursor setCursorToNextStartToken(final XmlObject object) {
        final XmlCursor cursor = object.newCursor();
        advanceCursor(cursor);
        return cursor;
    }

    public static void advanceCursor(final XmlCursor cursor) {
        cursor.toEndToken();
        while(cursor.hasNextToken() && cursor.toNextToken() != org.apache.xmlbeans.XmlCursor.TokenType.START) {
        }
    }

    public static void addHtmlRun(final String html, final XWPFDocument document, final XmlCursor cursor) {
        if (Strings.isEmpty(html)) {
            return;
        }
        new HtmlToDocConverter(document, cursor).convert(html);
    }

    public static void addBulletList(final XWPFDocument document, final XmlCursor cursor,
                                      final List<String> rows) {
        try {
            final BigInteger bigInteger = generateNumbering(document, unorderedAbstractNumXml(document));
            rows.forEach(value -> {
                final XWPFParagraph paragraph = document.insertNewParagraph(cursor);
                paragraph.setNumID(bigInteger);
                paragraph.setNumILvl(BigInteger.valueOf(0));
                addTextRun(value, paragraph);
                advanceCursor(cursor);
            });
        } catch (final XmlException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Needed for generating lists, index should be unique for each list
     */
    public static String orderedAbstractNumXml(final XWPFDocument document) {
        return StringUtils.replaceOnce(cTAbstractNumDecimalXML, "#REPLACE_NUM_ID#", "" + findNextAvailNumber(document));
    }

    /**
     * Needed for generating lists, index should be unique for each list
     */
    public static String unorderedAbstractNumXml(final XWPFDocument document) {
        return StringUtils.replaceOnce(cTAbstractNumBulletXML, "#REPLACE_NUM_ID#", "" + findNextAvailNumber(document));
    }

    /**
     * Get or create a new cell in the given row
     */
    public static XWPFTableCell getCell(final XWPFTableRow row, final int cellIdx) {
        final XWPFTableCell cell = row.getCell(cellIdx);
        if (cell == null) {
            return row.addNewTableCell();
        }
        return cell;
    }

    /**
     * Generate a new numbering element in the document, and return the created number index
     */
    public static BigInteger generateNumbering(final XWPFDocument document, final String abstractNumXml) throws XmlException {
        final CTNumbering cTNumbering = CTNumbering.Factory.parse(abstractNumXml);
        final CTAbstractNum cTAbstractNum = cTNumbering.getAbstractNumArray(0);

        final XWPFAbstractNum abstractNum = new XWPFAbstractNum(cTAbstractNum);
        final XWPFNumbering numbering = document.createNumbering();

        final BigInteger abstractNumID = numbering.addAbstractNum(abstractNum);
        return numbering.addNum(abstractNumID);
    }

    /**
     * Mark all TOC fields as dirty so Word silently refreshes them when the document opens,
     * instead of showing the cached entries from the template. Walks the entire document XML
     * (including content controls / sdt blocks) because TOCs are typically wrapped in a w:sdt.
     */
    public static void markTocFieldsDirty(final XWPFDocument document) {
        final String wNs = "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' ";
        try (final XmlCursor cursor = document.getDocument().getBody().newCursor()) {
            cursor.selectPath(wNs + ".//w:instrText");
            while (cursor.toNextSelection()) {
                final XmlObject obj = cursor.getObject();
                if (!(obj instanceof CTText instrText)) {
                    continue;
                }
                final String value = instrText.getStringValue();
                if (value == null || !value.trim().startsWith("TOC")) {
                    continue;
                }
                markEnclosingFieldBeginDirty(cursor);
            }
        }
    }

    private static void markEnclosingFieldBeginDirty(final XmlCursor instrTextCursor) {
        try (final XmlCursor c = instrTextCursor.newCursor()) {
            // Walk up to the enclosing w:p, then iterate its descendants to find the BEGIN fldChar.
            while (c.toParent()) {
                final XmlObject parent = c.getObject();
                if (parent instanceof CTP ctp) {
                    for (final CTR run : ctp.getRList()) {
                        for (final CTFldChar fldChar : run.getFldCharList()) {
                            if (fldChar.getFldCharType() == STFldCharType.BEGIN) {
                                fldChar.setDirty(Boolean.TRUE);
                                return;
                            }
                        }
                    }
                    return;
                }
            }
        }
    }

    /**
     * Looks through all numberings in the document and returns the next available numbering index
     */
    public static BigInteger findNextAvailNumber(final XWPFDocument document) {
        XWPFNumbering numbering = document.getNumbering();
        if (numbering == null) {
            numbering = document.createNumbering();
        }
        BigInteger curIdx = BigInteger.ONE;
        while (numbering.getAbstractNum(curIdx) != null) {
            curIdx = curIdx.add(BigInteger.ONE);
        }
        return curIdx;

    }

}
