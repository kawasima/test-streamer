package example;

import org.apache.commons.io.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class ExcelTest {
    @Test
    public void japanCode() throws IOException {
        System.out.println(getClass().getClassLoader());
        InputStream in = getClass().getResourceAsStream("/iso_3166_2_countries.xlsx");
        assertNotNull(in);
        try {
            XSSFWorkbook book = new XSSFWorkbook(in);
            XSSFSheet sheet = book.getSheetAt(0);
            for (int i=sheet.getFirstRowNum(); i<sheet.getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);
                if ("Japan".equalsIgnoreCase(row.getCell(1).getStringCellValue())) {
                    assertThat("Japanese currency is JPY.", row.getCell(7).getStringCellValue(), CoreMatchers.is("JPY"));
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}

