package com.group14.expensemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.group14.expensemanager.data.TransactionRepository;
import com.group14.expensemanager.model.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DetailViewModel extends AndroidViewModel {
    public enum FilterType {DAY, WEEK, MONTH, ALL}

    private final TransactionRepository repository;
    private final MutableLiveData<FilterType> currentFilter = new MutableLiveData<>(FilterType.ALL);
    private final MutableLiveData<Calendar> filterDate = new MutableLiveData<>(Calendar.getInstance());
    private final MediatorLiveData<List<Transaction>> filteredTransactions = new MediatorLiveData<>();
    private final MediatorLiveData<SummaryData> summaryData = new MediatorLiveData<>();

    public DetailViewModel(@NonNull Application application) {
        super(application);
        this.repository = TransactionRepository.getInstance(application);

        filteredTransactions.addSource(repository.getTransactions(), transactions -> {
            applyFilter(transactions, currentFilter.getValue(), filterDate.getValue());
        });

        filteredTransactions.addSource(currentFilter, filter -> {
            applyFilter(repository.getTransactions().getValue(), filter, filterDate.getValue());
        });

        filteredTransactions.addSource(filterDate, date -> {
            applyFilter(repository.getTransactions().getValue(), currentFilter.getValue(), date);
        });

        summaryData.addSource(filteredTransactions, transactions -> {
            summaryData.setValue(calculateSummary(transactions));
        });
    }

    public LiveData<List<Transaction>> getFilteredTransactions() {
        return filteredTransactions;
    }

    public LiveData<SummaryData> getSummaryData() {
        return summaryData;
    }

    public void setFilter(FilterType filter) {
        currentFilter.setValue(filter);
    }

    public void setFilterDate(Calendar date) {
        filterDate.setValue(date);
    }

    private void applyFilter(List<Transaction> all, FilterType filter, Calendar targetDate) {
        if (all == null) return;
        List<Transaction> result = new ArrayList<>();
        Calendar dateToUse = (targetDate != null) ? targetDate : Calendar.getInstance();

        for (Transaction t : all) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(t.getTimestamp());

            boolean match = false;
            switch (filter) {
                case DAY: match = isSameDay(dateToUse, date); break;
                case WEEK: match = isSameWeek(dateToUse, date); break;
                case MONTH: match = isSameMonth(dateToUse, date); break;
                case ALL: match = true; break;
            }
            if (match) result.add(t);
        }
        result.sort((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));
        filteredTransactions.setValue(result);
    }

    private SummaryData calculateSummary(List<Transaction> transactions) {
        double income = 0, expense = 0;
        if (transactions != null) {
            for (Transaction t : transactions) {
                if (t.isIncome()) income += t.getAmount();
                else expense += t.getAmount();
            }
        }
        return new SummaryData(income, expense);
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameWeek(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR);
    }

    private boolean isSameMonth(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
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
