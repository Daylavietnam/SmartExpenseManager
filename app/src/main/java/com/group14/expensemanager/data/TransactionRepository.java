package com.group14.expensemanager.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.group14.expensemanager.model.ChatMessage;
import com.group14.expensemanager.model.Transaction;

import java.util.List;

public class TransactionRepository {
    private static TransactionRepository instance;
    private final TransactionDao transactionDao;
    private final ChatDao chatDao;
    private final AppDatabase database;
    private final ProductDao productDao;
    private final LiveData<List<com.group14.expensemanager.model.Product>> allProducts;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<List<ChatMessage>> allMessages;

    private TransactionRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        database = db;
        productDao = db.productDao();
        allProducts = productDao.observeAll();
        transactionDao = db.transactionDao();
        chatDao = db.chatDao();
        allTransactions = transactionDao.getAllTransactions();
        allMessages = chatDao.getAllMessages();
    }

    public static synchronized TransactionRepository getInstance(Application application) {
        if (instance == null) {
            instance = new TransactionRepository(application);
        }
        return instance;
    }

    public LiveData<List<Transaction>> getTransactions() {
        return allTransactions;
    }

    public LiveData<List<com.group14.expensemanager.model.Product>> getProducts() {
        return allProducts;
    }

    public interface WriteCallback { void onComplete(String error); }

    private void write(Runnable action, WriteCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String error = null;
            try { database.runInTransaction(action); }
            catch (IllegalArgumentException e) { error = e.getMessage(); }
            catch (Exception e) { error = "Không thể lưu. Kiểm tra mã hàng trùng hoặc thử lại."; }
            String result = error;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onComplete(result));
        });
    }

    public void saveProduct(com.group14.expensemanager.model.Product product, WriteCallback callback) {
        write(() -> {
            if (product.sku.trim().isEmpty() || product.name.trim().isEmpty() || product.unit.trim().isEmpty()
                    || !Double.isFinite(product.referencePrice) || product.referencePrice < 0
                    || !Double.isFinite(product.minimumStock) || product.minimumStock < 0)
                throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ.");
            if (product.id == 0) productDao.insert(product);
            else {
                com.group14.expensemanager.model.Product old = productDao.find(product.id);
                if (old == null) throw new IllegalArgumentException("Sản phẩm không còn tồn tại.");
                if (!old.unit.equals(product.unit) && productDao.movementCount(product.id) > 0)
                    throw new IllegalArgumentException("Không thể đổi đơn vị khi sản phẩm đã có phiếu kho.");
                productDao.update(product);
            }
        }, callback);
    }

    public void deleteProduct(com.group14.expensemanager.model.Product product, WriteCallback callback) {
        write(() -> {
            if (productDao.movementCount(product.id) > 0)
                throw new IllegalArgumentException("Sản phẩm đã có phiếu kho, không thể xóa.");
            productDao.delete(product);
        }, callback);
    }

    public void saveMovement(Transaction movement, WriteCallback callback) {
        write(() -> persistMovement(movement), callback);
    }

    /** The movement and its success reply commit together. No AI response can assert an uncommitted write. */
    public void saveChatMovement(Transaction movement, boolean referencePrice, WriteCallback callback) {
        write(() -> {
            if (!movement.isIncome() && referencePrice)
                throw new IllegalArgumentException("Cần nhập đơn giá xuất trước khi lưu phiếu.");
            if (referencePrice) {
                com.group14.expensemanager.model.Product current = productDao.find(movement.getProductId());
                if (current == null) throw new IllegalArgumentException("Sản phẩm không còn tồn tại.");
                movement.setUnitPrice(current.referencePrice);
            }
            persistMovement(movement);
            double stock = com.group14.expensemanager.utils.StockCalculator.stock(movement.getProductId(),
                    transactionDao.getTransactionsForExport());
            java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"));
            String reply = "Đã " + (movement.isIncome() ? "nhập " : "xuất ") + format.format(movement.getQuantity())
                    + " " + movement.getUnit() + " " + movement.getTitle() + " (" + movement.getSku() + ")."
                    + "\nTồn hiện tại: " + format.format(stock) + " " + movement.getUnit() + "."
                    + "\nĐơn giá: " + format.format(movement.getUnitPrice()) + "đ"
                    + (referencePrice ? " (giá tham chiếu của sản phẩm)." : ".")
                    + "\nPhiếu đã được lưu trong Lịch sử. Có thể sửa hoặc xóa ở đó.";
            chatDao.insert(new ChatMessage(reply, ChatMessage.TYPE_AI));
        }, callback);
    }

    private void persistMovement(Transaction movement) {
            com.group14.expensemanager.model.Product product = productDao.find(movement.getProductId());
            if (product == null) throw new IllegalArgumentException("Vui lòng chọn sản phẩm có trong kho.");
            if (!Double.isFinite(movement.getQuantity()) || movement.getQuantity() <= 0
                    || !Double.isFinite(movement.getUnitPrice()) || movement.getUnitPrice() < 0
                    || !Double.isFinite(movement.getQuantity() * movement.getUnitPrice()))
                throw new IllegalArgumentException("Số lượng hoặc đơn giá không hợp lệ.");
            if (!Transaction.TYPE_INCOME.equals(movement.getType()) && !Transaction.TYPE_EXPENSE.equals(movement.getType()))
                throw new IllegalArgumentException("Loại phiếu không hợp lệ.");
            if (movement.getTimestamp() > System.currentTimeMillis())
                throw new IllegalArgumentException("Ngày phiếu không được ở tương lai.");
            List<Transaction> history = new java.util.ArrayList<>(transactionDao.getTransactionsForExport());
            if (movement.getId() != 0 && !history.removeIf(t -> t.getId() == movement.getId()))
                throw new IllegalArgumentException("Phiếu không còn tồn tại.");
            history.add(movement);
            if (!com.group14.expensemanager.utils.StockCalculator.validHistory(history))
                throw new IllegalArgumentException("Không đủ tồn kho tại ngày xuất. Kiểm tra số lượng và ngày nhập–xuất.");
            movement.setTitle(product.name);
            movement.setSku(product.sku);
            movement.setUnit(product.unit);
            movement.setCategory(product.category);
            movement.setAmount(movement.getQuantity() * movement.getUnitPrice());
            if (movement.getId() == 0) transactionDao.insert(movement);
            else transactionDao.update(movement);
    }

    public void deleteMovement(Transaction movement, WriteCallback callback) {
        write(() -> {
            List<Transaction> history = new java.util.ArrayList<>(transactionDao.getTransactionsForExport());
            history.removeIf(t -> t.getId() == movement.getId());
            if (!com.group14.expensemanager.utils.StockCalculator.validHistory(history))
                throw new IllegalArgumentException("Không thể xóa phiếu nhập đang được dùng cho các phiếu xuất.");
            transactionDao.delete(movement);
        }, callback);
    }

    public LiveData<List<ChatMessage>> getChatMessages() {
        return allMessages;
    }

    // Movement history for export
    public void getTransactionsForExport(
            ExportCallback callback
    ){

        AppDatabase.databaseWriteExecutor.execute(() -> {

            List<Transaction> transactions =
                    transactionDao.getTransactionsForExport();


            callback.onResult(transactions);

        });

    }



    public interface ExportCallback {

        void onResult(List<Transaction> transactions);

    }

    public void addChatMessage(ChatMessage message) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            chatDao.insert(message);
        });
    }

    public void clearChatHistory() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            chatDao.deleteAll();
        });
    }
}
