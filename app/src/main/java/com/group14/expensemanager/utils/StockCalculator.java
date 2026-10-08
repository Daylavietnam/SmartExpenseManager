package com.group14.expensemanager.utils;

import com.group14.expensemanager.model.Transaction;
import java.util.*;

/** Inventory always uses all movements, independently of report date filters. */
public final class StockCalculator {
    private StockCalculator() {}
    public static double stock(int productId, List<Transaction> movements) {
        double result = 0;
        if (movements != null) for (Transaction t : movements) {
            if (t.getProductId() == productId) result += t.isIncome() ? t.getQuantity() : -t.getQuantity();
        }
        return Math.abs(result) < 1e-8 ? 0 : result;
    }

    /** Reject edits/deletions/backdated issues that make any product negative at any date. */
    public static boolean validHistory(List<Transaction> movements) {
        List<Transaction> ordered = new ArrayList<>(movements);
        ordered.sort(Comparator.comparingLong(StockCalculator::day)
                .thenComparingInt(t -> t.isIncome() ? 0 : 1)
                .thenComparingInt(Transaction::getId));
        Map<Integer, Double> balances = new HashMap<>();
        for (Transaction t : ordered) {
            if (!Double.isFinite(t.getQuantity()) || t.getQuantity() <= 0) return false;
            double balance = balances.getOrDefault(t.getProductId(), 0d)
                    + (t.isIncome() ? t.getQuantity() : -t.getQuantity());
            if (balance < -1e-8) return false;
            balances.put(t.getProductId(), balance);
        }
        return true;
    }

    private static long day(Transaction movement) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(movement.getTimestamp());
        date.set(Calendar.HOUR_OF_DAY, 0); date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0); date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }
}
