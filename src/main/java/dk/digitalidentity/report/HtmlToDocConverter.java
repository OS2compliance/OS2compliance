package dk.digitalidentity.report;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dk.digitalidentity.report.DocxUtil.advanceCursor;
import static dk.digitalidentity.report.DocxUtil.generateNumbering;
import static dk.digitalidentity.report.DocxUtil.getCell;
import static dk.digitalidentity.report.DocxUtil.orderedAbstractNumXml;
import static dk.digitalidentity.report.DocxUtil.unorderedAbstractNumXml;

/**
 * Converts CKEditor-produced HTML to Word content at the given cursor position.
 * Block elements (p, h1-h6, li, table cells, blockquote) each map to a single paragraph,
 * inline elements (b/strong, i/em, u, s, a, br, font, span) map to runs within that paragraph.
 * Empty paragraphs and stray line breaks are removed before conversion.
 */
public class HtmlToDocConverter {
    private static final Pattern MARGIN_LEFT = Pattern.compile("margin-left:\\s*(\\d+(?:\\.\\d+)?)px");
    // CKEditor indents in px; Word indents in twips (1/20 pt). 1px = 15 twips at 96dpi.
    private static final int TWIPS_PER_PX = 15;
    private static final int QUOTE_INDENT = 720;
    private static final int PARAGRAPH_SPACING = 120;
    private static final int LIST_PARAGRAPH_SPACING = 60;
    // The numbering definitions in DocxUtil define three levels
    private static final int MAX_LIST_LEVEL = 2;
    // Offset by two so exported content headings sort under the report's own section headings.
    // DocxUtil.normalizeHeadingStyles guarantees these styles exist and are visually distinct.
    private static final Map<String, String> HEADING_STYLES = Map.of(
        "h1", "Heading3",
        "h2", "Heading4",
        "h3", "Heading5",
        "h4", "Heading6",
        "h5", "Heading6",
        "h6", "Heading6"
    );
    private static final Set<String> BLOCK_TAGS = Set.of("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "table", "figure", "blockquote");

    private final XWPFDocument document;
    private final XmlCursor cursor;

    // Inline formatting state, saved and restored while descending into inline elements
    private boolean bold;
    private boolean italic;
    private boolean underlined;
    private boolean strikethrough;
    private String fontColor;
    private Integer fontSize;

    public HtmlToDocConverter(final XWPFDocument document, final XmlCursor cursor) {
        this.document = document;
        this.cursor = cursor;
    }

    public void convert(final String html) {
        final Element body = Jsoup.parseBodyFragment(html).body();
        removeClutter(body);
        renderBlocks(body, BlockContext.root(), this::newDocumentParagraph);
    }

    private record BlockContext(BigInteger numId, String listTag, int listLevel, int indent, boolean quote, boolean inTable) {
        static BlockContext root() {
            return new BlockContext(null, null, -1, 0, false, false);
        }

        BlockContext indented(final int extraIndent) {
            return new BlockContext(numId, listTag, listLevel, indent + extraIndent, quote, inTable);
        }
    }

    private void removeClutter(final Element body) {
        // line breaks sitting between block elements
        for (final Element br : body.select("br")) {
            final Element parent = br.parent();
            if (parent != null && ("body".equals(parent.normalName()) || "blockquote".equals(parent.normalName()))) {
                br.remove();
            }
        }
        // line breaks at the edges of text blocks
        for (final Element block : body.select("p, li, h1, h2, h3, h4, h5, h6, div, td, th")) {
            trimEdges(block);
        }
        // blocks without any visible content at all (typically <p>&nbsp;</p> or <p><br></p>)
        for (final Element block : body.select("p, h1, h2, h3, h4, h5, h6, div")) {
            final boolean blankText = block.wholeText().replace('\u00a0', ' ').trim().isEmpty();
            if (blankText && block.select("table, ul, ol, img").isEmpty()) {
                block.remove();
            }
        }
    }

    private static void trimEdges(final Element block) {
        while (block.childNodeSize() > 0 && isRemovableEdge(block.childNode(0))) {
            block.childNode(0).remove();
        }
        while (block.childNodeSize() > 0 && isRemovableEdge(block.childNode(block.childNodeSize() - 1))) {
            block.childNode(block.childNodeSize() - 1).remove();
        }
    }

    private static boolean isRemovableEdge(final Node node) {
        if (node instanceof TextNode text) {
            return text.isBlank();
        }
        return node instanceof Element el && "br".equals(el.normalName());
    }

    private void renderBlocks(final Element parent, final BlockContext ctx, final Supplier<XWPFParagraph> sink) {
        // consecutive inline siblings at block level share one implicit paragraph
        XWPFParagraph implicitParagraph = null;
        for (final Node node : parent.childNodes()) {
            if (node instanceof Element el && BLOCK_TAGS.contains(el.normalName())) {
                implicitParagraph = null;
                renderBlockElement(el, ctx, sink);
            } else {
                if (node instanceof TextNode text && text.isBlank()) {
                    continue;
                }
                if (implicitParagraph == null) {
                    implicitParagraph = startParagraph(ctx, sink);
                }
                renderInlineNode(node, implicitParagraph);
            }
        }
    }

    private void renderBlockElement(final Element el, final BlockContext ctx, final Supplier<XWPFParagraph> sink) {
        switch (el.normalName()) {
            case "p", "div" -> {
                final XWPFParagraph paragraph = startParagraph(ctx.indented(parseIndent(el)), sink);
                renderInlineChildren(el, paragraph);
            }
            case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                final XWPFParagraph paragraph = startParagraph(ctx.indented(parseIndent(el)), sink);
                paragraph.setStyle(HEADING_STYLES.get(el.normalName()));
                renderInlineChildren(el, paragraph);
            }
            case "ul", "ol" -> renderList(el, ctx, sink);
            case "blockquote" -> {
                final boolean prevItalic = italic;
                italic = true;
                renderBlocks(el, new BlockContext(ctx.numId(), ctx.listTag(), ctx.listLevel(), ctx.indent() + QUOTE_INDENT, true, ctx.inTable()), sink);
                italic = prevItalic;
            }
            case "table" -> {
                if (ctx.inTable()) {
                    // nested tables cannot be inserted at the document cursor; flatten cell contents
                    for (final Element cellElement : el.select("td, th")) {
                        renderBlocks(cellElement, ctx, sink);
                    }
                } else {
                    renderTable(el);
                }
            }
            // CKEditor wraps tables in <figure class="table">
            default -> renderBlocks(el, ctx, sink);
        }
    }

    private XWPFParagraph startParagraph(final BlockContext ctx, final Supplier<XWPFParagraph> sink) {
        final XWPFParagraph paragraph = sink.get();
        if (ctx.numId() != null) {
            paragraph.setNumID(ctx.numId());
            paragraph.setNumILvl(BigInteger.valueOf(ctx.listLevel()));
            paragraph.setSpacingAfter(LIST_PARAGRAPH_SPACING);
        } else {
            paragraph.setSpacingAfter(PARAGRAPH_SPACING);
            if (ctx.indent() > 0) {
                paragraph.setIndentationLeft(ctx.indent());
            }
        }
        if (ctx.quote()) {
            addQuoteBorder(paragraph);
        }
        return paragraph;
    }

    private XWPFParagraph newDocumentParagraph() {
        final XWPFParagraph paragraph = document.insertNewParagraph(cursor);
        advanceCursor(cursor);
        return paragraph;
    }

    private void renderList(final Element listElement, final BlockContext ctx, final Supplier<XWPFParagraph> sink) {
        final String tag = listElement.normalName();
        BigInteger numId = ctx.numId();
        if (numId == null || !tag.equals(ctx.listTag())) {
            numId = createNumbering(tag);
        }
        final int level = Math.min(ctx.listLevel() + 1, MAX_LIST_LEVEL);
        final BlockContext listCtx = new BlockContext(numId, tag, level, ctx.indent(), ctx.quote(), ctx.inTable());
        for (final Element li : listElement.children()) {
            if ("li".equals(li.normalName())) {
                renderListItem(li, listCtx, sink);
            }
        }
    }

    private void renderListItem(final Element li, final BlockContext ctx, final Supplier<XWPFParagraph> sink) {
        XWPFParagraph paragraph = null;
        for (final Node node : li.childNodes()) {
            if (node instanceof Element el && ("ul".equals(el.normalName()) || "ol".equals(el.normalName()))) {
                renderList(el, ctx, sink);
                paragraph = null;
            } else if (node instanceof Element el && BLOCK_TAGS.contains(el.normalName())) {
                renderBlockElement(el, ctx, sink);
                paragraph = null;
            } else {
                if (node instanceof TextNode text && text.isBlank()) {
                    continue;
                }
                if (paragraph == null) {
                    paragraph = startParagraph(ctx, sink);
                }
                renderInlineNode(node, paragraph);
            }
        }
    }

    private BigInteger createNumbering(final String listTag) {
        try {
            return generateNumbering(document, "ol".equals(listTag) ? orderedAbstractNumXml(document) : unorderedAbstractNumXml(document));
        } catch (final XmlException e) {
            throw new RuntimeException(e);
        }
    }

    private void renderTable(final Element tableElement) {
        final XWPFTable table = document.insertNewTbl(cursor);
        advanceCursor(cursor);
        setFullWidth(table);
        final List<Element> rows = tableElement.select("tr").stream()
            .filter(tr -> enclosingTable(tr) == tableElement)
            .toList();
        int rowIdx = 0;
        for (final Element tr : rows) {
            final XWPFTableRow row = rowIdx == 0 ? table.getRow(0) : table.createRow();
            int colIdx = 0;
            for (final Element cellElement : tr.children()) {
                final String cellTag = cellElement.normalName();
                if (!"td".equals(cellTag) && !"th".equals(cellTag)) {
                    continue;
                }
                renderCell(cellElement, getCell(row, colIdx), "th".equals(cellTag));
                colIdx++;
            }
            rowIdx++;
        }
    }

    private static Element enclosingTable(final Element element) {
        Element parent = element.parent();
        while (parent != null && !"table".equals(parent.normalName())) {
            parent = parent.parent();
        }
        return parent;
    }

    private void renderCell(final Element cellElement, final XWPFTableCell cell, final boolean header) {
        final boolean prevBold = bold;
        bold = bold || header;
        // reuse the cell's default paragraph for the first block so cells do not start with an empty line
        final XWPFParagraph[] defaultParagraph = { cell.getParagraphs().get(0) };
        final Supplier<XWPFParagraph> cellSink = () -> {
            if (defaultParagraph[0] != null) {
                final XWPFParagraph first = defaultParagraph[0];
                defaultParagraph[0] = null;
                return first;
            }
            return cell.addParagraph();
        };
        renderBlocks(cellElement, new BlockContext(null, null, -1, 0, false, true), cellSink);
        bold = prevBold;
    }

    private static void setFullWidth(final XWPFTable table) {
        final CTTblPr tblPr = table.getCTTbl().getTblPr() != null ? table.getCTTbl().getTblPr() : table.getCTTbl().addNewTblPr();
        final CTTblWidth width = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        width.setType(STTblWidth.PCT);
        // table width is measured in fiftieths of a percent
        width.setW(BigInteger.valueOf(5000));
    }

    private void renderInlineChildren(final Element parent, final XWPFParagraph paragraph) {
        for (final Node node : parent.childNodes()) {
            renderInlineNode(node, paragraph);
        }
    }

    private void renderInlineNode(final Node node, final XWPFParagraph paragraph) {
        if (node instanceof TextNode text) {
            addTextRun(text.text(), paragraph);
            return;
        }
        if (!(node instanceof Element el)) {
            return;
        }
        switch (el.normalName()) {
            case "br" -> paragraph.createRun().addBreak();
            case "b", "strong" -> {
                final boolean prev = bold;
                bold = true;
                renderInlineChildren(el, paragraph);
                bold = prev;
            }
            case "i", "em" -> {
                final boolean prev = italic;
                italic = true;
                renderInlineChildren(el, paragraph);
                italic = prev;
            }
            case "u" -> {
                final boolean prev = underlined;
                underlined = true;
                renderInlineChildren(el, paragraph);
                underlined = prev;
            }
            case "s", "strike", "del" -> {
                final boolean prev = strikethrough;
                strikethrough = true;
                renderInlineChildren(el, paragraph);
                strikethrough = prev;
            }
            case "a" -> addHyperlinkRun(el, paragraph);
            case "font" -> {
                final String prevColor = fontColor;
                final Integer prevSize = fontSize;
                if (!el.attr("color").isEmpty()) {
                    fontColor = el.attr("color").replace("#", "");
                }
                if (!el.attr("size").isEmpty()) {
                    fontSize = parseIntOrNull(el.attr("size"));
                }
                renderInlineChildren(el, paragraph);
                fontColor = prevColor;
                fontSize = prevSize;
            }
            // span and unknown inline elements are formatting-transparent
            default -> renderInlineChildren(el, paragraph);
        }
    }

    private void addTextRun(final String text, final XWPFParagraph paragraph) {
        final XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setItalic(italic);
        run.setStrikeThrough(strikethrough);
        if (underlined) {
            run.setUnderline(UnderlinePatterns.SINGLE);
        }
        if (fontColor != null) {
            run.setColor(fontColor);
        }
        if (fontSize != null) {
            run.setFontSize(fontSize);
        }
    }

    private void addHyperlinkRun(final Element link, final XWPFParagraph paragraph) {
        final XWPFHyperlinkRun run = paragraph.createHyperlinkRun(fixUrl(link.attr("href")));
        run.setText(link.text());
        run.setStyle("Hyperlink");
        run.setBold(bold);
        run.setItalic(italic);
    }

    private static String fixUrl(final String url) {
        if (!StringUtils.startsWith(url, "http")) {
            return "https://" + url;
        }
        return url;
    }

    private static int parseIndent(final Element el) {
        final Matcher matcher = MARGIN_LEFT.matcher(el.attr("style"));
        if (matcher.find()) {
            return (int) (Double.parseDouble(matcher.group(1)) * TWIPS_PER_PX);
        }
        return 0;
    }

    private static Integer parseIntOrNull(final String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private static void addQuoteBorder(final XWPFParagraph paragraph) {
        final CTP ctp = paragraph.getCTP();
        final CTPPr ppr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        final CTPBdr pbdr = ppr.isSetPBdr() ? ppr.getPBdr() : ppr.addNewPBdr();
        final CTBorder left = pbdr.isSetLeft() ? pbdr.getLeft() : pbdr.addNewLeft();
        left.setVal(STBorder.SINGLE);
        left.setSz(BigInteger.valueOf(18));
        left.setColor("BFBFBF");
        left.setSpace(BigInteger.valueOf(4));
    }
}
