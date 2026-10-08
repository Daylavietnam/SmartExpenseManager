package com.group14.expensemanager.ui.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.group14.expensemanager.R;
import com.group14.expensemanager.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> messages = new ArrayList<>();

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardMessage;
        private final TextView tvSender, tvMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMessage = itemView.findViewById(R.id.cardMessage);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        public void bind(ChatMessage message) {
            tvMessage.setText(message.getContent());
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cardMessage.getLayoutParams();
            float density = itemView.getResources().getDisplayMetrics().density;
            params.width = Math.min((int) (360 * density), itemView.getResources().getDisplayMetrics().widthPixels - (int) (64 * density));

            if (message.getSenderType() == ChatMessage.TYPE_USER) {
                tvSender.setText("BẠN");
                params.gravity = Gravity.END;
                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.ui_hero));
                tvMessage.setTextColor(android.graphics.Color.WHITE);
                tvSender.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.ui_hero_secondary));
            } else {
                tvSender.setText("TRỢ LÝ KHO");
                params.gravity = Gravity.START;
                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.ui_surface));
                tvMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.ui_text));
                tvSender.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.ui_primary));
            }
            cardMessage.setLayoutParams(params);
        }
    }
}
