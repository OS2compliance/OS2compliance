package dk.digitalidentity.service.exporter;

import java.io.IOException;
import java.math.BigInteger;

import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import com.itextpdf.styledxmlparser.jsoup.Jsoup;
import com.itextpdf.styledxmlparser.jsoup.nodes.Document;
import com.itextpdf.styledxmlparser.jsoup.nodes.Element;
import com.itextpdf.styledxmlparser.jsoup.select.Elements;

@Service
public class DPIADocxExportService {

	public XWPFDocument export(final String html) throws IOException {
		final XWPFDocument doc = new XWPFDocument();
		final Document htmlDoc = Jsoup.parse(html);

		createTitle(doc, htmlDoc.getElementById("Title"));

		for (final Element section : htmlDoc.select(".DPIASection")) {
			createSection(doc, section);
		}

		createSection(doc, htmlDoc.getElementById("RiskAssessments"));

		createSection(doc, htmlDoc.getElementById("ConclusionSection"));
		
		return doc;
	}

	private static void createSection(final XWPFDocument doc, final Element section) {
		createSectionHeader(doc, section);

		createTable(doc, section.selectFirst("table"));
	}

	private static void createTitle(final XWPFDocument doc, final Element htmlTitle) {
		final XWPFHeader header = doc.createHeader(HeaderFooterType.FIRST);

		final XWPFParagraph title = header.createParagraph();
		title.setAlignment(ParagraphAlignment.CENTER);
		final XWPFRun run = title.createRun();
		run.setBold(true);
		run.setFontSize(16.5);
		run.setText(htmlTitle.text());
	}

	private static void createTable(final XWPFDocument doc, final Element htmlTable) {
		if(htmlTable == null) {
			return;
		}

		final Elements htmlRows = htmlTable.select("tr");
		final XWPFTable table = doc.createTable(htmlRows.size(), htmlRows.getFirst().select("td, th").size());
		table.getCTTbl().addNewTblGrid().addNewGridCol().setW(BigInteger.valueOf(3400));
		table.getCTTbl().getTblGrid().addNewGridCol().setW(BigInteger.valueOf(6600));
		for (int y = 0; y < htmlRows.size(); ++y) {
			final Element htmlRow = htmlRows.get(y);
			final Elements htmlCells = htmlRow.select("td, th");

			final XWPFTableRow row = table.getRow(y);
			for (int x = 0; x < htmlCells.size(); x++) {
				final Element htmlCell = htmlCells.get(x);

				final XWPFTableCell cell = row.getCell(x);

				final XWPFParagraph paragraph = cell.getParagraphs().getFirst();
				final XWPFRun run = paragraph.createRun();
				run.setFontSize(8.2);
				run.setText(htmlCell.text());
			}
		}

		// add space between table and next section
		doc.createParagraph();
	}

	private static void createSectionHeader(final XWPFDocument doc, Element section) {
		final String dpiaHeaderText = section.selectFirst(".DPIAHeader").text();
		if(!dpiaHeaderText.isBlank()) {
			final XWPFParagraph heading = doc.createParagraph();
			heading.getCTP().addNewPPr().addNewSpacing().setAfter(BigInteger.valueOf(200));
			final XWPFRun run = heading.createRun();
			run.setText(dpiaHeaderText);
			run.setBold(true);
			run.setFontSize(12.4);
		}
		final String DPIADescription = section.selectFirst(".DPIADescription").text();
		if(!DPIADescription.isBlank()) {
			final XWPFParagraph text = doc.createParagraph();
			text.getCTP().addNewPPr().addNewSpacing().setAfter(BigInteger.valueOf(200));
			final XWPFRun textRun = text.createRun();
			textRun.setText(DPIADescription);
			textRun.setFontSize(8.2);
		}
	}
}