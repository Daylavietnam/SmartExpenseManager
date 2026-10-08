package com.group14.expensemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.group14.expensemanager.data.TransactionRepository;
import com.group14.expensemanager.model.Transaction;

public class AddViewModel extends AndroidViewModel {
    private final TransactionRepository repository;

    public AddViewModel(@NonNull Application application) {
        super(application);
        this.repository = TransactionRepository.getInstance(application);
    }

    public void saveMovement(Transaction movement, TransactionRepository.WriteCallback callback) {
        repository.saveMovement(movement, callback);
    }
}