package dk.digitalidentity.report;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.xmlbeans.XmlCursor;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link HtmlToDocConverter}
 */
public class HtmlToDocConverterTest {

    private XWPFDocument convert(final String html) {
        final XWPFDocument document = new XWPFDocument();
        final XWPFParagraph anchor = document.createParagraph();
        try (final XmlCursor cursor = anchor.getCTP().newCursor()) {
            DocxUtil.addHtmlRun(html, document, cursor);
        }
        return document;
    }

    /** All paragraphs except the anchor paragraph the test inserts at. */
    private List<XWPFParagraph> contentParagraphs(final XWPFDocument document) {
        final List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.subList(0, paragraphs.size() - 1);
    }

    @Test
    public void inlineFormattingStaysInOneParagraph() {
        final XWPFDocument document = convert("<p>Hello <strong>bold</strong> and <em>italic</em> world</p>");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(1, paragraphs.size(), "inline formatting must not split the paragraph");
        assertEquals("Hello bold and italic world", paragraphs.get(0).getText());
        assertTrue(paragraphs.get(0).getRuns().stream().anyMatch(r -> r.isBold() && "bold".equals(r.text())));
        assertTrue(paragraphs.get(0).getRuns().stream().anyMatch(r -> r.isItalic() && "italic".equals(r.text())));
    }

    @Test
    public void emptyParagraphsAndStrayBreaksAreRemoved() {
        final XWPFDocument document = convert("<p>First</p><p>&nbsp;</p><br><p><br></p><p>Second</p>");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(2, paragraphs.size());
        assertEquals("First", paragraphs.get(0).getText());
        assertEquals("Second", paragraphs.get(1).getText());
    }

    @Test
    public void headingsGetMappedStyles() {
        final XWPFDocument document = convert("<h1>One</h1><h2>Two</h2><h5>Five</h5>");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(3, paragraphs.size());
        assertEquals("Heading3", paragraphs.get(0).getStyle());
        assertEquals("Heading4", paragraphs.get(1).getStyle());
        assertEquals("Heading6", paragraphs.get(2).getStyle());
    }

    @Test
    public void nestedListsKeepLevelsAndNumbering() {
        final XWPFDocument document = convert("<ul><li>a</li><li>b<ul><li>c</li></ul></li></ul>");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(3, paragraphs.size());
        assertNotNull(paragraphs.get(0).getNumID());
        assertEquals(BigInteger.ZERO, paragraphs.get(0).getNumIlvl());
        assertEquals(BigInteger.ZERO, paragraphs.get(1).getNumIlvl());
        assertEquals(BigInteger.ONE, paragraphs.get(2).getNumIlvl());
        assertEquals(paragraphs.get(1).getNumID(), paragraphs.get(2).getNumID(), "nested same-type list reuses the numbering instance");
    }

    @Test
    public void orderedAndUnorderedListsGetSeparateNumbering() {
        final XWPFDocument document = convert("<ol><li>one</li></ol><ul><li>bullet</li></ul>");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(2, paragraphs.size());
        assertNotEquals(paragraphs.get(0).getNumID(), paragraphs.get(1).getNumID());
    }

    @Test
    public void linksBecomeInlineHyperlinks() {
        final XWPFDocument document = convert("<p>See <a href=\"www.example.com\">the site</a> here</p>");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(1, paragraphs.size(), "link must stay inline in its paragraph");
        assertEquals("See the site here", paragraphs.get(0).getText());
        assertEquals(1, paragraphs.get(0).getCTP().getHyperlinkList().size());
    }

    @Test
    public void tablesAreConvertedWithHeaderAndCellContent() {
        final XWPFDocument document = convert(
            "<figure class=\"table\"><table><thead><tr><th>Header</th><th>Other</th></tr></thead>"
                + "<tbody><tr><td>Cell 1</td><td>Cell <strong>2</strong></td></tr></tbody></table></figure>");

        assertEquals(1, document.getTables().size());
        final XWPFTable table = document.getTables().get(0);
        assertEquals(2, table.getRows().size());
        assertEquals("Header", table.getRow(0).getCell(0).getText());
        assertTrue(table.getRow(0).getCell(0).getParagraphs().get(0).getRuns().get(0).isBold(), "th content is bold");
        assertEquals("Cell 2", table.getRow(1).getCell(1).getText());
    }

    @Test
    public void blockquoteIsIndentedAndItalic() {
        final XWPFDocument document = convert("<blockquote><p>quoted</p></blockquote><p>normal</p>");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(2, paragraphs.size());
        assertTrue(paragraphs.get(0).getIndentationLeft() > 0);
        assertTrue(paragraphs.get(0).getRuns().get(0).isItalic());
        assertEquals(-1, paragraphs.get(1).getIndentationLeft());
        assertTrue(paragraphs.get(1).getRuns().stream().noneMatch(r -> r.isItalic()));
    }

    @Test
    public void indentedParagraphGetsLeftIndent() {
        final XWPFDocument document = convert("<p style=\"margin-left:40px;\">indented</p>");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(1, paragraphs.size());
        assertEquals(600, paragraphs.get(0).getIndentationLeft());
    }

    @Test
    public void listItemsAreNotDroppedWhenWrappedInParagraphs() {
        final XWPFDocument document = convert("<ul><li><p>wrapped</p></li></ul>");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(1, paragraphs.size());
        assertEquals("wrapped", paragraphs.get(0).getText());
        assertNotNull(paragraphs.get(0).getNumID(), "paragraph inside li keeps list numbering");
    }

    @Test
    public void emptyOrNullHtmlAddsNothing() {
        assertEquals(0, contentParagraphs(convert("")).size());
        assertEquals(0, contentParagraphs(convert(null)).size());
        assertEquals(0, contentParagraphs(convert("<p>&nbsp;</p>")).size());
    }

    @Test
    public void plainTextWithoutTagsBecomesOneParagraph() {
        final XWPFDocument document = convert("just plain text");

        final List<XWPFParagraph> paragraphs = contentParagraphs(document);
        assertEquals(1, paragraphs.size());
        assertEquals("just plain text", paragraphs.get(0).getText());
        assertNull(paragraphs.get(0).getStyle());
    }
}
