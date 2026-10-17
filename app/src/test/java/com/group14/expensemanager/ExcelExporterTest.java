package com.group14.expensemanager;

import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.model.Product;
import com.group14.expensemanager.utils.ExcelExporter;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.junit.Assert.*;

public class ExcelExporterTest {
    private Map<String, Document> export(List<Transaction> rows) throws Exception {
        return export(rows, Collections.emptyList());
    }
    private Map<String, Document> export(List<Transaction> rows, List<Product> products) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ExcelExporter.write(bytes, rows, products);
        Map<String, Document> parts = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream xml = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024]; int count;
                while ((count = zip.read(buffer)) != -1) xml.write(buffer, 0, count);
                parts.put(entry.getName(), DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new ByteArrayInputStream(xml.toByteArray())));
            }
        }
        return parts;
    }

    private Element cell(Document sheet, String address) {
        for (int i = 0; i < sheet.getElementsByTagName("c").getLength(); i++) {
            Element node = (Element) sheet.getElementsByTagName("c").item(i);
            if (address.equals(node.getAttribute("r"))) return node;
        }
        throw new AssertionError(address);
    }

    @Test public void preservesUnicodePhonesNumbersAndMultiLineDocuments() throws Exception {
        Transaction row = new Transaction("Nước & bánh <mới>", "Đồ uống", 12500.75, Transaction.TYPE_EXPENSE, 0);
        row.setDocumentId("abcdef123"); row.setPartnerName("Chị Hồng"); row.setPartnerPhone("0900123456");
        row.setNote("=1+2\nGhi chú\u0001"); row.setQuantity(2.5); row.setUnitPrice(5000.3);
        Map<String, Document> parts = export(Arrays.asList(row, row));
        assertEquals(6, parts.size());
        assertNotNull(parts.get("[Content_Types].xml"));
        assertNotNull(parts.get("xl/_rels/workbook.xml.rels"));
        Document sheet = parts.get("xl/worksheets/sheet1.xml");
        assertEquals("Nước & bánh <mới>", cell(sheet, "G14").getTextContent());
        assertEquals("0900123456", cell(sheet, "D14").getTextContent());
        assertEquals("inlineStr", cell(sheet, "D14").getAttribute("t"));
        assertEquals("=1+2\nGhi chú", cell(sheet, "E14").getTextContent());
        assertEquals(0, cell(sheet, "E14").getElementsByTagName("f").getLength());
        assertEquals("2.5", cell(sheet, "J14").getTextContent());
        assertEquals("12500.75", cell(sheet, "M14").getTextContent());
        assertEquals(cell(sheet, "A14").getTextContent(), cell(sheet, "A15").getTextContent());
    }

    @Test public void emptyReportStillHasHeaders() throws Exception {
        Map<String, Document> parts = export(Collections.emptyList());
        Document sheet = parts.get("xl/worksheets/sheet1.xml");
        assertEquals("Nhà cung cấp", cell(sheet, "C9").getTextContent());
        assertEquals("Khách hàng", cell(sheet, "C13").getTextContent());
        assertEquals("Trạng thái", cell(sheet, "K17").getTextContent());
        assertEquals("0", cell(sheet, "C3").getElementsByTagName("f").item(0).getTextContent());
    }

    @Test public void separatesMovementsAndIncludesUnmovedProductsInInventory() throws Exception {
        Product a = new Product(); a.id = 1; a.sku = "001"; a.name = "Nước"; a.referencePrice = 10; a.minimumStock = 8;
        Product b = new Product(); b.id = 2; b.sku = "002"; b.name = "Nước";
        Transaction in = new Transaction("Nước", "", 105, Transaction.TYPE_INCOME, 0);
        in.setProductId(1); in.setQuantity(10.5);
        Transaction out = new Transaction("Nước", "", 50, Transaction.TYPE_EXPENSE, 0);
        out.setProductId(1); out.setQuantity(2.5);
        Map<String, Document> parts = export(Arrays.asList(out, in), Arrays.asList(a, b));
        Document inventory = parts.get("xl/worksheets/sheet1.xml");
        assertEquals("Nhập kho", cell(inventory, "I10").getTextContent());
        assertEquals("Xuất kho", cell(inventory, "I15").getTextContent());
        assertEquals("10.5", cell(inventory, "E20").getTextContent());
        assertEquals("2.5", cell(inventory, "F20").getTextContent());
        assertEquals("8.0", cell(inventory, "G20").getTextContent());
        assertEquals("80.0", cell(inventory, "I20").getTextContent());
        assertEquals("Sắp hết", cell(inventory, "K20").getTextContent());
        assertEquals("0.0", cell(inventory, "G21").getTextContent());
        assertEquals("Hết hàng", cell(inventory, "K21").getTextContent());
        Document book = parts.get("xl/workbook.xml");
        assertEquals(1, book.getElementsByTagName("sheet").getLength());
        assertEquals("Báo cáo kho", ((Element) book.getElementsByTagName("sheet").item(0)).getAttribute("name"));
    }

    @Test public void summaryHasCorrectTotalsAndSignedBalance() throws Exception {
        for (double sale : new double[]{50, 100, 150}) {
            Transaction in = new Transaction("Hàng", "", 100, Transaction.TYPE_INCOME, 0);
            Transaction out = new Transaction("Hàng", "", sale, Transaction.TYPE_EXPENSE, 0);
            Document sheet = export(Arrays.asList(out, in)).get("xl/worksheets/sheet1.xml");
            assertEquals("SUM(M10:M10)", cell(sheet, "C3").getElementsByTagName("f").item(0).getTextContent());
            assertEquals("SUM(M15:M15)", cell(sheet, "C4").getElementsByTagName("f").item(0).getTextContent());
            assertEquals("C4-C3", cell(sheet, "C5").getElementsByTagName("f").item(0).getTextContent());
            assertEquals(sale - 100, Double.parseDouble(cell(sheet, "C5").getElementsByTagName("v").item(0).getTextContent()), 0.001);
            assertEquals("3", cell(sheet, "C5").getAttribute("s"));
        }
    }

    @Test(expected = IOException.class) public void rejectsInvalidNumbers() throws Exception {
        Transaction row = new Transaction("Hàng", "", Double.NaN, Transaction.TYPE_INCOME, 0);
        export(Collections.singletonList(row));
    }

    @Test public void propagatesStorageFailure() throws Exception {
        try {
            ExcelExporter.write(new OutputStream() {
                @Override public void write(int b) throws IOException { throw new IOException("disk full"); }
            }, Collections.emptyList(), Collections.emptyList());
            fail("Must not report success when storage fails");
        } catch (IOException expected) {
            assertEquals("disk full", expected.getMessage());
        }
    }
}
