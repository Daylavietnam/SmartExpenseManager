package com.group14.expensemanager.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    public static final int TYPE_AI = 0;
    public static final int TYPE_USER = 1;

    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String content;
    private int senderType;
    private long timestamp;

    public ChatMessage(String content, int senderType) {
        this.content = content;
        this.senderType = senderType;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSenderType() {
        return senderType;
    }

    public void setSenderType(int senderType) {
        this.senderType = senderType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
