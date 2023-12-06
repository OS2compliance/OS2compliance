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
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumbering;

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
        final Document htmlDocument = Jsoup.parseBodyFragment(html);
        final Element body = htmlDocument.body();
        final HtmlToDocVisitor visitor = new HtmlToDocVisitor(document, cursor);
        body.traverse(visitor);
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
