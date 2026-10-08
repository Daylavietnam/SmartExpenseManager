package com.group14.expensemanager;

import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.utils.StockCalculator;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class StockCalculatorTest {
    private Transaction movement(int product, boolean receipt, double quantity, long date) {
        Transaction t = new Transaction("Hàng", "Khác", 0,
                receipt ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE, date * 86400000L);
        t.setProductId(product); t.setQuantity(quantity); return t;
    }
    @Test public void separatesProductsAndIncludesOlderReceipts() {
        List<Transaction> history = Arrays.asList(movement(1, true, 10, 1), movement(2, true, 20, 2), movement(1, false, 3, 100));
        assertEquals(7, StockCalculator.stock(1, history), 0.00001);
        assertEquals(20, StockCalculator.stock(2, history), 0.00001);
        assertTrue(StockCalculator.validHistory(history));
    }
    @Test public void rejectsOverIssueAndBackdatedIssue() {
        assertFalse(StockCalculator.validHistory(Arrays.asList(movement(1, true, 5, 1), movement(1, false, 6, 2))));
        assertFalse(StockCalculator.validHistory(Arrays.asList(movement(1, true, 10, 2), movement(1, false, 1, 1))));
    }
    @Test public void deletingOrReducingReceiptCannotInvalidatePastIssues() {
        Transaction receipt = movement(1, true, 10, 1), issue = movement(1, false, 8, 2);
        assertTrue(StockCalculator.validHistory(Arrays.asList(receipt, issue)));
        assertFalse(StockCalculator.validHistory(Collections.singletonList(issue)));
        receipt.setQuantity(7);
        assertFalse(StockCalculator.validHistory(Arrays.asList(receipt, issue)));
    }
    @Test public void allowsExactDepletionAndFractionalUnits() {
        List<Transaction> history = Arrays.asList(movement(1, true, 0.3, 1), movement(1, false, 0.1, 2), movement(1, false, 0.2, 3));
        assertTrue(StockCalculator.validHistory(history)); assertEquals(0, StockCalculator.stock(1, history), 0);
    }
    @Test public void sameDateReceiptPrecedesIssue() {
        assertTrue(StockCalculator.validHistory(Arrays.asList(movement(1, false, 1, 1), movement(1, true, 1, 1))));
        Transaction receipt = movement(1, true, 2, 10), issue = movement(1, false, 1, 10);
        receipt.setTimestamp(receipt.getTimestamp() + 1000);
        assertTrue(StockCalculator.validHistory(Arrays.asList(issue, receipt)));
    }
    @Test public void rejectsInvalidQuantity() {
        for (double quantity : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY})
            assertFalse(StockCalculator.validHistory(Collections.singletonList(movement(1, true, quantity, 1))));
    }
}
