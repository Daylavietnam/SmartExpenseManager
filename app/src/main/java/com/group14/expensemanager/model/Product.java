package com.group14.expensemanager.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "products", indices = {@Index(value = "sku", unique = true)})
public class Product {
    @PrimaryKey(autoGenerate = true) public int id;
    @NonNull public String sku = "";
    @NonNull public String name = "";
    @NonNull public String unit = "cái";
    @NonNull public String category = "Khác";
    public double referencePrice;
    public double minimumStock;

    @Override public String toString() { return sku + " — " + name; }
}
