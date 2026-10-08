package com.group14.expensemanager;

import android.app.Application;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.group14.expensemanager.data.*;
import com.group14.expensemanager.model.*;
import com.group14.expensemanager.ui.activity.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class InventoryWorkflowTest {
    interface Operation { void run(TransactionRepository.WriteCallback callback); }
    private String write(Operation op) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        op.run(error -> { result.set(error); done.countDown(); });
        assertTrue("Database callback timed out", done.await(10, TimeUnit.SECONDS));
        return result.get();
    }
    private Transaction movement(int product, boolean receipt, double quantity, long timestamp) {
        Transaction t = new Transaction("", "", 0, receipt ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE, timestamp);
        t.setProductId(product); t.setQuantity(quantity); t.setUnitPrice(10000); return t;
    }

    @Test public void repositoryEnforcesInventoryAndScreensOpen() throws Exception {
        Application app = (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        TransactionRepository repo = TransactionRepository.getInstance(app);
        AppDatabase db = AppDatabase.getDatabase(app);
        Product p = new Product(); p.sku = "TEST-" + System.nanoTime(); p.name = "Kiểm tra kho";
        p.referencePrice = 10000; p.minimumStock = 2;
        assertNull(write(cb -> repo.saveProduct(p, cb)));
        // The repository intentionally does not mutate the editor's Product id.
        Product persisted = null;
        androidx.lifecycle.LiveData<java.util.List<Product>> data = repo.getProducts();
        androidx.lifecycle.Observer<java.util.List<Product>> observer = products -> {};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> data.observeForever(observer));
        try {
            long deadline = System.currentTimeMillis() + 5000;
            while (persisted == null && System.currentTimeMillis() < deadline) {
                java.util.List<Product> products = data.getValue();
                if (products != null) for (Product product : products) if (product.sku.equals(p.sku)) persisted = product;
                if (persisted == null) Thread.sleep(25);
            }
            assertNotNull(persisted);
            int id = persisted.id;
            long date = System.currentTimeMillis() - 3 * 86400000L;
            Transaction receipt = movement(id, true, 10, date);
            assertNull(write(cb -> repo.saveMovement(receipt, cb)));
            assertNotNull(write(cb -> repo.saveMovement(movement(id, false, 11, date + 86400000L), cb)));
            assertNotNull(write(cb -> repo.saveMovement(movement(id, false, 1, date - 86400000L), cb)));
            assertNull(write(cb -> repo.saveMovement(movement(id, false, 4, date + 86400000L), cb)));
            Transaction savedReceipt = null;
            for (Transaction t : db.transactionDao().getTransactionsForExport()) if (t.getProductId() == id && t.isIncome()) savedReceipt = t;
            assertNotNull(savedReceipt);
            Transaction existing = savedReceipt;
            assertNotNull(write(cb -> repo.deleteMovement(existing, cb)));
            existing.setQuantity(3);
            assertNotNull(write(cb -> repo.saveMovement(existing, cb)));
            assertNotNull(write(cb -> repo.deleteProduct(db.productDao().find(id), cb)));
            assertEquals(6, com.group14.expensemanager.utils.StockCalculator.stock(id, db.transactionDao().getTransactionsForExport()), 0.001);

            try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
                onView(withId(R.id.mtv_total_label)).check(matches(withText("Tồn kho (giá tham chiếu):")));
            }

            try (ActivityScenario<ProductActivity> screen = ActivityScenario.launch(ProductActivity.class)) {
                onView(withText("Sản phẩm & tồn kho")).check(matches(isDisplayed()));
            }
            try (ActivityScenario<AddActivity> screen = ActivityScenario.launch(AddActivity.class)) {
                onView(withId(R.id.mbtn_income)).check(matches(isDisplayed()));
            }
            try (ActivityScenario<ReportActivity> screen = ActivityScenario.launch(ReportActivity.class)) {
                onView(withId(R.id.pc_report)).check(matches(isDisplayed()));
            }
        } finally {
            // Remove only this test's data, preserving existing user products and movements.
            db.runInTransaction(() -> {
                for (Transaction t : db.transactionDao().getTransactionsForExport())
                    if (p.sku.equals(t.getSku())) db.transactionDao().delete(t);
                java.util.List<Product> products = data.getValue();
                if (products != null) for (Product product : products) if (p.sku.equals(product.sku)) db.productDao().delete(product);
            });
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> data.removeObserver(observer));
        }
    }
}
