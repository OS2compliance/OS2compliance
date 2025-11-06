package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.Supplier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			// Collect unique suppliers from both supplier and suppliers
			final Set<Supplier> uniqueSuppliers = new HashSet<>();

			// Add single supplier if present
			if (asset.getSupplier() != null) {
				uniqueSuppliers.add(asset.getSupplier());
			}

			// Add suppliers from suppliers list
			if (asset.getSuppliers() != null) {
				for (final AssetSupplierMapping mapping : asset.getSuppliers()) {
					if (mapping.getSupplier() != null) {
						uniqueSuppliers.add(mapping.getSupplier());
					}
				}
			}

			// Create a row for each unique supplier
			if (uniqueSuppliers.isEmpty()) {
				// If no suppliers, create a row with just the asset name
				final Row row = sheet.createRow(rowCount++);
				createCell(row, 0, asset.getName(), style);
				createCell(row, 1, "", style);
				createCell(row, 2, "", style);
			} else {
				for (final Supplier supplier : uniqueSuppliers) {
					final Row row = sheet.createRow(rowCount++);
					createCell(row, 0, asset.getName(), style);
					createCell(row, 1, supplier.getName(), style);
					createCell(row, 2, supplier.getContact(), style);
				}
			}
		}
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    private void createMainHeader(final Workbook workbook, final Sheet sheet) {
        final Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        final Row header = sheet.createRow(0);
        createCell(header, 0, "IT-system", headerStyle);
        createCell(header, 1, "Leverandør", headerStyle);
        createCell(header, 2, "Kontakt", headerStyle);
    }
}
