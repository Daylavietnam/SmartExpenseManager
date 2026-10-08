package com.group14.expensemanager;

import com.group14.expensemanager.model.*;
import com.group14.expensemanager.utils.InventoryChatInterpreter;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class InventoryChatInterpreterTest {
    private Product product(int id, String sku, String name, String unit) {
        Product p = new Product(); p.id = id; p.sku = sku; p.name = name; p.unit = unit; return p;
    }
    private final List<Product> catalog = Arrays.asList(product(1, "NS001", "Nước suối", "chai"), product(2, "BUT02", "Bút 2B", "cái"));
    private InventoryChatInterpreter.Result parse(String text) { return InventoryChatInterpreter.interpret(text, catalog, null); }

    @Test public void understandsVietnameseReceiptIssueAndTypo() {
        for (String text : new String[]{"Tôi đã nhập thêm 20 chai nước suối", "thêm 20 sản phẩm NS001", "nhap NS001 so luong 20"}) {
            InventoryChatInterpreter.Result r = parse(text); assertTrue(text, r.ready());
            assertEquals(Transaction.TYPE_INCOME, r.draft.type); assertEquals(20, r.draft.quantity, 0); assertEquals(1, r.product.id);
        }
        for (String text : new String[]{"Tôi đã xuất 5 chai nước suối", "tôi đã suất 5 NS001", "bán 5 NS001"}) {
            InventoryChatInterpreter.Result r = parse(text); assertFalse(text, r.ready()); assertNotNull(r.draft);
            assertEquals(Transaction.TYPE_EXPENSE, r.draft.type); assertEquals(5, r.draft.quantity, 0);
        }
    }
    @Test public void supportsExplicitUnitPriceAndNumberFormats() {
        InventoryChatInterpreter.Result r = parse("nhập 1.000 NS001, đơn giá 5.000đ");
        assertTrue(r.ready()); assertEquals(1000, r.draft.quantity, 0); assertEquals(5000, r.draft.unitPrice, 0);
        r = parse("nhập 1,5 NS001 giá 5k"); assertTrue(r.ready()); assertEquals(1.5, r.draft.quantity, 0); assertEquals(5000, r.draft.unitPrice, 0);
    }
    @Test public void supportsUserDefinedUnits() {
        List<Product> products = Collections.singletonList(product(1, "S001", "Sách", "quyển"));
        InventoryChatInterpreter.Result r = InventoryChatInterpreter.interpret("Tôi đã nhập 5 quyển sách", products, null);
        assertTrue(r.ready()); assertEquals(5, r.draft.quantity, 0);
    }
    @Test public void numbersInsideProductNamesAreNotQuantities() {
        InventoryChatInterpreter.Result r = parse("nhập 5 Bút 2B"); assertTrue(r.ready()); assertEquals(5, r.draft.quantity, 0);
        assertFalse(parse("nhập Bút 2B").ready());
    }
    @Test public void numericSkuNeverStealsQuantityFromAnotherProduct() {
        List<Product> products = Arrays.asList(catalog.get(0), product(3, "3", "Bút", "cái"));
        InventoryChatInterpreter.Result r = InventoryChatInterpreter.interpret("xuất 3 chai NS001", products, null);
        assertFalse(r.ready()); assertEquals(1, r.product.id); assertEquals(3, r.draft.quantity, 0);
        r = InventoryChatInterpreter.interpret("xuất NS001", products, null);
        r = InventoryChatInterpreter.interpret("3", products, r.draft);
        assertFalse(r.ready()); assertEquals(1, r.product.id); assertEquals(3, r.draft.quantity, 0);
        r = InventoryChatInterpreter.interpret("nhập 2 mã 3", products, null);
        assertTrue(r.ready()); assertEquals(3, r.product.id); assertEquals(2, r.draft.quantity, 0);
    }
    @Test public void questionsNegationAndHypotheticalsNeverWrite() {
        for (String text : new String[]{"Nếu xuất 5 NS001 thì còn bao nhiêu?", "Tôi chưa xuất 5 NS001", "Không nhập 10 NS001", "Có nên nhập 10 NS001", "Ví dụ nhập 5 NS001", "Tôi sẽ xuất 5 NS001", "hướng dẫn xuất 5 NS001"})
            assertFalse(text, parse(text).ready());
    }
    @Test public void asksAndCompletesMissingQuantityOrProduct() {
        InventoryChatInterpreter.Result r = parse("xuất NS001"); assertFalse(r.ready()); assertNotNull(r.draft);
        r = InventoryChatInterpreter.interpret("3 chai", catalog, r.draft); assertFalse(r.ready()); assertEquals(3, r.draft.quantity, 0);
        r = InventoryChatInterpreter.interpret("giá 5000", catalog, r.draft); assertTrue(r.ready()); assertEquals(3, r.draft.quantity, 0);
        r = parse("nhập 20 chai"); assertFalse(r.ready()); assertNotNull(r.draft);
        r = InventoryChatInterpreter.interpret("NS001", catalog, r.draft); assertTrue(r.ready()); assertEquals(20, r.draft.quantity, 0);
    }
    @Test public void unknownProductFollowupDoesNotUseOldProduct() {
        InventoryChatInterpreter.Result r = parse("xuất NS001");
        r = InventoryChatInterpreter.interpret("5 chai nước cam", catalog, r.draft); assertFalse(r.ready()); assertEquals(0, r.draft.productId);
    }
    @Test public void rejectsAmbiguityAndMultipleOperations() {
        assertFalse(parse("nhập 5 NS001 và 3 BUT02").ready());
        assertFalse(parse("nhập 5 NS001 rồi xuất 2 NS001").ready());
        List<Product> duplicate = Arrays.asList(product(1, "A", "Nước suối", "chai"), product(2, "B", "Nước suối", "chai"));
        InventoryChatInterpreter.Result r = InventoryChatInterpreter.interpret("nhập 5 nước suối", duplicate, null);
        assertFalse(r.ready());
        r = InventoryChatInterpreter.interpret("B", duplicate, r.draft); assertTrue(r.ready()); assertEquals(2, r.product.id);
    }
    @Test public void rejectsInvalidNumbersUnitsAndDates() {
        for (String text : new String[]{"xuất -5 NS001", "nhập 0 NS001", "nhập 5 thùng NS001", "nhập 5 NS001 hôm qua", "nhập 5 NS001 ngày 01/10/2026", "nhập 5 NS001 giá -1000"})
            assertFalse(text, parse(text).ready());
    }
    @Test public void cancellationAndNewInstructionDiscardPendingDraft() {
        InventoryChatInterpreter.Result pending = parse("xuất NS001");
        assertNull(InventoryChatInterpreter.interpret("Hủy", catalog, pending.draft).draft);
        InventoryChatInterpreter.Result next = InventoryChatInterpreter.interpret("nhập 5 BUT02", catalog, pending.draft);
        assertTrue(next.ready()); assertEquals(2, next.product.id); assertEquals(Transaction.TYPE_INCOME, next.draft.type);
    }
    @Test public void issueWaitsForPriceAndPreservesQuantity() {
        for (String price : new String[]{"5000", "5.000đ", "giá 5000", "đơn giá 5k", "5000đ/chai"}) {
            InventoryChatInterpreter.Result r = parse("xuất 3 NS001"); assertFalse(r.ready()); assertTrue(r.reply.contains("đơn giá"));
            r = InventoryChatInterpreter.interpret(price, catalog, r.draft);
            assertTrue(price, r.ready()); assertEquals(3, r.draft.quantity, 0); assertEquals(5000, r.draft.unitPrice, 0);
        }
        assertTrue(parse("xuất 3 NS001 đơn giá 7000").ready());
    }
    @Test public void invalidPriceDoesNotDropPendingIssueAndCanCancel() {
        InventoryChatInterpreter.Result r = parse("xuất 3 NS001");
        r = InventoryChatInterpreter.interpret("-1000", catalog, r.draft); assertFalse(r.ready()); assertNotNull(r.draft);
        r = InventoryChatInterpreter.interpret("5 chai", catalog, r.draft); assertFalse(r.ready()); assertEquals(3, r.draft.quantity, 0);
        assertNull(InventoryChatInterpreter.interpret("Hủy", catalog, r.draft).draft);
        r = InventoryChatInterpreter.interpret("0", catalog, r.draft); assertTrue(r.ready()); assertEquals(0, r.draft.unitPrice, 0);
    }
}
