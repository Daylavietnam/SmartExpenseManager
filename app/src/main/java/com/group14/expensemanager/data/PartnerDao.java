package com.group14.expensemanager.data;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.group14.expensemanager.model.Partner;
import java.util.List;

@Dao
public interface PartnerDao {
    @Query("SELECT * FROM partners ORDER BY name COLLATE NOCASE") LiveData<List<Partner>> observeAll();
    @Query("SELECT * FROM partners WHERE id = :id") Partner find(int id);
    @Insert long insert(Partner partner);
    @Update void update(Partner partner);
    @Delete void delete(Partner partner);
    @Query("SELECT COUNT(*) FROM transactions WHERE partnerId = :id") int movementCount(int id);
}
