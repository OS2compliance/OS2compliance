package dk.digitalidentity.service;

import dk.digitalidentity.model.ExcelColumn;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * This class exports server side grid tables to excel.
 */
@Service
public class ExcelExportService {

	/**
	 * @param data - The data we provide when calling this method in each of the rest controllers. It contains the columns and rows.
	 * @param fileName - The file name of the exported Excel sheet.
	 * @param response - The reponse we send back.
	 * @throws IOException - An exception we throw when an error happens.
	 */
	public void exportToExcel(List<?> data, String fileName, HttpServletResponse response) throws IOException {
		if (data == null || data.isEmpty()) {
			throw new IllegalArgumentException("No data to export");
		}

		// Create workbook
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Export");

		// Create styles
		CellStyle headerStyle = createHeaderStyle(workbook);
		CellStyle rowStyle = createRowStyle(workbook);
		CellStyle alternateRowStyle = createAlternateRowStyle(workbook, rowStyle);

		// Get column information from the DTO class
		Class<?> dtoClass = data.get(0).getClass();
		List<Field> exportableFields = getExportableFields(dtoClass);
		List<String> columnHeaders = getColumnHeaders(exportableFields);

		// Create header row
		createHeaderRow(sheet, columnHeaders, headerStyle);

		// Fill data rows
		fillDataRows(sheet, data, exportableFields, rowStyle, alternateRowStyle);

		// Auto-size columns
		autoSizeColumns(sheet, columnHeaders.size());

		// Set response headers
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

		// Write workbook to output
		workbook.write(response.getOutputStream());
		workbook.close();
	}

	private List<Field> getExportableFields(Class<?> dtoClass) {
		return Arrays.stream(dtoClass.getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(ExcelColumn.class))
				.sorted(Comparator.comparingInt(f -> f.getAnnotation(ExcelColumn.class).order()))
				.peek(f -> f.setAccessible(true))
				.toList();
	}

	private boolean isActionField(String fieldName) {
		String lowerName = fieldName.toLowerCase();
		return lowerName.equals("handlinger") ||
				lowerName.equals("actions") ||
				lowerName.equals("action") ||
				lowerName.equals("id");
	}


	private List<String> getColumnHeaders(List<Field> fields) {
		return fields.stream()
				.map(f -> f.getAnnotation(ExcelColumn.class).headerName())
				.toList();
	}

	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setColor(IndexedColors.BLACK.getIndex());
		headerFont.setFontName("Arial");
		headerFont.setFontHeightInPoints((short) 11);
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return headerStyle;
	}

	private CellStyle createRowStyle(Workbook workbook) {
		CellStyle rowStyle = workbook.createCellStyle();
		rowStyle.setWrapText(true);
		rowStyle.setBorderBottom(BorderStyle.THIN);
		rowStyle.setBorderTop(BorderStyle.THIN);
		rowStyle.setBorderLeft(BorderStyle.THIN);
		rowStyle.setBorderRight(BorderStyle.THIN);
		return rowStyle;
	}

	private CellStyle createAlternateRowStyle(Workbook workbook, CellStyle baseStyle) {
		CellStyle alternateRowStyle = workbook.createCellStyle();
		alternateRowStyle.cloneStyleFrom(baseStyle);
		alternateRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		alternateRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return alternateRowStyle;
	}

	private void createHeaderRow(Sheet sheet, List<String> columnHeaders, CellStyle headerStyle) {
		Row header = sheet.createRow(0);
		for (int i = 0; i < columnHeaders.size(); i++) {
			Cell cell = header.createCell(i);
			cell.setCellValue(columnHeaders.get(i));
			cell.setCellStyle(headerStyle);
		}
	}

	private void fillDataRows(Sheet sheet, List<?> data, List<Field> fields,
			CellStyle rowStyle, CellStyle alternateRowStyle) {

		for (int r = 0; r < data.size(); r++) {
			Row row = sheet.createRow(r + 1);
			Object dto = data.get(r);
			CellStyle styleToUse = (r % 2 == 0) ? rowStyle : alternateRowStyle;

			for (int c = 0; c < fields.size(); c++) {
				Field field = fields.get(c);
				try {
					Object value = field.get(dto);
					Cell cell = row.createCell(c);
					cell.setCellValue(value != null ? value.toString() : "");
					cell.setCellStyle(styleToUse);
				} catch (IllegalAccessException e) {
					// Log error and continue
					e.printStackTrace();
				}
			}
		}
	}

	private void autoSizeColumns(Sheet sheet, int columnCount) {
		for (int i = 0; i < columnCount; i++) {
			sheet.autoSizeColumn(i);
		}
	}
}




