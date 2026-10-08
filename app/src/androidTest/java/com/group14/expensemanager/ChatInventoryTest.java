package com.group14.expensemanager;

import android.app.Application;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.group14.expensemanager.data.AppDatabase;
import com.group14.expensemanager.model.*;
import com.group14.expensemanager.ui.activity.AiChatActivity;
import com.group14.expensemanager.viewmodel.AiChatViewModel;
import com.group14.expensemanager.utils.StockCalculator;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class ChatInventoryTest {
    interface Check { boolean done(); }
    private void await(Check check) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        while (!check.done() && System.currentTimeMillis() < deadline) Thread.sleep(30);
        assertTrue("Chat action timed out", check.done());
    }
    private void send(ActivityScenario<AiChatActivity> screen, String text) throws Exception {
        onView(withId(R.id.tiet_message)).perform(replaceText(text));
        closeSoftKeyboard();
        onView(withId(R.id.mbtn_send)).perform(click());
        await(() -> {
            AtomicReference<Boolean> loading = new AtomicReference<>(true);
            screen.onActivity(activity -> loading.set(Boolean.TRUE.equals(new ViewModelProvider(activity)
                    .get(AiChatViewModel.class).getIsLoading().getValue())));
            return !loading.get();
        });
    }
    private int count(AppDatabase db, int id) {
        int count = 0; for (Transaction t : db.transactionDao().getTransactionsForExport()) if (t.getProductId() == id) count++;
        return count;
    }
    @Test public void chatCreatesRealMovementsAsksAndRejectsOverIssue() throws Exception {
        Application app = (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(app);
        Product product = new Product(); product.sku = "CHATTEST" + System.nanoTime(); product.name = product.sku;
        product.referencePrice = 5000; product.unit = "chai";
        product.id = (int) db.productDao().insert(product);
        try (ActivityScenario<AiChatActivity> screen = ActivityScenario.launch(AiChatActivity.class)) {
            await(() -> {
                AtomicReference<Boolean> loaded = new AtomicReference<>(false);
                screen.onActivity(activity -> {
                    java.util.List<Product> products = com.group14.expensemanager.data.TransactionRepository.getInstance(app).getProducts().getValue();
                    if (products != null) for (Product p : products) if (p.id == product.id) loaded.set(true);
                });
                return loaded.get();
            });
            send(screen, "Tôi đã thêm 10 chai " + product.sku);
            await(() -> count(db, product.id) == 1);
            assertEquals(10, StockCalculator.stock(product.id, db.transactionDao().getTransactionsForExport()), 0);
            send(screen, "Tôi đã suất 3 chai " + product.sku);
            assertEquals(1, count(db, product.id));
            assertEquals(10, StockCalculator.stock(product.id, db.transactionDao().getTransactionsForExport()), 0);
            assertTrue(chatLog(db).contains("đơn giá bao nhiêu"));
            screen.recreate();
            send(screen, "giá 6000");
            assertEquals(chatLog(db), 7, StockCalculator.stock(product.id, db.transactionDao().getTransactionsForExport()), 0);
            send(screen, "xuất 100 " + product.sku + " đơn giá 6000");
            assertEquals(2, count(db, product.id));
            assertEquals(7, StockCalculator.stock(product.id, db.transactionDao().getTransactionsForExport()), 0);
            send(screen, "nhập " + product.sku);
            assertEquals(2, count(db, product.id));
            send(screen, "2 chai " + product.sku);
            assertEquals(9, StockCalculator.stock(product.id, db.transactionDao().getTransactionsForExport()), 0);
            send(screen, "nhập 1 " + product.sku + " đơn giá 7000đ");
            assertEquals(10, StockCalculator.stock(product.id, db.transactionDao().getTransactionsForExport()), 0);
            boolean defaultPrice = false, explicitPrice = false;
            for (Transaction t : db.transactionDao().getTransactionsForExport()) if (t.getProductId() == product.id) {
                defaultPrice |= t.getUnitPrice() == 5000; explicitPrice |= t.getUnitPrice() == 7000;
            }
            assertTrue(defaultPrice); assertTrue(explicitPrice);
        } finally {
            db.runInTransaction(() -> {
                for (Transaction t : db.transactionDao().getTransactionsForExport()) if (t.getProductId() == product.id) db.transactionDao().delete(t);
                db.productDao().delete(product);
                db.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM chat_messages WHERE content LIKE ?", new Object[]{"%" + product.sku + "%"});
            });
        }
    }

    private String chatLog(AppDatabase db) {
        StringBuilder log = new StringBuilder();
        try (android.database.Cursor cursor = db.getOpenHelper().getReadableDatabase().query("SELECT content FROM chat_messages ORDER BY id DESC LIMIT 5")) {
            while (cursor.moveToNext()) log.append(cursor.getString(0)).append("\n");
        }
        return log.toString();
    }
}
