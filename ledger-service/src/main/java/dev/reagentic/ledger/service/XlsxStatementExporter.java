package dev.reagentic.ledger.service;

import dev.reagentic.ledger.domain.LedgerEntry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class XlsxStatementExporter {

    private static final byte[] NAVY = {0x2D, 0x43, (byte) 0xF5};
    private static final byte[] GOLD = {(byte) 0xC9, (byte) 0xA2, 0x27};
    private static final byte[] BAND = {(byte) 0xF4, (byte) 0xF5, (byte) 0xFA};
    private static final byte[] WHITE = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private static final byte[] BORDER = {(byte) 0xE2, (byte) 0xE4, (byte) 0xEC};
    private static final byte[] MUTED = {0x6B, 0x72, (byte) 0x80};

    private static final String CURRENCY_FORMAT = "$#,##0.00";

    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "OPENING", "Opening balance",
            "DEBIT", "Transfer out",
            "CREDIT", "Transfer in",
            "DEBIT_FAILED", "Failed transfer",
            "COMPENSATE", "Refund");

    private XlsxStatementExporter() {
    }

    public static byte[] render(String accountId, List<LedgerEntry> entries) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Statement");
            sheet.setTabColor(new XSSFColor(NAVY, null));
            sheet.createFreezePane(0, 4);
            sheet.setColumnWidth(0, 26 * 256);
            sheet.setColumnWidth(1, 24 * 256);
            sheet.setColumnWidth(2, 38 * 256);
            sheet.setColumnWidth(3, 16 * 256);
            sheet.setColumnWidth(4, 16 * 256);

            titleRow(wb, sheet);
            subtitleRow(wb, sheet, accountId);
            sheet.createRow(2).setHeightInPoints(8f);
            headerRow(wb, sheet);
            dataRows(wb, sheet, entries);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render XLSX statement", e);
        }
    }

    private static void titleRow(XSSFWorkbook wb, XSSFSheet sheet) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(30f);
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(NAVY, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
        style.setFont(font);
        Cell cell = row.createCell(0);
        cell.setCellValue("REAGENTIC BANK");
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        for (int i = 1; i <= 4; i++) {
            row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellStyle(style);
        }
    }

    private static void subtitleRow(XSSFWorkbook wb, XSSFSheet sheet, String accountId) {
        Row row = sheet.createRow(1);
        row.setHeightInPoints(20f);
        CellStyle title = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        titleFont.setColor(new XSSFColor(NAVY, null));
        title.setFont(titleFont);
        title.setVerticalAlignment(VerticalAlignment.CENTER);
        Cell heading = row.createCell(0);
        heading.setCellValue("Account statement \u00b7 " + accountId);
        heading.setCellStyle(title);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));
        for (int i = 1; i <= 3; i++) {
            row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellStyle(title);
        }
        CellStyle generated = wb.createCellStyle();
        XSSFFont generatedFont = wb.createFont();
        generatedFont.setFontHeightInPoints((short) 10);
        generatedFont.setColor(new XSSFColor(MUTED, null));
        generated.setFont(generatedFont);
        generated.setAlignment(HorizontalAlignment.RIGHT);
        generated.setVerticalAlignment(VerticalAlignment.CENTER);
        Cell stamp = row.createCell(4);
        stamp.setCellValue("Generated " + Instant.now().toString());
        stamp.setCellStyle(generated);
    }

    private static void headerRow(XSSFWorkbook wb, XSSFSheet sheet) {
        Row row = sheet.createRow(3);
        row.setHeightInPoints(20f);
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(GOLD, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
        style.setFont(font);
        String[] headers = {"Date", "Description", "Reference", "Amount", "Balance"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private static void dataRows(XSSFWorkbook wb, XSSFSheet sheet, List<LedgerEntry> entries) {
        CellStyle amountStyle = amountStyle(wb);
        for (int i = 0; i < entries.size(); i++) {
            LedgerEntry e = entries.get(i);
            Row row = sheet.createRow(4 + i);
            row.setHeightInPoints(18f);
            CellStyle base = dataStyle(wb, i % 2 == 1);
            String[] values = {
                    Instant.ofEpochMilli(e.getCreatedAt()).toString(),
                    describe(e.getType()),
                    e.getPaymentId() == null ? "" : e.getPaymentId()
            };
            for (int c = 0; c < values.length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(values[c]);
                cell.setCellStyle(base);
            }
            Cell amount = row.createCell(3);
            amount.setCellValue(e.getSignedAmount().doubleValue());
            amount.setCellStyle(amountStyle);
            Cell balance = row.createCell(4);
            balance.setCellValue(e.getBalanceAfter().doubleValue());
            balance.setCellStyle(amountStyle);
        }
    }

    private static CellStyle dataStyle(XSSFWorkbook wb, boolean banded) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(banded ? BAND : WHITE, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        border(style);
        return style;
    }

    private static CellStyle amountStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat(CURRENCY_FORMAT));
        style.setAlignment(HorizontalAlignment.RIGHT);
        border(style);
        return style;
    }

    private static void border(CellStyle style) {
        XSSFCellStyle xstyle = (XSSFCellStyle) style;
        XSSFColor color = new XSSFColor(BORDER, null);
        xstyle.setBorderTop(BorderStyle.THIN);
        xstyle.setTopBorderColor(color);
        xstyle.setBorderBottom(BorderStyle.THIN);
        xstyle.setBottomBorderColor(color);
        xstyle.setBorderLeft(BorderStyle.THIN);
        xstyle.setLeftBorderColor(color);
        xstyle.setBorderRight(BorderStyle.THIN);
        xstyle.setRightBorderColor(color);
    }

    private static String describe(String type) {
        return type == null ? "" : DESCRIPTIONS.getOrDefault(type, type);
    }
}