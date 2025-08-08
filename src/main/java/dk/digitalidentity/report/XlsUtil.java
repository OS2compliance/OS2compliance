package dk.digitalidentity.report;

import dk.digitalidentity.report.systemowneroverview.SystemOwnerOverviewView;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XlsUtil {

	@FunctionalInterface
	public interface CellValueSetter<T, C> {
		void apply(T one, C two);
	}

    public static void createCell(final Row header, final int column, final String value, final CellStyle style) {
        final Cell cell = header.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    public static void createCell(final Row header, final int column, final LocalDate value, final CellStyle style) {
        final Cell cell = header.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

	public static void setCellValueForLocalDate(LocalDate localDate, Cell cell) {
		cell.setCellValue(localDate);
		CellStyle style = cell.getSheet().getWorkbook().createCellStyle();
		style.setDataFormat(cell.getSheet().getWorkbook().getCreationHelper().createDataFormat().getFormat("dd/mm-yyyy"));
		cell.setCellStyle(style);
	}

	public static void createHeaderRow(Sheet sheet, List<String> headers) {
		Row headerRow = sheet.createRow(0);
		CellStyle style = headerRow.getSheet().getWorkbook().createCellStyle();

		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderBottom(BorderStyle.MEDIUM);

		Font headerFont = headerRow.getSheet().getWorkbook().createFont();
		headerFont.setBold(true);
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		headerFont.setFontHeightInPoints((short) 14);
		style.setFont(headerFont);

		for (int i = 0; i < headers.size(); i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellStyle(style);
			cell.setCellValue(headers.get(i));
		}

		headerRow.setHeightInPoints((short) 20);
	}

	public static <T> void createSheet(String name, List<T> objects, Map<String, CellValueSetter<T, Cell>> converterMap, Workbook workbook) {
		Sheet sheet = workbook.createSheet(name);

		// Create header
		List<String> headers = new ArrayList<>(converterMap.keySet());
		createHeaderRow(sheet, headers);

		List<Map.Entry<String, CellValueSetter<T, Cell>>> converterMapEntriesAsList = converterMap.entrySet().stream().toList();

		// Create rows
		for (int rowIndex = 0; rowIndex < objects.size(); rowIndex++) {
			Row row = sheet.createRow(rowIndex+1); // plus one because of header row

			// Create cells based on the specific cell creation function defined in the convertermap
			for (int columnIndex = 0; columnIndex < converterMapEntriesAsList.size(); columnIndex++) {
				Cell cell = row.createCell(columnIndex);
				converterMapEntriesAsList.get(columnIndex).getValue()
						.apply(objects.get(rowIndex), cell);
			}

		}

		// Autosize columns
		for (int i = 0; i < headers.size(); i++) {
			sheet.autoSizeColumn(i);
		}

	}

	public static void createEmptySheet(String name, List<String> headers, Workbook workbook) {
		Sheet sheet = workbook.createSheet(name);
		createHeaderRow(sheet, headers);
	}


}
