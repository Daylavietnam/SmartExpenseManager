package com.group14.expensemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.data.TransactionRepository;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final MediatorLiveData<SummaryData> summaryData = new MediatorLiveData<>();
    private final MediatorLiveData<String> expenseAnalysis = new MediatorLiveData<>();
    private final MediatorLiveData<String> incomeAnalysis = new MediatorLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.repository = TransactionRepository.getInstance(application);

        summaryData.addSource(repository.getTransactions(), transactions -> {
            summaryData.setValue(calculateSummary(transactions));
        });

        expenseAnalysis.addSource(summaryData, this::updateExpenseAnalysis);
        incomeAnalysis.addSource(summaryData, this::updateIncomeAnalysis);

    }

    public LiveData<SummaryData> getSummaryData() {
        return summaryData;
    }

    public LiveData<String> getExpenseAnalysis() {
        return expenseAnalysis;
    }

    public LiveData<String> getIncomeAnalysis() {
        return incomeAnalysis;
    }

    private void updateExpenseAnalysis(SummaryData data) {
        expenseAnalysis.setValue(generateAnalysis(data, false));
    }

    private void updateIncomeAnalysis(SummaryData data) {
        incomeAnalysis.setValue(generateAnalysis(data, true));
    }

    private String generateAnalysis(SummaryData data, boolean isIncome) {
        if (data == null) return null;
        double total = isIncome ? data.income : data.expense;
        Map<String, Double> categoryData = isIncome ? data.categoryIncomes : data.categoryExpenses;

        if (total <= 0 || categoryData == null || categoryData.isEmpty()) {
            return null;
        }

        StringBuilder htmlBuilder = new StringBuilder();
        String maxCat = "", minCat = "";
        double maxVal = -1, minVal = Double.MAX_VALUE;

        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            String cat = entry.getKey();
            double amount = entry.getValue();
            double percent = (amount / total) * 100;

            htmlBuilder.append("&#8226; <b>").append(cat).append("</b> chiếm ")
                    .append("<b>")
                    .append(String.format(Locale.forLanguageTag("vi-VN"), "%.0f%%", percent))
                    .append("</b> ").append(isIncome ? "giá trị nhập kho" : "giá trị xuất kho").append(" của tháng<br/>");

            if (amount > maxVal) { maxVal = amount; maxCat = cat; }
            if (amount < minVal) { minVal = amount; minCat = cat; }
        }

        if (categoryData.size() > 1) {
            htmlBuilder.append("<br/>&#9642; ").append(isIncome ? "Nhóm nhập thấp nhất" : "Xuất kho ít nhất").append(": <b>").append(minCat).append("</b>");
            htmlBuilder.append("<br/>&#9642; ").append(isIncome ? "Nhóm nhập chính" : "Xuất kho nhiều nhất").append(": <b>").append(maxCat).append("</b>");
        } else {
            htmlBuilder.append("<br/>&#9642; ").append(isIncome ? "Nhóm nhập chính" : "Xuất kho nhiều nhất").append(": <b>").append(maxCat).append("</b>");
        }
        return htmlBuilder.toString();
    }

    private SummaryData calculateSummary(List<Transaction> transactions) {
        Calendar currentCalendar = Calendar.getInstance();
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);

        double income = 0;
        double expense = 0;
        Map<String, Double> categoryExpenses = new HashMap<>();
        Map<String, Double> categoryIncomes = new HashMap<>();

        if (transactions != null) for (Transaction t : transactions) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(t.getTimestamp());
            if (date.get(Calendar.MONTH) == currentMonth && date.get(Calendar.YEAR) == currentYear) {
                if (t.isIncome()) {
                    income += t.getAmount();
                    String cat = t.getCategory();
                    categoryIncomes.put(cat, categoryIncomes.getOrDefault(cat, 0.0) + t.getAmount());
                } else {
                    expense += t.getAmount();
                    String cat = t.getCategory();
                    categoryExpenses.put(cat, categoryExpenses.getOrDefault(cat, 0.0) + t.getAmount());
                }
            }
        }
        return new SummaryData(income, expense, currentMonth, currentYear, categoryExpenses, categoryIncomes);
    }

    public static class SummaryData {
        public final double income;
        public final double expense;
        public final double total;
        public final int month;
        public final int year;
        public final Map<String, Double> categoryExpenses;
        public final Map<String, Double> categoryIncomes;

        public SummaryData(double income, double expense, int month, int year, Map<String, Double> categoryExpenses, Map<String, Double> categoryIncomes) {
            this.income = income;
            this.expense = expense;
            this.total = income - expense;
            this.month = month;
            this.year = year;
            this.categoryExpenses = categoryExpenses;
            this.categoryIncomes = categoryIncomes;
        }
    }

}
