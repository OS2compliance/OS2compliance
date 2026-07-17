package dk.digitalidentity.report;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link DocxUtil#normalizeHeadingStyles}
 */
public class DocxUtilHeadingStylesTest {

    private static final int[] EXPECTED_SIZES = {40, 36, 32, 28, 24, 24};
    private static final boolean[] EXPECTED_BOLD = {false, false, true, true, true, true};
    private static final boolean[] EXPECTED_ITALIC = {false, false, false, false, false, true};

    @ParameterizedTest
    @ValueSource(strings = {
        "reports/default/default.docx",
        "reports/ISO27001/ISO27001.docx",
        "reports/ISO27002/ISO27002.docx",
        "reports/article30/main.docx",
        "reports/risk/main.docx"
    })
    public void allTemplatesGetFullHeadingLadder(final String template) throws IOException {
        final XWPFDocument document = loadTemplate(template);

        DocxUtil.normalizeHeadingStyles(document);

        assertHeadingLadder(document, template);
    }

    @Test
    public void blankDocumentGetsAllHeadingStylesWithOutlineLevels() {
        final XWPFDocument document = new XWPFDocument();

        DocxUtil.normalizeHeadingStyles(document);

        assertHeadingLadder(document, "blank document");
        for (int level = 1; level <= 6; level++) {
            final XWPFStyle style = document.getStyles().getStyle("Heading" + level);
            assertEquals(BigInteger.valueOf(level - 1), style.getCTStyle().getPPr().getOutlineLvl().getVal(),
                "created Heading" + level + " must keep TOC/navigation nesting via outlineLvl");
            assertEquals("Normal", style.getBasisStyleID());
        }
    }

    @Test
    public void existingStylesKeepTheirTemplateColor() throws IOException {
        final XWPFDocument document = loadTemplate("reports/default/default.docx");

        DocxUtil.normalizeHeadingStyles(document);

        // default.docx defines Heading3 with color 1F3763; normalization must not touch it
        final CTRPr rpr = document.getStyles().getStyle("Heading3").getCTStyle().getRPr();
        assertEquals(1, rpr.sizeOfColorArray());
        assertEquals("1F3763", rpr.getColorArray(0).xgetVal().getStringValue().toUpperCase());
    }

    @Test
    public void normalizingTwiceIsIdempotent() throws IOException {
        final XWPFDocument document = loadTemplate("reports/default/default.docx");

        DocxUtil.normalizeHeadingStyles(document);
        DocxUtil.normalizeHeadingStyles(document);

        assertHeadingLadder(document, "double normalization");
        final CTRPr rpr = document.getStyles().getStyle("Heading3").getCTStyle().getRPr();
        assertEquals(1, rpr.sizeOfSzArray());
        assertEquals(1, rpr.sizeOfBArray());
    }

    private XWPFDocument loadTemplate(final String template) throws IOException {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream(template);
        return new XWPFDocument(Objects.requireNonNull(stream, "template not found: " + template));
    }

    private void assertHeadingLadder(final XWPFDocument document, final String context) {
        final XWPFStyles styles = document.getStyles();
        for (int level = 1; level <= 6; level++) {
            final String styleId = "Heading" + level;
            final XWPFStyle style = styles.getStyle(styleId);
            assertNotNull(style, context + ": " + styleId + " must exist");
            final CTRPr rpr = style.getCTStyle().getRPr();
            assertNotNull(rpr, context + ": " + styleId + " must have run properties");
            assertEquals(BigInteger.valueOf(EXPECTED_SIZES[level - 1]), rpr.getSzArray(0).getVal(),
                context + ": " + styleId + " font size");
            assertEquals(EXPECTED_BOLD[level - 1], rpr.sizeOfBArray() > 0,
                context + ": " + styleId + " bold");
            assertEquals(EXPECTED_ITALIC[level - 1], rpr.sizeOfIArray() > 0,
                context + ": " + styleId + " italic");
            assertTrue(rpr.getSzArray(0).getVal().toString().equals(rpr.getSzCsArray(0).getVal().toString()),
                context + ": " + styleId + " szCs must match sz");
        }
    }
}
