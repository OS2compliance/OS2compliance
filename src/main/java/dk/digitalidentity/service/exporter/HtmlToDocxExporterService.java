package dk.digitalidentity.service.exporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jspecify.annotations.NonNull;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPrBase;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.Enum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HtmlToDocxExporterService {

	public ByteArrayOutputStream convert(@NonNull final String html) throws IOException {
		final Document doc = Jsoup.parseBodyFragment(html);

		try (final XWPFDocument xwpfDoc = new XWPFDocument()) {
			final CssParser cssParser = new CssParser(doc);

			cssParser.applyStyles(xwpfDoc);

			final HtmlParser parser = new HtmlParser(xwpfDoc, cssParser.getClassBackgroundColors());
			parser.parse(doc);

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			xwpfDoc.write(out);
			return out;
		}
	}

	private static class HtmlParser {

		private final XWPFDocument document;
		private final Map<String, String> classBackgroundColors;
		private XWPFParagraph currentParagraph;
		private XWPFTableCell currentCell;

		HtmlParser(final XWPFDocument document, final Map<String, String> classBackgroundColors) {
			this.document = document;
			this.classBackgroundColors = classBackgroundColors;
		}

		private Optional<Integer> parsePx(final String value) {
			if (value.isEmpty()) {
				return Optional.empty();
			}
			final String digits = value.replace("px", "").trim();
			if (!digits.chars().allMatch(Character::isDigit)) {
				return Optional.empty();
			}
			return Optional.of(Integer.parseInt(digits) * 9525);
		}


		private XWPFParagraph createParagraph() {
			return currentCell != null
				? currentCell.addParagraph()
				: document.createParagraph();
		}

		void parse(Document html) {
			parseBody(html.body());
		}

		void parseBody(final Element body) {
			processChildren(body, FormatState.none());
		}

		private void processNode(final Node node, final FormatState format) {
			if (node instanceof final TextNode text) {
				final String content = text.getWholeText();
				if (content.isBlank() && !content.equals(" ")) {
					return;
				}
				if (content.equals(" ") && (node.previousSibling() == null || isBlockOrBreak(node.previousSibling()))) {
					return;
				}
				if (currentParagraph == null) {
					currentParagraph = createParagraph();
			}
				final XWPFRun run = currentParagraph.createRun();
				run.setBold(format.bold());
				run.setItalic(format.italic());
				if (format.underline()) {
					run.setUnderline(UnderlinePatterns.SINGLE);
				}
				run.setText(content);
			}
			else if (node instanceof final Element element) {
				applyParagraphStyle(element);
				processElement(element, format);
			}
		}

		private boolean isBlockOrBreak(final Node node) {
			if (node instanceof final Element el) {
				return switch (HtmlTag.from(el)) {
					case HEADING, P, DIV, TABLE, TR, TD, TH, OL, UL, LI, BR -> true;
					default -> false;
				};
			}
			return false;
		}

		private void processElement(final Element element, final FormatState format) {
			final HtmlTag tag = HtmlTag.from(element);

			switch (tag) {
				case HEADING -> processHeading(element);
				case P, DIV -> processBlock(element, format);
				case TABLE -> processTable(element, format);
				case TR, TD, TH -> processChildren(element, format);
				case B -> processChildren(element, format.withBold());
				case I -> processChildren(element, format.withItalic());
				case U -> processChildren(element, format.withUnderline());
				case IMG -> processImage(element);
				case OL, UL -> processList(element, format, 0, null);
			case BR -> {
				if (currentParagraph != null) {
					if (!currentParagraph.getRuns().isEmpty()) {
						currentParagraph.getRuns().getLast().addBreak();
					} else {
						currentParagraph.createRun().addBreak();
					}
				}
			}
				default -> processChildren(element, format);
			}
		}

		private void processImage(final Element element) {
			final String src = element.attr("src");
			if (src.isEmpty() || !src.startsWith("data:")) {
				return;
			}

			final int headerEnd = src.indexOf(";");
			if (headerEnd < 0) {
				return;
			}
			final String mime = src.substring(5, headerEnd);
			final String base64 = src.substring(headerEnd + 8);
			final byte[] data = java.util.Base64.getDecoder().decode(base64);

			final String[] mimeParts = mime.split("/");
			if (mimeParts.length < 2) {
				return;
			}
			final String ext = mimeParts[1];
			final int type = switch (ext) {
				case "png" -> XWPFDocument.PICTURE_TYPE_PNG;
				case "jpeg", "jpg" -> XWPFDocument.PICTURE_TYPE_JPEG;
				case "gif" -> XWPFDocument.PICTURE_TYPE_GIF;
					default -> XWPFDocument.PICTURE_TYPE_PNG;
			};

			final int cx = parsePx(element.attr("width")).orElse(600000);
			final int cy = parsePx(element.attr("height")).orElse(600000);

			try {
				if (currentParagraph == null) {
					currentParagraph = createParagraph();
				}

				final XWPFRun run = currentParagraph.createRun();

				run.addPicture(
						new java.io.ByteArrayInputStream(data),
						type,
						java.util.UUID.randomUUID().toString(),
						cx,
						cy
						);
			} catch (final Exception e) {
				log.warn("Failed to add image: {}", e.getMessage());
			}
		}

		private void processTable(final Element element, final FormatState format) {
			final Elements htmlRows = element.select("tr");
			if (htmlRows.isEmpty()) {
				return;
			}

			final int cols = htmlRows.stream()
				.mapToInt(r -> r.select("td, th").size())
				.max()
				.orElse(0);

			if (cols == 0) {
				return;
			}

			final XWPFTable table = document.createTable(htmlRows.size(), cols);

			final CTTblGrid tblGrid = table.getCTTbl().addNewTblGrid();
			for (int i = 0; i < cols; i++) {
				tblGrid.addNewGridCol().setW(BigInteger.valueOf(12240 / cols));
			}

			applyTableStyle(element, table);
			applyColumnWidths(element, table);
			for (int y = 0; y < htmlRows.size(); y++) {
				final Elements htmlCells = htmlRows.get(y).select("td, th");
				final XWPFTableRow row = table.getRow(y);

				final int cellCount = Math.min(htmlCells.size(), cols);
				for (int x = 0; x < cellCount; x++) {
					currentCell = row.getCell(x);
					currentParagraph = currentCell.getParagraphs().getFirst();

					final FormatState cellFormat = htmlCells.get(x).tagName().equalsIgnoreCase("th")
						? format.withBold()
						: format;
					applyCellShading(htmlCells.get(x), currentCell);
					processChildren(htmlCells.get(x), cellFormat);
					currentCell.getCTTc().addNewTcPr().addNewVAlign().setVal(STVerticalJc.CENTER);
					currentCell = null;
				}
				currentParagraph = null;
			}
		}

		private void applyCellShading(final Element htmlCell, final XWPFTableCell cell) {
			if (classBackgroundColors == null || classBackgroundColors.isEmpty()) {
				return;
			}

			final String ownColor = findBackgroundColor(htmlCell);
			if (ownColor != null) {
				cell.getCTTc().addNewTcPr().addNewShd().setFill(ownColor);
				return;
			}

			final java.util.Set<String> colors = new java.util.LinkedHashSet<>();
			for (final Element descendant : htmlCell.select("[class]")) {
				final String color = findBackgroundColor(descendant);
				if (color != null) {
					colors.add(normalizeColor(color));
				}
			}

			if (colors.size() == 1) {
				cell.getCTTc().addNewTcPr().addNewShd().setFill(colors.iterator().next());
			}
		}

		private String normalizeColor(final String hex) {
			if (hex != null && hex.length() == 3) {
				return "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
			}
			return hex;
		}

		private String findBackgroundColor(final Element element) {
			for (final String token : element.attr("class").trim().split("\\s+")) {
				if (token.isEmpty()) {
					continue;
				}
				final String color = classBackgroundColors.get(token);
				if (color != null) {
					return color;
				}
			}
			return null;
		}

		private void processHeading(final Element element) {
			final int level = Integer.parseInt(element.tagName().substring(1));
			if (currentCell == null || !currentParagraph.getRuns().isEmpty()) {
				currentParagraph = createParagraph();
			}
			applyParagraphStyle(element);
			final XWPFRun run = currentParagraph.createRun();
			run.setBold(true);
			run.setFontSize(switch (level) {
				case 1 -> 24;
				case 2 -> 18;
				case 3 -> 14;
				case 4 -> 12;
				case 5 -> 11;
				case 6 -> 10;
				default -> 12;
			});
			run.setText(element.text());
		}

		private boolean isWhitespaceOnly(final XWPFParagraph paragraph) {
			return !paragraph.getRuns().isEmpty()
				&& paragraph.getRuns().stream().allMatch(r -> {
					final String text = r.text();
					return text != null && text.isBlank();
				});
		}

		private void processBlock(final Element element, final FormatState format) {
			if (currentCell == null || !currentParagraph.getRuns().isEmpty()) {
				if (currentCell != null && isWhitespaceOnly(currentParagraph)) {
					for (int i = currentParagraph.getRuns().size() - 1; i >= 0; i--) {
						currentParagraph.removeRun(i);
					}
				} else {
					currentParagraph = createParagraph();
				}
			}
			applyParagraphStyle(element);
			processChildren(element, format);
		}

		private void processList(final Element element, final FormatState format,
				final int depth, final BigInteger parentNumId) {
			final XWPFNumbering numbering = document.getNumbering() != null
				? document.getNumbering() : document.createNumbering();

			final BigInteger numId = parentNumId != null
				? parentNumId
				: createNumbering(element, depth, numbering);

			for (final Node child : element.childNodes()) {
				if (child instanceof Element e && HtmlTag.from(e) == HtmlTag.LI) {
					processListItem(e, format, numId, depth);
				}
			}
		}

		private BigInteger createNumbering(final Element element, final int depth,
				final XWPFNumbering numbering) {
			final boolean ordered = HtmlTag.from(element) == HtmlTag.OL;
			final CTAbstractNum cTAbstractNum = CTAbstractNum.Factory.newInstance();
			cTAbstractNum.setAbstractNumId(
					BigInteger.valueOf(numbering.getAbstractNums().size()));

			for (int i = 0; i <= depth; i++) {
				addNumberingLevel(cTAbstractNum, i, ordered);
			}

			final XWPFAbstractNum abstractNum = new XWPFAbstractNum(cTAbstractNum, numbering);
			return numbering.addNum(numbering.addAbstractNum(abstractNum));
		}

		private void addNumberingLevel(final CTAbstractNum cTAbstractNum,
				final int index, final boolean ordered) {
			final CTLvl lvl = cTAbstractNum.addNewLvl();
			lvl.setIlvl(BigInteger.valueOf(index));
			lvl.addNewNumFmt().setVal(
					ordered ? STNumberFormat.DECIMAL : STNumberFormat.BULLET);
			lvl.addNewLvlText().setVal(
					ordered ? "%" + (index + 1) + "." : "\u2022");
			lvl.addNewStart().setVal(BigInteger.ONE);

			if (!ordered) {
				final CTFonts fonts = lvl.addNewRPr().addNewRFonts();
				fonts.setAscii("Symbol");
				fonts.setHAnsi("Symbol");
			}

			final CTInd ind = lvl.addNewPPr().addNewInd();
			ind.setLeft(BigInteger.valueOf(720L * (index + 1)));
			ind.setHanging(BigInteger.valueOf(180));
		}

		private void processListItem(final Element element, final FormatState format,
				final BigInteger numId, final int depth) {
			currentParagraph = createParagraph();

			final CTNumPr numPr = currentParagraph.getCTP().addNewPPr().addNewNumPr();
			numPr.addNewNumId().setVal(numId);
			numPr.addNewIlvl().setVal(BigInteger.valueOf(depth));

			for (final Node child : element.childNodes()) {
				processNode(child, format);
			}
		}

		private void processChildren(final Element element, final FormatState format) {
			for (final Node child : element.childNodes()) {
				processNode(child, format);
			}
		}

		private void applyColumnWidths(final Element table, final XWPFTable xwpfTable) {
			final Elements cols = table.select("colgroup col");
			if (cols.isEmpty()) {
				return;
			}

			final CTTblGrid tblGrid = xwpfTable.getCTTbl().getTblGrid();
			if (tblGrid == null) {
				return;
			}

			final int gridCols = tblGrid.sizeOfGridColArray();

			final int pageWidth = 12240;

			for (int i = 0; i < cols.size() && i < gridCols; i++) {
				final String width = cols.get(i).attr("width");
				if (!width.isEmpty() && width.endsWith("%")) {
					final String digits = width.replace("%", "").trim();

					if (!digits.chars().allMatch(Character::isDigit)) {
						continue;
					}

					final int pct = Integer.parseInt(digits);
					final int widthTwips = pageWidth * pct / 100;
					tblGrid.getGridColArray(i).setW(BigInteger.valueOf(widthTwips));
				}
			}
		}

		private void applyTableStyle(final Element element, final XWPFTable table) {
			final CTTblPr tblPr = table.getCTTbl().addNewTblPr();
			tblPr.addNewTblStyle().setVal("TableGrid");

			final CTTblWidth tblW = tblPr.addNewTblW();
			tblW.setW(BigInteger.valueOf(5000));
			tblW.setType(STTblWidth.Enum.forString("pct"));

			final CTTblLayoutType tblLayout = tblPr.addNewTblLayout();
			tblLayout.setType(STTblLayoutType.Enum.forString("fixed"));
		}

		private void applyParagraphStyle(final Element element) {
			final String style = element.attr("style");
			if (style.isEmpty() || currentParagraph == null) {
				return;
			}

			final Map<String, String> styles = parseStyle(style);

			final String textAlign = styles.get("text-align");
			if (textAlign != null) {
				currentParagraph.setAlignment(switch (textAlign) {
					case "left" -> ParagraphAlignment.LEFT;
					case "center" -> ParagraphAlignment.CENTER;
					case "right" -> ParagraphAlignment.RIGHT;
					case "justify" -> ParagraphAlignment.BOTH;
						default -> null;
				});
			}
		}

		private Map<String, String> parseStyle(final String css) {
			final Map<String, String> map = new java.util.LinkedHashMap<>();
			for (final String part : css.split(";")) {
				final String[] kv = part.split(":", 2);
				if (kv.length == 2) {
					map.put(kv[0].trim().toLowerCase(), kv[1].trim().toLowerCase());
				}
			}
			return map;
		}


		private enum HtmlTag {
			HEADING, P, DIV, TABLE, TR, TD, TH, B, I, U, IMG, OL, UL, LI, BR, UNKNOWN;

			static HtmlTag from(final Element element) {
				final String tag = element.tagName().toLowerCase();
				return switch (tag) {
					case "h1", "h2", "h3", "h4", "h5", "h6" -> HEADING;
					case "p" -> P;
					case "div" -> DIV;
					case "table" -> TABLE;
					case "tr" -> TR;
					case "td" -> TD;
					case "th" -> TH;
					case "b", "strong" -> B;
					case "i", "em" -> I;
					case "u" -> U;
					case "img" -> IMG;
					case "ol" -> OL;
					case "ul" -> UL;
					case "li" -> LI;
					case "br" -> BR;
					default -> UNKNOWN;
				};
			}
		}

		private record FormatState(boolean bold, boolean italic, boolean underline) {

			static FormatState none() {
				return new FormatState(false, false, false);
			}

			FormatState withBold() {
				return new FormatState(true, italic, underline);
			}

			FormatState withItalic() {
				return new FormatState(bold, true, underline);
			}

			FormatState withUnderline() {
				return new FormatState(bold, italic, true);
			}
		}
	}

	private static class CssParser {

		private static final Pattern RGB_COLOR = Pattern.compile("(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)");
		private final String css;
		private final List<CssRule> rules;

		CssParser(final Document html) {
			this.css = html.select("style").html();
			this.rules = parseCssRules();
		}

		private List<CssRule> parseCssRules() {
			final List<CssRule> rules = new java.util.ArrayList<>();
			for (final String part : css.split("}")) {
				final String[] ruleParts = part.split("\\{", 2);
				if (ruleParts.length < 2) {
					continue;
				}
				rules.add(new CssRule(ruleParts[0].trim().toLowerCase(), ruleParts[1].trim().toLowerCase()));
			}
			return rules;
		}

		private record CssRule(String selectors, String declarations) {
		}

		void applyStyles(final XWPFDocument doc) {
			applyTableStyles(doc);
		}

		private void applyTableStyles(final XWPFDocument doc) {
			final Map<String, String> allProps = new java.util.LinkedHashMap<>();
			for (final String selector : List.of("table", "tr", "th", "td")) {
				allProps.putAll(getAllProperties(selector));
			}

			if (allProps.isEmpty()) {
				return;
			}

			final XWPFStyles styles = doc.createStyles();
			final CTStyle ctStyle = CTStyle.Factory.newInstance();
			ctStyle.setType(STStyleType.TABLE);
			ctStyle.setStyleId("TableGrid");
			ctStyle.addNewName().setVal("Table Grid");
			ctStyle.addNewQFormat();

			final CTTblPrBase tblPr = ctStyle.addNewTblPr();

			for (final Entry<String, String> entry : allProps.entrySet()) {
				final String prop = entry.getKey();
				final String value = entry.getValue();

				if (prop.equals("border") || prop.startsWith("border-")) {
					applyBorder(tblPr, value);
				}
				else if (prop.equals("width")) {
					applyWidth(tblPr, value);
				}
			}

			styles.addStyle(new XWPFStyle(ctStyle));
		}

		private void applyBorder(final CTTblPrBase tblPr, final String border) {
			final String[] parts = border.split("\\s+");
			if (parts.length < 3) {
				return;
			}

			int borderWidth = 4;
			String borderColor = "000000";

			for (final String part : parts) {
				if (part.endsWith("px")) {
					final String borderDigits = part.replace("px", "").trim();
					if (borderDigits.chars().allMatch(Character::isDigit)) {
						borderWidth = Integer.parseInt(borderDigits);
					}
				}
				else {
					final String hex = toHexColor(part);
					if (hex != null) {
						borderColor = hex;
					}
				}
			}

			final CTTblBorders borders = tblPr.addNewTblBorders();
			final Enum borderType = STBorder.Enum.forString("single");

			setBorderSide(borders.addNewTop(), borderType, borderWidth, borderColor);
			setBorderSide(borders.addNewBottom(), borderType, borderWidth, borderColor);
			setBorderSide(borders.addNewLeft(), borderType, borderWidth, borderColor);
			setBorderSide(borders.addNewRight(), borderType, borderWidth, borderColor);
			setBorderSide(borders.addNewInsideH(), borderType, borderWidth, borderColor);
			setBorderSide(borders.addNewInsideV(), borderType, borderWidth, borderColor);
		}

		private void setBorderSide(final CTBorder border,
				final STBorder.Enum type,
				final int size,
				final String color) {
			border.setVal(type);
			border.setSz(BigInteger.valueOf(size));
			border.setColor(color);
		}

		private void applyWidth(final CTTblPrBase tblPr, final String width) {
			if (width.endsWith("%")) {
				final String digits = width.replace("%", "").trim();
				if (!digits.chars().allMatch(Character::isDigit)) {
					return; 
				}

				final int pct = Integer.parseInt(digits);
				final CTTblWidth tblW = tblPr.addNewTblW();
				tblW.setW(BigInteger.valueOf(pct * 50));
				tblW.setType(STTblWidth.Enum.forString("pct"));
			}
		}

		Map<String, String> getAllProperties(final String selector) {
			final Map<String, String> result = new java.util.LinkedHashMap<>();

			for (final CssRule rule : rules) {
				if (!java.util.Arrays.asList(rule.selectors().split("\\s*,\\s*")).contains(selector)) {
					continue;
				}

				for (final String decl : rule.declarations().split(";")) {
					final String[] kv = decl.split(":", 2);
					if (kv.length == 2) {
						result.put(kv[0].trim(), kv[1].trim());
					}
				}
			}

			return result;
		}

		Map<String, String> getClassBackgroundColors() {
			final Map<String, String> result = new java.util.LinkedHashMap<>();

			for (final CssRule rule : rules) {
				final String[] selectors = rule.selectors().split("\\s*,\\s*");
				final String[] declarations = rule.declarations().split(";");

				for (final String selector : selectors) {
					if (!selector.matches("^\\.([a-z0-9_-]+)$")) {
						continue;
					}

					for (final String declaration : declarations) {
						final String[] kv = declaration.split(":", 2);
						if (kv.length != 2 || !kv[0].trim().equals("background-color")) {
							continue;
						}

						final String hex = toHexColor(kv[1].trim().replaceAll("!\\s*important\\s*", ""));
						if (hex != null) {
							result.put(selector.substring(1), hex);
							break;
						}
					}
				}
			}

			return result;
		}

		private String toHexColor(final String value) {
			if (value.startsWith("#")) {
				return value.substring(1).toUpperCase();
			}
			if (value.startsWith("rgb")) {
				final Matcher matcher = RGB_COLOR.matcher(value);
				if (matcher.find()) {
					return String.format("%02X%02X%02X",
							Integer.parseInt(matcher.group(1)),
							Integer.parseInt(matcher.group(2)),
							Integer.parseInt(matcher.group(3)));
				}
			}
			return null;
		}
	}
}