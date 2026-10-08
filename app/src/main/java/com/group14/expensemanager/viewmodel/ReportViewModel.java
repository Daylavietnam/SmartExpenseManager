package com.group14.expensemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.group14.expensemanager.data.TransactionRepository;
import com.group14.expensemanager.model.Transaction;

import com.group14.expensemanager.utils.AiManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReportViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final MutableLiveData<Calendar> selectedMonth = new MutableLiveData<>(Calendar.getInstance());
    private final MutableLiveData<Boolean> showExpense = new MutableLiveData<>(true);
    private final MediatorLiveData<List<Transaction>> reportTransactions = new MediatorLiveData<>();
    private final MediatorLiveData<SummaryData> monthlySummary = new MediatorLiveData<>();
    private final MediatorLiveData<String> aiAnalysis = new MediatorLiveData<>();
    private final MediatorLiveData<List<CashFlowPoint>> cashFlowData = new MediatorLiveData<>();

    public ReportViewModel(@NonNull Application application) {
        super(application);
        this.repository = TransactionRepository.getInstance(application);
        reportTransactions.addSource(repository.getTransactions(), transactions -> updateReportData());
        reportTransactions.addSource(selectedMonth, month -> updateReportData());
        reportTransactions.addSource(showExpense, expense -> updateReportData());
        monthlySummary.addSource(repository.getTransactions(), transactions -> updateSummaryData());
        monthlySummary.addSource(selectedMonth, month -> updateSummaryData());
        
        aiAnalysis.addSource(monthlySummary, this::performAiAnalysis);

        cashFlowData.addSource(repository.getTransactions(), transactions -> updateCashFlowPoints());
        cashFlowData.addSource(selectedMonth, month -> updateCashFlowPoints());
    }

    public LiveData<List<Transaction>> getReportTransactions() { return reportTransactions; }
    public LiveData<SummaryData> getMonthlySummary() { return monthlySummary; }
    public LiveData<String> getAiAnalysis() { return aiAnalysis; }
    public LiveData<List<CashFlowPoint>> getCashFlowData() { return cashFlowData; }
    public LiveData<Boolean> isShowExpense() { return showExpense; }
    public void setSelectedMonth(Calendar calendar) { selectedMonth.setValue(calendar); }
    public LiveData<Calendar> getSelectedMonth() { return selectedMonth; }
    public List<Transaction> getAllTransactions() { return repository.getTransactions().getValue(); }
    public void setShowExpense(boolean expense) { showExpense.setValue(expense); }

    public void nextMonth() {
        Calendar current = selectedMonth.getValue();
        if (current != null) {
            Calendar next = (Calendar) current.clone();
            next.add(Calendar.MONTH, 1);
            selectedMonth.setValue(next);
        }
    }

    public void prevMonth() {
        Calendar current = selectedMonth.getValue();
        if (current != null) {
            Calendar prev = (Calendar) current.clone();
            prev.add(Calendar.MONTH, -1);
            selectedMonth.setValue(prev);
        }
    }

    private void updateReportData() {
        List<Transaction> all = repository.getTransactions().getValue();
        Calendar month = selectedMonth.getValue();
        Boolean expense = showExpense.getValue();
        if (all == null || month == null || expense == null) return;
        List<Transaction> result = new ArrayList<>();
        int targetMonth = month.get(Calendar.MONTH);
        int targetYear = month.get(Calendar.YEAR);
        for (Transaction t : all) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(t.getTimestamp());
            if (date.get(Calendar.MONTH) == targetMonth && date.get(Calendar.YEAR) == targetYear) {
                if (expense != t.isIncome()) result.add(t);
            }
        }
        reportTransactions.setValue(result);
    }

    private void updateSummaryData() {
        List<Transaction> all = repository.getTransactions().getValue();
        Calendar month = selectedMonth.getValue();
        if (all == null || month == null) return;
        double income = 0, expense = 0;
        int targetMonth = month.get(Calendar.MONTH);
        int targetYear = month.get(Calendar.YEAR);
        for (Transaction t : all) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(t.getTimestamp());
            if (date.get(Calendar.MONTH) == targetMonth && date.get(Calendar.YEAR) == targetYear) {
                if (t.isIncome()) income += t.getAmount();
                else expense += t.getAmount();
            }
        }
        monthlySummary.setValue(new SummaryData(income, expense));
    }

    private void performAiAnalysis(SummaryData data) {
        if (data == null || (data.income == 0 && data.expense == 0)) {
            aiAnalysis.setValue("Chưa có phiếu kho trong tháng."); return;
        }
        aiAnalysis.setValue("Giá trị nhập tháng này: " + String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,.0fđ", data.income)
                + "\nGiá trị xuất tháng này: " + String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,.0fđ", data.expense)
                + "\nXem Sản phẩm & tồn kho để kiểm tra số lượng cần bổ sung. Chênh lệch giá trị phiếu không phải lợi nhuận hay giá trị tồn kho.");
    }
    private void updateCashFlowPoints() {
        List<Transaction> all = repository.getTransactions().getValue();
        if (all == null) return;
        cashFlowData.setValue(new ArrayList<>()); 
    }

    public static class CashFlowPoint {
        public final long timestamp;
        public final double balance;
        public CashFlowPoint(long timestamp, double balance) {
            this.timestamp = timestamp;
            this.balance = balance;
        }
    }

    public static class SummaryData {
        public final double income;
        public final double expense;
        public final double total;
        public SummaryData(double income, double expense) {
            this.income = income;
            this.expense = expense;
            this.total = income - expense;
        }
    }
}
