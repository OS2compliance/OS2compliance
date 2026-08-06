package dk.digitalidentity.service.exporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
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
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HtmlToDocxExporterService {

	public ByteArrayOutputStream convert(@NonNull final String html) throws Exception {
		final Document doc = Jsoup.parseBodyFragment(html);

        try (final XWPFDocument xwpfDoc = new XWPFDocument()) {
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
                default -> processChildren(element, format);
            }
        }

		private void processTable(final Element element, final FormatState format) {
			final Elements htmlRows = element.select("tr");
			if (htmlRows.isEmpty()) {
				return;
			}

			final int cols = htmlRows.getFirst().select("td, th").size();
			final XWPFTable table = document.createTable(htmlRows.size(), cols);

			// Create tblGrid manually
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
					final XWPFTableCell cell = row.getCell(x);
					final XWPFParagraph paragraph = cell.getParagraphs().getFirst();
					final XWPFRun run = paragraph.createRun();
					run.setText(htmlCells.get(x).text());

					if (htmlCells.get(x).tagName().equalsIgnoreCase("th")) {
						run.setBold(true);
					}
				}
			}
		}

		private void processHeading(final Element element) {
			final int level = Integer.parseInt(element.tagName().substring(1));
			currentParagraph = document.createParagraph();
			applyParagraphStyle(element);
			final XWPFRun run = currentParagraph.createRun();
			currentParagraph.setSpacingAfter(100);
			currentParagraph.setSpacingBefore(100);
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
			currentParagraph = document.createParagraph();
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

			final var tblW = tblPr.addNewTblW();
			tblW.setW(BigInteger.valueOf(5000));
			tblW.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.PCT);

			final var tblLayout = tblPr.addNewTblLayout();
			tblLayout.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType.FIXED);
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
			HEADING, P, DIV, TABLE, TR, TD, TH, B, I, U, UNKNOWN;

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
}