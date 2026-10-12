package com.group14.expensemanager.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "transactions")
public class Transaction implements Serializable {
    public static final String TYPE_INCOME = "income";
    public static final String TYPE_EXPENSE = "expense";

    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String category;
    private double amount;
    private String type;
    private long timestamp;
    private int productId;
    private double quantity;
    private double unitPrice;
    private String unit = "cái";
    private String sku = "";
    @androidx.room.ColumnInfo(defaultValue = "''")
    private String documentId = "";
    @androidx.room.ColumnInfo(defaultValue = "0")
    private int partnerId;
    @androidx.room.ColumnInfo(defaultValue = "''")
    private String partnerName = "";
    @androidx.room.ColumnInfo(defaultValue = "''")
    private String partnerPhone = "";
    @androidx.room.ColumnInfo(defaultValue = "''")
    private String note = "";

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String value) { documentId = value; }
    public int getPartnerId() { return partnerId; }
    public void setPartnerId(int value) { partnerId = value; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String value) { partnerName = value; }
    public String getPartnerPhone() { return partnerPhone; }
    public void setPartnerPhone(String value) { partnerPhone = value; }
    public String getNote() { return note; }
    public void setNote(String value) { note = value; }

    public int getProductId() { return productId; }
    public void setProductId(int value) { productId = value; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double value) { quantity = value; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double value) { unitPrice = value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }
    public String getSku() { return sku; }
    public void setSku(String value) { sku = value; }

    public Transaction(String title, String category, double amount, String type, long timestamp) {
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isIncome() {
        return TYPE_INCOME.equals(type);
    }
}
