package dk.digitalidentity.report;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;

import java.time.LocalDate;

public class XlsUtil {

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

}
