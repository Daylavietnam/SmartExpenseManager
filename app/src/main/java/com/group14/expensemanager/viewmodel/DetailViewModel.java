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
    private final MutableLiveData<String> search = new MutableLiveData<>("");
    private final MutableLiveData<Integer> partner = new MutableLiveData<>(0);
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
        filteredTransactions.addSource(search, query -> applyFilter(repository.getTransactions().getValue(), currentFilter.getValue(), filterDate.getValue()));
        filteredTransactions.addSource(partner, id -> applyFilter(repository.getTransactions().getValue(), currentFilter.getValue(), filterDate.getValue()));

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
    public void setSearch(String query) { search.setValue(query); }
    public void setPartner(int id) { partner.setValue(id); }

    private String normalized(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).replace('đ', 'd');
    }

    private void applyFilter(List<Transaction> all, FilterType filter, Calendar targetDate) {
        if (all == null) return;
        List<Transaction> result = new ArrayList<>();
        Calendar dateToUse = (targetDate != null) ? targetDate : Calendar.getInstance();

        String query = normalized(search.getValue()).trim();
        for (List<Transaction> document : com.group14.expensemanager.utils.StockDocuments.group(all)) {
            Transaction t = document.get(0);
            if (partner.getValue() != null && partner.getValue() != 0 && t.getPartnerId() != partner.getValue()) continue;
            StringBuilder terms = new StringBuilder(com.group14.expensemanager.utils.StockDocuments.number(t))
                    .append(' ').append(t.getPartnerName()).append(' ').append(t.getPartnerPhone()).append(' ').append(t.getNote());
            for (Transaction line : document) terms.append(' ').append(line.getSku()).append(' ').append(line.getTitle());
            if (!normalized(terms.toString()).contains(query)) continue;
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(t.getTimestamp());

            boolean match = false;
            switch (filter) {
                case DAY: match = isSameDay(dateToUse, date); break;
                case WEEK: match = isSameWeek(dateToUse, date); break;
                case MONTH: match = isSameMonth(dateToUse, date); break;
                case ALL: match = true; break;
            }
            if (match) result.addAll(document);
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
