package dk.digitalidentity.report;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.math.BigInteger;

import static dk.digitalidentity.report.DocxUtil.advanceCursor;
import static dk.digitalidentity.report.DocxUtil.generateNumbering;
import static dk.digitalidentity.report.DocxUtil.orderedAbstractNumXml;
import static dk.digitalidentity.report.DocxUtil.unorderedAbstractNumXml;

public class HtmlToDocVisitor implements NodeVisitor {
    private static final String HEADING3 = "Heading3";
    private static final String HEADING4 = "Heading4";
    private static final String HEADING5 = "Heading5";
    private static final String HEADING6 = "Heading6";

    private final XWPFDocument document;
    private final XmlCursor cursor;
    private String nodeName;
    private boolean isItalic;
    private boolean isBold;
    private boolean isUnderlined;
    private boolean isLink;
    private BigInteger currentNumbering;
    private int fontSize;
    private String fontColor;
    private XWPFRun run;
    private XWPFParagraph paragraph;
    private XWPFTable table;
    private XWPFTableRow tableRow;
    private int tableColumnCounter;
    private int tableRowCounter;
    private int listLevel;

    public HtmlToDocVisitor(final XWPFDocument document, final XmlCursor cursor) {
        this.document = document;
        this.cursor = cursor;
        nodeName = "";
        isItalic = false;
        isBold = false;
        isUnderlined = false;
        fontSize = 11;
        fontColor = "000000";
        currentNumbering = BigInteger.ONE;
        isLink = false;
        listLevel = -1;
        startNewRun();
    }

    @SneakyThrows
    @Override
    public void head(final Node node, final int depth) {
        this.nodeName = node.nodeName();
        if ("#text".equals(nodeName)) {
        } else if ("i".equals(nodeName)) {
            isItalic = true;
        } else if ("b".equals(nodeName) || "strong".equals(nodeName)) {
            isBold = true;
        } else if ("u".equals(nodeName)) {
            isUnderlined = true;
        } else if ("a".equals(nodeName)) {
            final String target = node.attr("href");
            startNewHyperlinkRun(fixUrl(target));
            isLink = true;
        } else if ("font".equals(nodeName)) {
            fontColor = (!"".equals(node.attr("color"))) ? node.attr("color").substring(1) : "000000";
            fontSize = (!"".equals(node.attr("size"))) ? Integer.parseInt(node.attr("size")) : 11;
        } else if ("ul".equals(nodeName)) {
            currentNumbering = generateNumbering(document, unorderedAbstractNumXml(document));
            listLevel++;
        } else if ("ol".equals(nodeName)) {
            currentNumbering = generateNumbering(document, orderedAbstractNumXml(document));
            listLevel++;
        } else if ("li".equals(nodeName)) {
            paragraph.setNumID(currentNumbering);
            paragraph.setNumILvl(BigInteger.valueOf(listLevel));
        } else if ("table".equals(nodeName)) {
            table = paragraph.getBody().insertNewTbl(cursor);
            advanceCursor(cursor);
            tableRowCounter = 0;
            tableColumnCounter = 0;
        } else if ("tr".equals(nodeName)) {
            tableRow = tableRowCounter == 0 ? table.getRow(0) : table.createRow();
            tableColumnCounter = 0;
        } else if ("td".equals(nodeName)) {
            XWPFTableCell cell = table.getRow(tableRowCounter).getCell(tableColumnCounter);
            if (cell == null) {
                cell = tableRow.addNewTableCell();
            }
            paragraph = cell.getParagraphs().get(0);
        } else if ("h1".equals(nodeName)) {
            paragraph.setStyle(HEADING3);
        } else if ("h2".equals(nodeName)) {
            paragraph.setStyle(HEADING4);
        } else if ("h3".equals(nodeName)) {
            paragraph.setStyle(HEADING5);
        } else if ("h4".equals(nodeName)) {
            paragraph.setStyle(HEADING6);
        }
        applyRunSettings();
    }

    @Override
    public void tail(final Node node, final int depth) {
        nodeName = node.nodeName();
        if ("#text".equals(nodeName)) {
            final String text = ((TextNode) node).text();
            if (tableRow != null) {
                final XWPFTableCell cell = tableRow.getCell(tableColumnCounter);
                cell.setText(text);
            } else {
                run.setText(text);
                startNewRun();
            }
        } else if ("a".equals(nodeName)) {
            isLink = false;
        } else if ("br".equals(nodeName)) {
            run.addBreak();
            startNewRun();
        } else if ("i".equals(nodeName)) {
            isItalic = false;
        } else if ("b".equals(nodeName) || "strong".equals(nodeName)) {
            isBold = false;
        } else if ("u".equals(nodeName)) {
            isUnderlined = false;
        } else if ("font".equals(nodeName)) {
            fontColor = "000000";
            fontSize = 11;
        } else if ("table".equals(nodeName)) {
            run.addBreak();
            startNewRun();
            tableRow = null;
            table = null;
        } else if ("td".equals(nodeName)) {
            tableColumnCounter++;
        } else if ("tr".equals(nodeName)) {
            tableRowCounter++;
        } else if ("ol".equals(nodeName)) {
            listLevel--;
        } else if ("ul".equals(nodeName)) {
            listLevel--;
        }
        applyRunSettings();
    }


    private void applyRunSettings() {
        run.setItalic(isItalic);
        run.setBold(isBold);
        if (!isLink) {
            if (isUnderlined) {
                run.setUnderline(UnderlinePatterns.SINGLE);
            } else {
                run.setUnderline(UnderlinePatterns.NONE);
            }
            run.setColor(fontColor);
            run.setFontSize(fontSize);
        }
    }

    private void startNewRun() {
        paragraph = document.insertNewParagraph(cursor);
		paragraph.setSpacingAfter(240);
        run = paragraph.createRun();
        advanceCursor(cursor);
    }
    private void startNewHyperlinkRun(final String url) {
        paragraph = document.insertNewParagraph(cursor);
		paragraph.setSpacingAfter(240);g
        run = paragraph.createHyperlinkRun(url);
        run.setStyle("Hyperlink");
        advanceCursor(cursor);
    }

    private String fixUrl(final String url) {
        if (!StringUtils.startsWith(url, "http")) {
            return "https://" + url;
        }
        return url;
    }
}
