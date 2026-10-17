package com.group14.expensemanager.data;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.group14.expensemanager.model.Product;
import java.util.List;

@Dao
public interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    LiveData<List<Product>> observeAll();
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    List<Product> getAllForExport();
    @Query("SELECT * FROM products WHERE id = :id") Product find(int id);
    @Insert long insert(Product product);
    @Update void update(Product product);
    @Delete void delete(Product product);
    @Query("SELECT COUNT(*) FROM transactions WHERE productId = :id") int movementCount(int id);
}
