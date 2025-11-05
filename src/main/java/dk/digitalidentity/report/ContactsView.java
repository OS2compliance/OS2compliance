package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.Asset;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.util.List;
import java.util.Map;

import static dk.digitalidentity.report.XlsUtil.createCell;

@SuppressWarnings("Convert2MethodRef")
public class ContactsView extends AbstractXlsView {
    private CellStyle style;
    @Override
    protected void buildExcelDocument(final Map<String, Object> model, final Workbook workbook, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Sheet sheet = workbook.createSheet("Kontakter");
        createMainHeader(workbook, sheet);

        style = workbook.createCellStyle();
        style.setWrapText(true);

        final CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat((short) 14);

        final List<Asset> assets = (List<Asset>) model.get("assets");

        int rowCount = 1;
        for (final Asset asset : assets) {
			final Row row = sheet.createRow(rowCount++);
			createCell(row, 0, asset.getName(), style);
			createCell(row, 1, asset.getSupplier() != null ? asset.getSupplier().getContact() : "", style);
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createMainHeader(final Workbook workbook, final Sheet sheet) {
        final Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        final Row header = sheet.createRow(0);
        createCell(header, 0, "IT-system", headerStyle);
        createCell(header, 1, "Kontakt", headerStyle);
    }
}
