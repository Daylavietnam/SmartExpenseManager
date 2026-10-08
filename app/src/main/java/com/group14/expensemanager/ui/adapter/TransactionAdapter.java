package com.group14.expensemanager.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.group14.expensemanager.R;
import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.utils.AppAssets;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<Transaction> transactions = new ArrayList<>();
    private final OnTransactionActionListener listener;

    public interface OnTransactionActionListener {
        void onEdit(Transaction transaction);
        void onDelete(Transaction transaction);
    }

    public TransactionAdapter(OnTransactionActionListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction t = transactions.get(position);
        holder.bind(t, listener);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView mtvTitle, mtvCategory, mtvAmount, mtvDate, mtvTypeIcon;
        private final Button mbtnEdit, mbtnDelete;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault());
        private final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            mtvTitle = itemView.findViewById(R.id.mtv_transaction_title);
            mtvCategory = itemView.findViewById(R.id.mtv_category);
            mtvAmount = itemView.findViewById(R.id.mtv_transaction_amount);
            mtvDate = itemView.findViewById(R.id.mtv_transaction_date);
            mtvTypeIcon = itemView.findViewById(R.id.mtv_type_icon);
            mbtnEdit = itemView.findViewById(R.id.mbtn_edit);
            mbtnDelete = itemView.findViewById(R.id.mbtn_delete);
        }

        public void bind(Transaction t, OnTransactionActionListener listener) {
            mtvTitle.setText(t.getSku() + " — " + t.getTitle());
            mtvCategory.setText(t.getCategory());
            mtvDate.setText(sdf.format(new Date(t.getTimestamp())));

            // Sử dụng màu sắc và icon từ AppAssets
            int categoryColor = ContextCompat.getColor(itemView.getContext(), R.color.ui_accent);
            mtvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(categoryColor));

            if (t.isIncome()) {
                mtvAmount.setText("Nhập " + formatter.format(t.getQuantity()) + " " + t.getUnit()
                        + "\n" + formatter.format(t.getAmount()) + "đ • " + formatter.format(t.getUnitPrice()) + "đ/" + t.getUnit());
                mtvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
                mtvTypeIcon.setText("↑");
                mtvTypeIcon.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
            } else {
                mtvAmount.setText("Xuất " + formatter.format(t.getQuantity()) + " " + t.getUnit()
                        + "\n" + formatter.format(t.getAmount()) + "đ • " + formatter.format(t.getUnitPrice()) + "đ/" + t.getUnit());
                mtvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
                mtvTypeIcon.setText("↓");
                mtvTypeIcon.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
            }

            mbtnEdit.setOnClickListener(v -> listener.onEdit(t));
            mbtnDelete.setOnClickListener(v -> listener.onDelete(t));
        }
    }
}
