package com.group14.expensemanager.utils;

import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.model.Product;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** A real Office Open XML workbook, using inline strings so user text never becomes a formula. */
public final class ExcelExporter {
    public static final String MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String[] HEADERS = {"Mã phiếu", "Ngày", "Đối tác", "Điện thoại", "Ghi chú",
            "Mã hàng", "Sản phẩm", "Nhóm sản phẩm", "Loại", "Số lượng", "Đơn vị", "Đơn giá (VNĐ)", "Thành tiền (VNĐ)"};

    private ExcelExporter() { }

    public static String fileName() {
        return "Bao-cao-kho-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()) + ".xlsx";
    }

    /** Writes all movement lines; closes the supplied stream, including on failure. */
    public static void write(OutputStream output, List<Transaction> transactions, List<Product> products) throws IOException {
        if ((long) transactions.size() + products.size() + 20 > 1_048_576)
            throw new IOException("Báo cáo vượt giới hạn số dòng Excel.");
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            entry(zip, "[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
            entry(zip, "_rels/.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip, "xl/workbook.xml", "<workbook xmlns=\"" + NS + "\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                    + "<sheets><sheet name=\"Báo cáo kho\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                    + "<calcPr fullCalcOnLoad=\"1\"/></workbook>");
            entry(zip, "xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                    + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            entry(zip, "xl/styles.xml", "<styleSheet xmlns=\"" + NS + "\">"
                    + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"[Green]+#,##0.00;[Red]-#,##0.00;0.00\"/></numFmts>"
                    + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
                    + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFA3CDF6\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                    + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                    + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                    + "<cellXfs count=\"4\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                    + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFill=\"1\" applyFont=\"1\"/>"
                    + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                    + "<xf numFmtId=\"164\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/></cellXfs>"
                    + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>");
            List<Transaction> incoming = new ArrayList<>(), outgoing = new ArrayList<>();
            Map<Integer, double[]> totals = new HashMap<>();
            double totalIn = 0, totalOut = 0;
            for (Transaction t : transactions) {
                (t.isIncome() ? incoming : outgoing).add(t);
                double[] sum = totals.computeIfAbsent(t.getProductId(), key -> new double[2]);
                sum[t.isIncome() ? 0 : 1] += t.getQuantity();
                if (t.isIncome()) totalIn += t.getAmount(); else totalOut += t.getAmount();
            }
            Writer writer = startSheet(zip);
            int incomingStart = 10, outgoingStart = incomingStart + incoming.size() + 4;
            heading(writer, 1, "BÁO CÁO NHẬP – XUẤT – TỒN");
            summary(writer, 3, "Tổng tiền nhập (VNĐ)", sumFormula(incomingStart, incoming.size()), totalIn, 2);
            summary(writer, 4, "Tổng tiền xuất (VNĐ)", sumFormula(outgoingStart, outgoing.size()), totalOut, 2);
            summary(writer, 5, "Chênh lệch xuất − nhập (VNĐ)", "C4-C3", totalOut-totalIn, 3);
            heading(writer, 6, "Dương: xuất > nhập; âm: xuất < nhập; 0: cân bằng. Đây là chênh lệch giá trị, không phải lợi nhuận.");
            heading(writer, 8, "1. NHẬP KHO");
            movements(writer, 9, incoming, "Nhà cung cấp");
            heading(writer, outgoingStart - 2, "2. XUẤT KHO");
            movements(writer, outgoingStart - 1, outgoing, "Khách hàng");
            String[] headers = {"Mã hàng", "Sản phẩm", "Nhóm sản phẩm", "Đơn vị", "Tổng nhập", "Tổng xuất", "Tồn kho",
                    "Giá tham chiếu (VNĐ)", "Giá trị tồn tham chiếu (VNĐ)", "Tồn tối thiểu", "Trạng thái"};
            int row = outgoingStart + outgoing.size() + 2;
            heading(writer, row++, "3. HÀNG TỒN (toàn bộ lịch sử)");
            header(writer, row++, headers);
            for (Product p : products) {
                double[] sum = totals.getOrDefault(p.id, new double[2]);
                double stock = sum[0] - sum[1];
                if (Math.abs(stock) < 1e-8) stock = 0;
                writer.write("<row r=\"" + row + "\">");
                text(writer, 0, row, p.sku, 0); text(writer, 1, row, p.name, 0);
                text(writer, 2, row, p.category, 0); text(writer, 3, row, p.unit, 0);
                number(writer, 4, row, sum[0]); number(writer, 5, row, sum[1]); number(writer, 6, row, stock);
                number(writer, 7, row, p.referencePrice); number(writer, 8, row, stock * p.referencePrice);
                number(writer, 9, row, p.minimumStock);
                text(writer, 10, row, stock < 0 ? "Âm kho" : stock == 0 ? "Hết hàng" : stock <= p.minimumStock ? "Sắp hết" : "Còn hàng", 0);
                writer.write("</row>"); row++;
            }
            writer.write("</sheetData></worksheet>");
            writer.flush();
            zip.closeEntry();
        }
    }

    private static Writer startSheet(ZipOutputStream zip) throws IOException {
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            Writer writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"" + NS + "\">"
                    + "<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>"
                    + "<cols><col min=\"1\" max=\"13\" width=\"24\" customWidth=\"1\"/></cols><sheetData>");
            return writer;
    }

    private static void movements(Writer writer, int headerRow, List<Transaction> transactions, String partnerLabel) throws IOException {
            String[] headers = HEADERS.clone(); headers[2] = partnerLabel;
            header(writer, headerRow, headers);
            SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT);
            int row = headerRow + 1;
            for (Transaction t : transactions) {
                writer.write("<row r=\"" + row + "\">");
                String[] values = {StockDocuments.number(t), date.format(new Date(t.getTimestamp())),
                        t.getPartnerName(), t.getPartnerPhone(), t.getNote(), t.getSku(), t.getTitle(),
                        t.getCategory(), t.isIncome() ? "Nhập kho" : "Xuất kho"};
                for (int c = 0; c < values.length; c++) text(writer, c, row, values[c], 0);
                number(writer, 9, row, t.getQuantity());
                text(writer, 10, row, t.getUnit(), 0);
                number(writer, 11, row, t.getUnitPrice());
                number(writer, 12, row, t.getAmount());
                writer.write("</row>");
                row++;
            }
    }

    private static void header(Writer writer, int row, String[] labels) throws IOException {
        writer.write("<row r=\"" + row + "\">");
        for (int c = 0; c < labels.length; c++) text(writer, c, row, labels[c], 1);
        writer.write("</row>");
    }

    private static void heading(Writer writer, int row, String label) throws IOException {
        header(writer, row, new String[]{label});
    }

    private static String sumFormula(int start, int count) {
        return count == 0 ? "0" : "SUM(M" + start + ":M" + (start + count - 1) + ")";
    }

    private static void summary(Writer writer, int row, String label, String formula, double value, int style) throws IOException {
        if (!Double.isFinite(value)) throw new IOException("Tổng tiền không hợp lệ.");
        writer.write("<row r=\"" + row + "\">");
        text(writer, 0, row, label, 1);
        writer.write("<c r=\"C" + row + "\" s=\"" + style + "\"><f>" + formula + "</f><v>" + value + "</v></c></row>");
    }

    private static void entry(ZipOutputStream zip, String name, String xml) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void text(Writer writer, int column, int row, String value, int style) throws IOException {
        writer.write("<c r=\"" + (char) ('A' + column) + row + "\" t=\"inlineStr\" s=\"" + style
                + "\"><is><t xml:space=\"preserve\">" + escape(value) + "</t></is></c>");
    }

    private static void number(Writer writer, int column, int row, double value) throws IOException {
        if (!Double.isFinite(value)) throw new IOException("Dữ liệu số trong báo cáo không hợp lệ.");
        writer.write("<c r=\"" + (char) ('A' + column) + row + "\" s=\"2\"><v>" + value + "</v></c>");
    }

    private static String escape(String value) throws IOException {
        if (value == null) return "";
        if (value.length() > 32767) throw new IOException("Nội dung ô vượt giới hạn Excel.");
        StringBuilder out = new StringBuilder();
        value.codePoints().filter(c -> c == 9 || c == 10 || c == 13 || c >= 32 && c <= 0xD7FF
                || c >= 0xE000 && c <= 0xFFFD || c >= 0x10000 && c <= 0x10FFFF).forEach(out::appendCodePoint);
        return out.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
