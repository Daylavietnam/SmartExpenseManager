package com.group14.expensemanager.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.group14.expensemanager.model.ChatMessage;
import com.group14.expensemanager.model.Transaction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Transaction.class, ChatMessage.class, com.group14.expensemanager.model.Product.class, com.group14.expensemanager.model.Partner.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
    public abstract ChatDao chatDao();
    public abstract ProductDao productDao();
    public abstract PartnerDao partnerDao();

    public static final androidx.room.migration.Migration MIGRATION_1_2 = new androidx.room.migration.Migration(1, 2) {
        @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS partners (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, phone TEXT, address TEXT, type TEXT)");
            db.execSQL("ALTER TABLE transactions ADD COLUMN documentId TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE transactions ADD COLUMN partnerId INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE transactions ADD COLUMN partnerName TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE transactions ADD COLUMN partnerPhone TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE transactions ADD COLUMN note TEXT DEFAULT ''");
            db.execSQL("UPDATE transactions SET documentId = 'legacy-' || id");
        }
    };

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "smart_stock_db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
