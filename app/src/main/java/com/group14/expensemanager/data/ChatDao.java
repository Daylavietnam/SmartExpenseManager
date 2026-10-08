package com.group14.expensemanager.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.group14.expensemanager.model.ChatMessage;

import java.util.List;

@Dao
public interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, senderType DESC, id ASC")
    LiveData<List<ChatMessage>> getAllMessages();

    @Insert
    void insert(ChatMessage message);

    @Query("DELETE FROM chat_messages")
    void deleteAll();
}
