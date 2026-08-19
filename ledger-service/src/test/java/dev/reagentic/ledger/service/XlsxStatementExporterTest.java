package dev.reagentic.ledger.service;

import dev.reagentic.ledger.domain.LedgerEntry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XlsxStatementExporterTest {

    private static final byte[] NAVY = {0x2D, 0x43, (byte) 0xF5};
    private static final byte[] GOLD = {(byte) 0xC9, (byte) 0xA2, 0x27};
    private static final byte[] BAND = {(byte) 0xF4, (byte) 0xF5, (byte) 0xFA};
    private static final byte[] WHITE = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    private static LedgerEntry entry(long createdAt, String paymentId, String type, String signed, String balanceAfter) {
        LedgerEntry e = new LedgerEntry("acc-checking-0001", paymentId, type,
                new BigDecimal(signed), new BigDecimal(balanceAfter));
        try {
            Field f = LedgerEntry.class.getDeclaredField("createdAt");
            f.setAccessible(true);
            f.setLong(e, createdAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return e;
    }

    private static XSSFWorkbook open(byte[] bytes) {
        try {
            return new XSSFWorkbook(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] fill(XSSFCellStyle style) {
        return style.getFillForegroundXSSFColor().getRGB();
    }

    @Test
    void rendersWorkbookWithStatementSheetAndBrandedTab() {
        XSSFWorkbook wb = open(XlsxStatementExporter.render("acc-checking-0001", List.of()));
        assertEquals(1, wb.getNumberOfSheets());
        XSSFSheet sheet = wb.getSheet("Statement");
        assertNotNull(sheet);
        assertNotNull(sheet.getTabColor());
        assertArrayEquals(NAVY, ((XSSFColor) sheet.getTabColor()).getRGB());
    }

    @Test
    void laysOutTitleSubtitleSpacerAndHeaderRows() {
        XSSFWorkbook wb = open(XlsxStatementExporter.render("acc-checking-0001", List.of(
                entry(1785324900000L, "pmt-1", "DEBIT", "-50.00", "950.00"))));
        XSSFSheet sheet = wb.getSheetAt(0);
        assertEquals(5, sheet.getLastRowNum() + 1);
        assertEquals("REAGENTIC BANK", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals(2, sheet.getMergedRegions().size());
        assertTrue(sheet.getRow(1).getCell(0).getStringCellValue().contains("acc-checking-0001"));
        assertEquals("Date", sheet.getRow(3).getCell(0).getStringCellValue());
        assertEquals("Description", sheet.getRow(3).getCell(1).getStringCellValue());
        assertEquals("Reference", sheet.getRow(3).getCell(2).getStringCellValue());
        assertEquals("Amount", sheet.getRow(3).getCell(3).getStringCellValue());
        assertEquals("Balance", sheet.getRow(3).getCell(4).getStringCellValue());
    }

    @Test
    void rendersOneDataRowPerEntryInChronologicalOrder() {
        XSSFWorkbook wb = open(XlsxStatementExporter.render("acc-checking-0001", List.of(
                entry(1785324900000L, "OPENING", "OPENING", "1000.00", "1000.00"),
                entry(1785411300000L, "pmt-1", "DEBIT", "-50.00", "950.00"),
                entry(1785497700000L, "pmt-2", "CREDIT", "25.00", "975.00"))));
        XSSFSheet sheet = wb.getSheetAt(0);
        assertEquals(7, sheet.getLastRowNum() + 1);
        assertTrue(sheet.getRow(4).getCell(0).getStringCellValue().startsWith("2026-07-29T11:35:00Z"));
        assertEquals(950.0, sheet.getRow(5).getCell(4).getNumericCellValue(), 0.0001);
        assertEquals("pmt-2", sheet.getRow(6).getCell(2).getStringCellValue());
    }

    @Test
    void stylesTitleRowWithBrandNavyAndWhiteBold() {
        XSSFWorkbook wb = open(XlsxStatementExporter.render("acc-checking-0001", List.of()));
        XSSFCellStyle style = wb.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
        assertArrayEquals(NAVY, fill(style));
        XSSFFont font = wb.getFontAt(style.getFontIndex());
        assertTrue(font.getBold());
        assertArrayEquals(WHITE, font.getXSSFColor().getRGB());
    }

    @Test
    void stylesHeaderRowWithGoldFillAndWhiteBoldFont() {
        XSSFWorkbook wb = open(XlsxStatementExporter.render("acc-checking-0001", List.of()));
        XSSFCellStyle style = wb.getSheetAt(0).getRow(3).getCell(0).getCellStyle();
        assertArrayEquals(GOLD, fill(style));
        XSSFFont font = wb.getFontAt(style.getFontIndex());
        assertTrue(font.getBold());
        assertArrayEquals(WHITE, font.getXSSFColor().getRGB());
    }

    @Test
    void formatsAmountAndBalanceAsSignedCurrencyRightAligned() {
        XSSFWorkbook wb = open(XlsxStatementExporter.render("acc-checking-0001", List.of(
                entry(1785411300000L, "pmt-1", "DEBIT", "-50.00", "950.00"))));
        XSSFSheet sheet = wb.getSheetAt(0);
        Cell amount = sheet.getRow(4).getCell(3);
        Cell balance = sheet.getRow(4).getCell(4);
        assertEquals(-50.0, amount.getNumericCellValue(), 0.0001);
        assertEquals(950.0, balance.getNumericCellValue(), 0.0001);
        assertEquals("$#,##0.00", amount.getCellStyle().getDataFormatString());
        assertEquals("$#,##0.00", balance.getCellStyle().getDataFormatString());
        assertEquals(HorizontalAlignment.RIGHT, amount.getCellStyle().getAlignment());
        assertEquals(HorizontalAlignment.RIGHT, balance.getCellStyle().getAlignment());
    }

    @Test
    void humanizesDescriptionsIntoStatementCells() {
        XSSFWorkbook wb = open(XlsxStatementExporter.render("acc-checking-0001", List.of(
                entry(1785324900000L, "OPENING", "OPENING", "1000.00", "1000.00"),
                entry(1785411300000L, "pmt-1", "DEBIT", "-50.00", "950.00"),
                entry(1785411300000L, "pmt-2", "COMPENSATE", "50.00", "1000.00"))));
        XSSFSheet sheet = wb.getSheetAt(0);
        assertEquals("Opening balance", sheet.getRow(4).getCell(1).getStringCellValue());
        assertEquals("Transfer out", sheet.getRow(5).getCell(1).getStringCellValue());
        assertEquals("Refund", sheet.getRow(6).getCell(1).getStringCellValue());
    }

    @Test
    void bandedDataRowsWithBordersAndFreezeOnHeader() {
        XSSFWorkbook wb = open(XlsxStatementExporter.render("acc-checking-0001", List.of(
                entry(1785324900000L, "pmt-1", "DEBIT", "-50.00", "950.00"),
                entry(1785411300000L, "pmt-2", "CREDIT", "25.00", "975.00"))));
        XSSFSheet sheet = wb.getSheetAt(0);
        assertArrayEquals(WHITE, fill(sheet.getRow(4).getCell(0).getCellStyle()));
        assertArrayEquals(BAND, fill(sheet.getRow(5).getCell(0).getCellStyle()));
        assertEquals(BorderStyle.THIN, sheet.getRow(4).getCell(0).getCellStyle().getBorderTop());
        assertTrue(sheet.getPaneInformation().isFreezePane());
        assertEquals(4, sheet.getPaneInformation().getHorizontalSplitPosition());
    }

    @Test
    void rendersStyledHeaderForEmptyLedger() {
        XSSFWorkbook wb = open(XlsxStatementExporter.render("acc-checking-0001", List.of()));
        XSSFSheet sheet = wb.getSheetAt(0);
        assertEquals(4, sheet.getLastRowNum() + 1);
        assertArrayEquals(GOLD, fill(sheet.getRow(3).getCell(0).getCellStyle()));
    }
}