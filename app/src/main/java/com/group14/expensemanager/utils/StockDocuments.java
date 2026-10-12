package com.group14.expensemanager.utils;

import com.group14.expensemanager.model.Transaction;
import java.util.*;

/** Lines share a stable document id; legacy rows still represent one document each. */
public final class StockDocuments {
    public static String key(Transaction line) {
        return line.getDocumentId() == null || line.getDocumentId().isEmpty() ? "legacy-" + line.getId() : line.getDocumentId();
    }
    public static List<List<Transaction>> group(List<Transaction> lines) {
        Map<String, List<Transaction>> groups = new LinkedHashMap<>();
        for (Transaction line : lines) groups.computeIfAbsent(key(line), id -> new ArrayList<>()).add(line);
        return new ArrayList<>(groups.values());
    }
    public static double total(List<Transaction> lines) {
        double total = 0; for (Transaction line : lines) total += line.getAmount(); return total;
    }
    public static String number(Transaction line) {
        String key = key(line);
        return (line.isIncome() ? "NK-" : "XK-") + (key.startsWith("legacy-") ? key.substring(7) : key.substring(0, Math.min(8, key.length()))).toUpperCase(Locale.ROOT);
    }
}
