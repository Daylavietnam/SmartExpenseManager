package com.group14.expensemanager.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "partners")
public class Partner implements Serializable {
    public static final String CUSTOMER = "customer";
    public static final String SUPPLIER = "supplier";
    @PrimaryKey(autoGenerate = true) public int id;
    public String name = "";
    public String phone = "";
    public String address = "";
    public String type = CUSTOMER;
    @Override public String toString() { return name + (phone.isEmpty() ? "" : " • " + phone); }
}
