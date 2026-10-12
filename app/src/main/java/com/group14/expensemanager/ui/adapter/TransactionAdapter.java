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
    private List<List<Transaction>> documents = new ArrayList<>();
    private final OnTransactionActionListener listener;

    public interface OnTransactionActionListener {
        void onEdit(Transaction transaction);
        void onDelete(Transaction transaction);
    }

    public TransactionAdapter(OnTransactionActionListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.documents = com.group14.expensemanager.utils.StockDocuments.group(transactions);
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
        holder.bind(documents.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView mtvTitle, mtvCategory, mtvAmount, mtvDate, mtvTypeIcon, mtvLines;
        private final Button mbtnEdit, mbtnDelete;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault());
        private final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            mtvTitle = itemView.findViewById(R.id.mtv_transaction_title);
            mtvLines = itemView.findViewById(R.id.mtv_document_lines);
            mtvCategory = itemView.findViewById(R.id.mtv_category);
            mtvAmount = itemView.findViewById(R.id.mtv_transaction_amount);
            mtvDate = itemView.findViewById(R.id.mtv_transaction_date);
            mtvTypeIcon = itemView.findViewById(R.id.mtv_type_icon);
            mbtnEdit = itemView.findViewById(R.id.mbtn_edit);
            mbtnDelete = itemView.findViewById(R.id.mbtn_delete);
        }

        public void bind(List<Transaction> lines, OnTransactionActionListener listener) {
            Transaction t = lines.get(0);
            mtvTitle.setText((t.isIncome() ? "Phiếu nhập " : "Phiếu xuất ") + com.group14.expensemanager.utils.StockDocuments.number(t));
            mtvCategory.setText(lines.size() + " dòng hàng");
            mtvDate.setText(sdf.format(new Date(t.getTimestamp())));
            StringBuilder body = new StringBuilder();
            body.append(t.isIncome() ? "Nhà cung cấp: " : "Khách hàng: ").append(t.getPartnerName().isEmpty() ? "Chưa chọn" : t.getPartnerName());
            if (!t.getPartnerPhone().isEmpty()) body.append(" • ").append(t.getPartnerPhone());
            for (Transaction line : lines) body.append("\n\n").append(line.getSku()).append(" — ").append(line.getTitle())
                    .append("\n").append(formatter.format(line.getQuantity())).append(" ").append(line.getUnit())
                    .append(" × ").append(formatter.format(line.getUnitPrice())).append("đ = ").append(formatter.format(line.getAmount())).append("đ");
            if (!t.getNote().isEmpty()) body.append("\n\nGhi chú: ").append(t.getNote());
            mtvLines.setText(body.toString());

            // Sử dụng màu sắc và icon từ AppAssets
            int categoryColor = ContextCompat.getColor(itemView.getContext(), R.color.ui_accent);
            mtvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(categoryColor));

            if (t.isIncome()) {
                mtvAmount.setText("Tổng tiền: " + formatter.format(com.group14.expensemanager.utils.StockDocuments.total(lines)) + "đ");
                mtvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
                mtvTypeIcon.setText("↑");
                mtvTypeIcon.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
            } else {
                mtvAmount.setText("Tổng tiền: " + formatter.format(com.group14.expensemanager.utils.StockDocuments.total(lines)) + "đ");
                mtvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
                mtvTypeIcon.setText("↓");
                mtvTypeIcon.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
            }

            mbtnEdit.setOnClickListener(v -> listener.onEdit(t));
            mbtnDelete.setOnClickListener(v -> listener.onDelete(t));
        }
    }
}
