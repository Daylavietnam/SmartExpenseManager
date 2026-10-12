package com.group14.expensemanager;

import android.app.Application;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.group14.expensemanager.data.*;
import com.group14.expensemanager.model.*;
import com.group14.expensemanager.ui.activity.*;
import com.group14.expensemanager.utils.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
public class StockDocumentTest {
    interface Write { void run(TransactionRepository.WriteCallback callback); }
    private String write(Write operation) throws Exception {
        CountDownLatch latch=new CountDownLatch(1);AtomicReference<String> error=new AtomicReference<>();
        operation.run(result->{error.set(result);latch.countDown();});
        assertTrue(latch.await(10,TimeUnit.SECONDS));return error.get();
    }
    private Transaction line(Product product,double quantity,double price) {
        Transaction t=new Transaction("","",0,Transaction.TYPE_INCOME,System.currentTimeMillis());t.setProductId(product.id);t.setQuantity(quantity);t.setUnitPrice(price);return t;
    }
    private List<Transaction> lines(AppDatabase db,String sku) {
        List<Transaction> result=new ArrayList<>();for(Transaction t:db.transactionDao().getTransactionsForExport())if(t.getSku().startsWith(sku))result.add(t);return result;
    }
    @Test public void documentsSaveEditDeleteAtomicallyAndEditorRestoresAllLines() throws Exception {
        Application app=(Application)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        AppDatabase db=AppDatabase.getDatabase(app);TransactionRepository repo=TransactionRepository.getInstance(app);
        String marker="DOCTEST"+System.nanoTime();Product a=new Product(),b=new Product();a.sku=marker+"A";a.name="Bánh thử";a.referencePrice=1000;b.sku=marker+"B";b.name="Nước thử";b.referencePrice=2000;
        a.id=(int)db.productDao().insert(a);b.id=(int)db.productDao().insert(b);
        Partner supplier=new Partner();supplier.name=marker+" NCC";supplier.phone="0900000000";supplier.type=Partner.SUPPLIER;supplier.id=(int)db.partnerDao().insert(supplier);
        Partner customer=new Partner();customer.name=marker+" KH";customer.type=Partner.CUSTOMER;customer.id=(int)db.partnerDao().insert(customer);
        long date=System.currentTimeMillis()-86400000L;
        try {
            assertNull(write(cb->repo.saveDocument(null,Transaction.TYPE_INCOME,date,supplier.id,"Giao buổi sáng",Arrays.asList(line(a,10,1000),line(b,20,2000)),cb)));
            List<Transaction> initial=lines(db,marker);assertEquals(2,initial.size());assertEquals(1,StockDocuments.group(initial).size());assertEquals(50000,StockDocuments.total(initial),0);
            Transaction receipt=initial.get(0);String receiptKey=StockDocuments.key(receipt);
            assertEquals(supplier.name,receipt.getPartnerName());assertEquals("Giao buổi sáng",receipt.getNote());
            String snapshot=receipt.getPartnerName();supplier.name=marker+" NCC sửa tên";
            assertNull(write(cb->repo.savePartner(supplier,cb)));assertEquals(snapshot,lines(db,marker).get(0).getPartnerName());
            supplier.type=Partner.CUSTOMER;assertNotNull(write(cb->repo.savePartner(supplier,cb)));supplier.type=Partner.SUPPLIER;
            assertNotNull(write(cb->repo.deletePartner(supplier,cb)));
            try(ActivityScenario<AddActivity> create=ActivityScenario.launch(AddActivity.class)) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                onView(withId(R.id.stock_add_line)).perform(scrollTo(),click());
                create.onActivity(activity->{
                    List<android.view.View> products=views(activity.findViewById(R.id.stock_document_fields),R.id.stock_product);
                    chooseProduct((android.widget.Spinner)products.get(0),a.id);chooseProduct((android.widget.Spinner)products.get(1),b.id);
                    android.widget.Spinner partners=activity.findViewById(R.id.stock_partner);
                    for(int i=0;i<partners.getCount();i++)if(partners.getItemAtPosition(i).toString().contains(supplier.name))partners.setSelection(i);
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                create.onActivity(activity->{
                    List<android.view.View> quantities=views(activity.findViewById(R.id.stock_document_fields),R.id.stock_quantity);
                    List<android.view.View> prices=views(activity.findViewById(R.id.stock_document_fields),R.id.stock_price);
                    ((android.widget.EditText)quantities.get(0)).setText("2");((android.widget.EditText)quantities.get(1)).setText("3");
                    ((android.widget.EditText)prices.get(0)).setText("100");((android.widget.EditText)prices.get(1)).setText("200");
                    ((android.widget.EditText)activity.findViewById(R.id.stock_note)).setText(marker+" UI create");
                });
                onView(withId(R.id.mbtn_save)).perform(scrollTo(),click());
                long deadline=System.currentTimeMillis()+5000;while(lines(db,marker).size()!=4&&System.currentTimeMillis()<deadline)Thread.sleep(30);
                assertEquals(4,lines(db,marker).size());
                Transaction created=null;for(Transaction t:lines(db,marker))if(t.getNote().equals(marker+" UI create"))created=t;
                assertNotNull(created);assertEquals(supplier.id,created.getPartnerId());
                Transaction saved=created;assertNull(write(cb->repo.deleteDocument(saved,cb)));
            }
            assertNotNull(write(cb->repo.saveDocument(null,Transaction.TYPE_EXPENSE,date,customer.id,"",Arrays.asList(line(a,2,1500),line(b,21,3000)),cb)));
            assertEquals(2,lines(db,marker).size());assertEquals(10,StockCalculator.stock(a.id,lines(db,marker)),0);
            assertNotNull(write(cb->repo.saveDocument(null,Transaction.TYPE_EXPENSE,date,supplier.id,"",Collections.singletonList(line(a,1,1500)),cb)));
            assertNull(write(cb->repo.saveDocument(null,Transaction.TYPE_EXPENSE,date,customer.id,"Khách lấy tại kho",Arrays.asList(line(a,2,1500),line(b,3,3000)),cb)));
            List<Transaction> all=lines(db,marker);assertEquals(4,all.size());assertEquals(8,StockCalculator.stock(a.id,all),0);assertEquals(17,StockCalculator.stock(b.id,all),0);
            assertNotNull(write(cb->repo.deleteDocument(receipt,cb)));
            assertNotNull(write(cb->repo.saveDocument(receiptKey,Transaction.TYPE_INCOME,date,supplier.id,"Sửa lỗi",Arrays.asList(line(a,1,1000),line(b,20,2000)),cb)));
            assertEquals(4,lines(db,marker).size());
            assertNull(write(cb->repo.saveDocument(receiptKey,Transaction.TYPE_INCOME,date,supplier.id,"Đã sửa",Arrays.asList(line(a,12,1000),line(b,20,2000)),cb)));
            assertEquals(10,StockCalculator.stock(a.id,lines(db,marker)),0);
            Transaction issue=null;for(Transaction t:lines(db,marker))if(!t.isIncome()){issue=t;break;}assertNotNull(issue);
            Transaction outbound=issue;
            try(ActivityScenario<AddActivity> screen=ActivityScenario.launch(new Intent(app,AddActivity.class).putExtra("TRANSACTION",outbound))) {
                long deadline=System.currentTimeMillis()+5000;AtomicReference<Integer> count=new AtomicReference<>(0);
                do { screen.onActivity(activity->{android.widget.LinearLayout fields=activity.findViewById(R.id.stock_document_fields);count.set(countViews(fields,R.id.stock_quantity));});if(count.get()!=2)Thread.sleep(30); } while(count.get()!=2&&System.currentTimeMillis()<deadline);
                assertEquals(2,(int)count.get());
                screen.onActivity(activity->{
                    List<android.view.View> quantities=views(activity.findViewById(R.id.stock_document_fields),R.id.stock_quantity);
                    List<android.view.View> prices=views(activity.findViewById(R.id.stock_document_fields),R.id.stock_price);
                    ((android.widget.EditText)quantities.get(0)).setText("3");((android.widget.EditText)quantities.get(1)).setText("4");
                    ((android.widget.EditText)prices.get(0)).setText("1111");((android.widget.EditText)prices.get(1)).setText("2222");
                    ((android.widget.EditText)activity.findViewById(R.id.stock_note)).setText("Sửa trên giao diện");
                });
                screen.recreate();InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                screen.onActivity(activity->{
                    assertEquals(2,countViews(activity.findViewById(R.id.stock_document_fields),R.id.stock_quantity));
                    assertEquals(customer.name,((android.widget.Spinner)activity.findViewById(R.id.stock_partner)).getSelectedItem().toString());
                    assertEquals("Sửa trên giao diện",((android.widget.EditText)activity.findViewById(R.id.stock_note)).getText().toString());
                    List<android.view.View> quantities=views(activity.findViewById(R.id.stock_document_fields),R.id.stock_quantity);
                    List<android.view.View> prices=views(activity.findViewById(R.id.stock_document_fields),R.id.stock_price);
                    assertEquals("3",((android.widget.EditText)quantities.get(0)).getText().toString());assertEquals("4",((android.widget.EditText)quantities.get(1)).getText().toString());
                    assertEquals("1111",((android.widget.EditText)prices.get(0)).getText().toString());assertEquals("2222",((android.widget.EditText)prices.get(1)).getText().toString());
                    assertTrue(((android.widget.TextView)activity.findViewById(R.id.stock_total)).getText().toString().contains("12.221"));
                });
                if ("true".equals(InstrumentationRegistry.getArguments().getString("screenshots"))) {
                    onView(withText("Dòng 1")).perform(scrollTo()); screenshot("document-editor");
                }
                onView(withId(R.id.mbtn_save)).perform(scrollTo(),click());
                long savedDeadline=System.currentTimeMillis()+5000;
                while(StockCalculator.stock(a.id,lines(db,marker))!=9&&System.currentTimeMillis()<savedDeadline)Thread.sleep(30);
                assertEquals(9,StockCalculator.stock(a.id,lines(db,marker)),0);
                assertEquals(16,StockCalculator.stock(b.id,lines(db,marker)),0);
            }
            try (ActivityScenario<DetailActivity> history=ActivityScenario.launch(DetailActivity.class)) {
                onView(withId(R.id.stock_search_documents)).perform(replaceText(marker));androidx.test.espresso.Espresso.closeSoftKeyboard();
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                history.onActivity(activity->{
                    androidx.recyclerview.widget.RecyclerView list=activity.findViewById(R.id.rv_transactions);
                    assertEquals(2,list.getAdapter().getItemCount());
                });
                screenshot("document-history");
            }
            try (ActivityScenario<PartnerActivity> contacts=ActivityScenario.launch(PartnerActivity.class)) { screenshot("document-partners"); }
            assertNull(write(cb->repo.deleteDocument(outbound,cb)));assertEquals(2,lines(db,marker).size());
            assertNull(write(cb->repo.deletePartner(customer,cb)));
            Transaction newest=lines(db,marker).get(0);assertNull(write(cb->repo.deleteDocument(newest,cb)));assertEquals(0,lines(db,marker).size());
        } finally {
            db.runInTransaction(()->{for(Transaction t:lines(db,marker))db.transactionDao().delete(t);db.productDao().delete(a);db.productDao().delete(b);db.partnerDao().delete(supplier);db.partnerDao().delete(customer);});
        }
    }
    private int countViews(android.view.View v,int id) {
        int count=v.getId()==id?1:0;if(v instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)v;for(int i=0;i<group.getChildCount();i++)count+=countViews(group.getChildAt(i),id);}return count;
    }
    private List<android.view.View> views(android.view.View v,int id) {
        List<android.view.View> result=new ArrayList<>();if(v.getId()==id)result.add(v);
        if(v instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)v;for(int i=0;i<group.getChildCount();i++)result.addAll(views(group.getChildAt(i),id));}return result;
    }
    private void chooseProduct(android.widget.Spinner spinner,int id) {
        for(int i=0;i<spinner.getCount();i++)if(((Product)spinner.getItemAtPosition(i)).id==id){spinner.setSelection(i);return;}
        fail("Product missing from picker");
    }
    private void screenshot(String name) throws Exception {
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("screenshots"))) return;
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(500);
        android.graphics.Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        java.io.File file=new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),name+".png");
        try (java.io.FileOutputStream stream=new java.io.FileOutputStream(file)) { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream); } bitmap.recycle();
    }
    @Test public void migrationKeepsOldStockAndChats() {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="migration-test-"+System.nanoTime();
        android.database.sqlite.SQLiteDatabase old=context.openOrCreateDatabase(name,0,null);
        old.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT, category TEXT, amount REAL NOT NULL, type TEXT, timestamp INTEGER NOT NULL, productId INTEGER NOT NULL, quantity REAL NOT NULL, unitPrice REAL NOT NULL, unit TEXT, sku TEXT)");
        old.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sku TEXT NOT NULL, name TEXT NOT NULL, unit TEXT NOT NULL, category TEXT NOT NULL, referencePrice REAL NOT NULL, minimumStock REAL NOT NULL)");
        old.execSQL("CREATE UNIQUE INDEX index_products_sku ON products(sku)");
        old.execSQL("CREATE TABLE chat_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, content TEXT, senderType INTEGER NOT NULL, timestamp INTEGER NOT NULL)");
        old.execSQL("INSERT INTO products VALUES(1,'OLD','Hàng cũ','cái','Khác',5000,1)");
        old.execSQL("INSERT INTO transactions VALUES(1,'Hàng cũ','Khác',50000,'income',1,1,10,5000,'cái','OLD')");
        old.execSQL("INSERT INTO chat_messages VALUES(1,'Tin nhắn cũ',1,1)");old.setVersion(1);old.close();
        AppDatabase migrated=androidx.room.Room.databaseBuilder(context,AppDatabase.class,name).addMigrations(AppDatabase.MIGRATION_1_2).build();
        try {
            List<Transaction> history=migrated.transactionDao().getTransactionsForExport();assertEquals(1,history.size());assertEquals("legacy-1",history.get(0).getDocumentId());assertEquals(10,StockCalculator.stock(1,history),0);
            try(android.database.Cursor cursor=migrated.getOpenHelper().getReadableDatabase().query("SELECT content FROM chat_messages")){assertTrue(cursor.moveToFirst());assertEquals("Tin nhắn cũ",cursor.getString(0));}
            assertEquals(0,history.get(0).getPartnerId());
        } finally {migrated.close();context.deleteDatabase(name);}
    }
}
