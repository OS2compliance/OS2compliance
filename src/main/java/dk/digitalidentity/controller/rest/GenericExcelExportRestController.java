package dk.digitalidentity.controller.rest;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

// TODO: Remove this as soon as ExcelExportService works
@RestController
public class GenericExcelExportRestController {

	@PostMapping("/export-excel")
	public void exportExcel(@RequestBody GridTableData tableData,
			HttpServletResponse response) throws IOException {

		// Create workbook
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Export");

		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setColor(IndexedColors.BLACK.getIndex());
		headerFont.setFontName("Arial");
		headerFont.setFontHeightInPoints((short) 11);
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		// Create header row
		Row header = sheet.createRow(0);
		for (int i = 0; i < tableData.getColumns().size(); i++) {
			if (tableData.getColumns().get(i).equals("Handlinger")) {
				break;
			}
			Cell cell = header.createCell(i);
			cell.setCellValue(tableData.getColumns().get(i));
			cell.setCellStyle(headerStyle);

		}

		CellStyle rowStyle = workbook.createCellStyle();
		rowStyle.setWrapText(true);

		rowStyle.setBorderBottom(BorderStyle.THIN);
		rowStyle.setBorderTop(BorderStyle.THIN);
		rowStyle.setBorderLeft(BorderStyle.THIN);
		rowStyle.setBorderRight(BorderStyle.THIN);

		CellStyle alternateRowStyle = workbook.createCellStyle();
		alternateRowStyle.cloneStyleFrom(rowStyle);
		alternateRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		alternateRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		// Fill data
		for (int r = 0; r < tableData.getRows().size(); r++) {
			Row row = sheet.createRow(r + 1);
			List<Object> rowData = tableData.getRows().get(r);

			CellStyle styleToUse = (r % 2 == 0) ? rowStyle : alternateRowStyle;

			for (int c = 0; c < rowData.size(); c++) {
				if (tableData.getColumns().get(c).equals("Handlinger")) {
					break; // Stop here too
				}
				Cell cell = row.createCell(c);
				cell.setCellValue(rowData.get(c) != null ? rowData.get(c).toString() : "");
				cell.setCellStyle(styleToUse);
			}
		}

		for (int i = 0; i < tableData.getColumns().size(); i++) {
			if (tableData.getColumns().get(i).equals("Handlinger")) {
				break;
			}
			sheet.autoSizeColumn(i);
		}

		// Set response headers
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Content-Disposition", "attachment; filename=export.xlsx");

		// Write workbook to output
		workbook.write(response.getOutputStream());
		workbook.close();
	}

	public static class GridTableData {
		private List<String> columns;
		private List<List<Object>> rows;

		public List<String> getColumns() {
			return columns;
		}

		public void setColumns(List<String> columns) {
			this.columns = columns;
		}

		public List<List<Object>> getRows() {
			return rows;
		}

		public void setRows(List<List<Object>> rows) {
			this.rows = rows;
		}
	}
}
