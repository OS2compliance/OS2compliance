package dk.digitalidentity.service.exporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPrBase;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HtmlToDocxExporterService {

	public ByteArrayOutputStream convert(@NonNull final String html) throws Exception {
		final Document doc = Jsoup.parseBodyFragment(html);

        try (final XWPFDocument xwpfDoc = new XWPFDocument()) {
			final CssParser cssParser = new CssParser(doc);

			cssParser.applyStyles(xwpfDoc);

            final HtmlParser parser = new HtmlParser(xwpfDoc);
            parser.parse(doc);

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            xwpfDoc.write(out);
            return out;
        } catch (final IOException e) {
            throw new Exception(e);
        }
	}

    private static class HtmlParser {

        private final XWPFDocument document;
		private XWPFParagraph currentParagraph;
		private XWPFTableCell currentCell;

        HtmlParser(final XWPFDocument document) {
            this.document = document;
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
				if (!content.isBlank()) {
					final XWPFRun run = currentParagraph.createRun();
					run.setBold(format.bold());
					run.setItalic(format.italic());
					if (format.underline()) {
						run.setUnderline(UnderlinePatterns.SINGLE);
					}
					run.setText(content);
				}
			}
			else if (node instanceof final Element element) {
				applyParagraphStyle(element);
				processElement(element, format);
			}
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
                default -> processChildren(element, format);
            }
        }

		private void processImage(final Element element) {
			final String src = element.attr("src");
			if (src.isEmpty() || !src.startsWith("data:")) {
				return;
			}

			final int headerEnd = src.indexOf(";");
			final String mime = src.substring(5, headerEnd);
			final String base64 = src.substring(headerEnd + 8);
			final byte[] data = java.util.Base64.getDecoder().decode(base64);

			final String ext = mime.split("/")[1];
			final int type = switch (ext) {
				case "png" -> XWPFDocument.PICTURE_TYPE_PNG;
				case "jpeg", "jpg" -> XWPFDocument.PICTURE_TYPE_JPEG;
				case "gif" -> XWPFDocument.PICTURE_TYPE_GIF;
				default -> XWPFDocument.PICTURE_TYPE_PNG;
			};

			final String width = element.attr("width");
			final String height = element.attr("height");
			final int cx = width.isEmpty() ? 600000 : Integer.parseInt(width.replace("px", "")) * 9525;
			final int cy = height.isEmpty() ? 600000 : Integer.parseInt(height.replace("px", "")) * 9525;

			try {
				document.addPictureData(data, type);
				final XWPFRun run = currentParagraph.createRun();
				run.addPicture(
					new java.io.ByteArrayInputStream(data),
					type,
					"Test",
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

			final int cols = htmlRows.getFirst().select("td, th").size();
			final XWPFTable table = document.createTable(htmlRows.size(), cols);

			final var tblGrid = table.getCTTbl().addNewTblGrid();
			for (int i = 0; i < cols; i++) {
				tblGrid.addNewGridCol().setW(BigInteger.valueOf(12240 / cols));
			}

			applyTableStyle(element, table);
			applyColumnWidths(element, table);
			for (int y = 0; y < htmlRows.size(); y++) {
				final Elements htmlCells = htmlRows.get(y).select("td, th");
				final XWPFTableRow row = table.getRow(y);

				for (int x = 0; x < htmlCells.size(); x++) {
					currentCell = row.getCell(x);
					currentParagraph = currentCell.getParagraphs().getFirst();

					final FormatState cellFormat = htmlCells.get(x).tagName().equalsIgnoreCase("th")
						? format.withBold()
						: format;
					processChildren(htmlCells.get(x), cellFormat);
					currentCell = null;
				}
			}
		}

		private void processHeading(final Element element) {
			final int level = Integer.parseInt(element.tagName().substring(1));
			if (currentCell != null) {
				if (!currentParagraph.getRuns().isEmpty()) {
					currentParagraph = currentCell.addParagraph();
				}
			} else {
				currentParagraph = document.createParagraph();
			}
			applyParagraphStyle(element);
			final XWPFRun run = currentParagraph.createRun();
			currentParagraph.setSpacingAfter(100);
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

		private void processBlock(final Element element, final FormatState format) {
			if (currentCell != null) {
				if (currentParagraph.getRuns().isEmpty()) {
					// reuse existing paragraph
				} else {
					currentParagraph = currentCell.addParagraph();
				}
			} else {
				currentParagraph = document.createParagraph();
			}
			applyParagraphStyle(element);
			currentParagraph.setSpacingAfter(100);
			processChildren(element, format);
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

			final var tblGrid = xwpfTable.getCTTbl().getTblGrid();
			if (tblGrid == null) {
				return;
			}

			final int gridCols = tblGrid.sizeOfGridColArray();

			final int pageWidth = 12240;

			for (int i = 0; i < cols.size() && i < gridCols; i++) {
				final String width = cols.get(i).attr("width");
				if (!width.isEmpty() && width.endsWith("%")) {
					final int pct = Integer.parseInt(width.replace("%", "").trim());
					final int widthTwips = pageWidth * pct / 100;
					tblGrid.getGridColArray(i).setW(BigInteger.valueOf(widthTwips));
				}
			}
		}

		private void applyTableStyle(final Element element, final XWPFTable table) {
			final var tblPr = table.getCTTbl().addNewTblPr();
			tblPr.addNewTblStyle().setVal("TableGrid");

			final var tblW = tblPr.addNewTblW();
			tblW.setW(BigInteger.valueOf(5000));
			tblW.setType(STTblWidth.Enum.forString("pct"));

			final var tblLayout = tblPr.addNewTblLayout();
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
			HEADING, P, DIV, TABLE, TR, TD, TH, B, I, U, IMG, UNKNOWN;

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

		private final String css;

		CssParser(final Document html) {
			this.css = html.select("style").html();
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

			final var styles = doc.createStyles();
			final var ctStyle = CTStyle.Factory.newInstance();
			ctStyle.setType(STStyleType.TABLE);
			ctStyle.setStyleId("TableGrid");
			ctStyle.addNewName().setVal("Table Grid");
			ctStyle.addNewQFormat();

			final var tblPr = ctStyle.addNewTblPr();

			for (final var entry : allProps.entrySet()) {
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
					borderWidth = Integer.parseInt(part.replace("px", "").trim());
				}
				else {
					final String hex = toHexColor(part);
					if (hex != null) {
						borderColor = hex;
					}
				}
			}

			final var borders = tblPr.addNewTblBorders();
			final var borderType = STBorder.Enum.forString("single");

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
				final int pct = Integer.parseInt(width.replace("%", "").trim());
				final var tblW = tblPr.addNewTblW();
				tblW.setW(BigInteger.valueOf(pct * 50));
				tblW.setType(STTblWidth.Enum.forString("pct"));
			}
		}

		Map<String, String> getAllProperties(final String selector) {
			final Map<String, String> result = new java.util.LinkedHashMap<>();

			for (final String rule : css.split("}")) {
				final String[] parts = rule.split("\\{", 2);
				if (parts.length < 2) {
					continue;
				}

				final String selectors = parts[0].trim().toLowerCase();
				final String declarations = parts[1].trim().toLowerCase();

				if (!java.util.Arrays.asList(selectors.split("\\s*,\\s*")).contains(selector)) {
					continue;
				}

				for (final String decl : declarations.split(";")) {
					final String[] kv = decl.split(":", 2);
					if (kv.length == 2) {
						result.put(kv[0].trim(), kv[1].trim());
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
				final var matcher = java.util.regex.Pattern
						.compile("(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)")
						.matcher(value);
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