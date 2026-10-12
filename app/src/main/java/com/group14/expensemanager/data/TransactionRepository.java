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
    private final LiveData<List<com.group14.expensemanager.model.Partner>> allPartners;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<List<ChatMessage>> allMessages;

    private TransactionRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        database = db;
        productDao = db.productDao();
        allProducts = productDao.observeAll();
        allPartners = db.partnerDao().observeAll();
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
    public interface DocumentCallback { void onLoaded(List<Transaction> lines); }
    public void loadDocument(String key, DocumentCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Transaction> lines = new java.util.ArrayList<>();
            for (Transaction t : transactionDao.getTransactionsForExport())
                if (key.equals(com.group14.expensemanager.utils.StockDocuments.key(t))) lines.add(t);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onLoaded(lines));
        });
    }

    public LiveData<List<com.group14.expensemanager.model.Partner>> getPartners() { return allPartners; }

    public void savePartner(com.group14.expensemanager.model.Partner partner, WriteCallback callback) {
        write(() -> {
            partner.name = partner.name.trim(); partner.phone = partner.phone.trim(); partner.address = partner.address.trim();
            if (partner.name.isEmpty() || (!com.group14.expensemanager.model.Partner.CUSTOMER.equals(partner.type)
                    && !com.group14.expensemanager.model.Partner.SUPPLIER.equals(partner.type)))
                throw new IllegalArgumentException("Vui lòng nhập tên và loại đối tác hợp lệ.");
            if (partner.id == 0) database.partnerDao().insert(partner);
            else {
                com.group14.expensemanager.model.Partner old = database.partnerDao().find(partner.id);
                if (old == null) throw new IllegalArgumentException("Đối tác không còn tồn tại.");
                if (!old.type.equals(partner.type) && database.partnerDao().movementCount(partner.id) > 0)
                    throw new IllegalArgumentException("Không thể đổi loại đối tác đã có phiếu kho.");
                database.partnerDao().update(partner);
            }
        }, callback);
    }

    public void deletePartner(com.group14.expensemanager.model.Partner partner, WriteCallback callback) {
        write(() -> {
            if (database.partnerDao().movementCount(partner.id) > 0)
                throw new IllegalArgumentException("Đối tác đã có phiếu kho, không thể xóa.");
            database.partnerDao().delete(partner);
        }, callback);
    }

    /** Replace all lines as one transaction, validating the final history before writing any line. */
    public void saveDocument(String existingKey, String type, long date, int partnerId, String note,
                             List<Transaction> lines, WriteCallback callback) {
        write(() -> {
            if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("Phiếu cần ít nhất một sản phẩm.");
            com.group14.expensemanager.model.Partner partner = partnerId == 0 ? null : database.partnerDao().find(partnerId);
            if (partnerId != 0 && partner == null) throw new IllegalArgumentException("Đối tác không còn tồn tại.");
            String expected = Transaction.TYPE_INCOME.equals(type) ? com.group14.expensemanager.model.Partner.SUPPLIER : com.group14.expensemanager.model.Partner.CUSTOMER;
            if (partner != null && !expected.equals(partner.type)) throw new IllegalArgumentException("Nhập kho chọn nhà cung cấp, xuất kho chọn khách hàng.");
            List<Transaction> history = new java.util.ArrayList<>(transactionDao.getTransactionsForExport());
            List<Transaction> old = new java.util.ArrayList<>();
            if (existingKey != null) {
                for (Transaction t : history) if (existingKey.equals(com.group14.expensemanager.utils.StockDocuments.key(t))) old.add(t);
                if (old.isEmpty()) throw new IllegalArgumentException("Phiếu không còn tồn tại.");
                history.removeAll(old);
            }
            String key = existingKey == null ? java.util.UUID.randomUUID().toString() : existingKey;
            double total = 0;
            for (Transaction line : lines) {
                line.setId(0); line.setDocumentId(key); line.setType(type); line.setTimestamp(date);
                line.setPartnerId(partnerId); line.setPartnerName(partner == null ? "" : partner.name);
                line.setPartnerPhone(partner == null ? "" : partner.phone); line.setNote(note == null ? "" : note.trim());
                prepareMovement(line); total += line.getAmount(); history.add(line);
            }
            if (!Double.isFinite(total)) throw new IllegalArgumentException("Tổng tiền phiếu quá lớn.");
            if (!com.group14.expensemanager.utils.StockCalculator.validHistory(history))
                throw new IllegalArgumentException("Không đủ tồn kho tại ngày xuất. Kiểm tra toàn bộ sản phẩm trên phiếu.");
            for (Transaction t : old) transactionDao.delete(t);
            for (Transaction line : lines) transactionDao.insert(line);
        }, callback);
    }

    public void deleteDocument(Transaction selected, WriteCallback callback) {
        write(() -> {
            List<Transaction> history = new java.util.ArrayList<>(transactionDao.getTransactionsForExport());
            List<Transaction> removed = new java.util.ArrayList<>();
            String key = com.group14.expensemanager.utils.StockDocuments.key(selected);
            for (Transaction t : history) if (key.equals(com.group14.expensemanager.utils.StockDocuments.key(t))) removed.add(t);
            if (removed.isEmpty()) throw new IllegalArgumentException("Phiếu không còn tồn tại.");
            history.removeAll(removed);
            if (!com.group14.expensemanager.utils.StockCalculator.validHistory(history))
                throw new IllegalArgumentException("Không thể xóa phiếu nhập đang được dùng cho các phiếu xuất.");
            for (Transaction t : removed) transactionDao.delete(t);
        }, callback);
    }

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
            com.group14.expensemanager.model.Partner partner = database.partnerDao().find(movement.getPartnerId());
            String partnerType = movement.isIncome() ? com.group14.expensemanager.model.Partner.SUPPLIER : com.group14.expensemanager.model.Partner.CUSTOMER;
            if (partner == null || !partnerType.equals(partner.type))
                throw new IllegalArgumentException(movement.isIncome() ? "Cần chọn nhà cung cấp có trong danh bạ." : "Cần chọn khách hàng có trong danh bạ.");
            movement.setPartnerName(partner.name); movement.setPartnerPhone(partner.phone);
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
                    + "\n" + (movement.isIncome() ? "Nhà cung cấp: " : "Khách hàng: ") + partner.name
                    + (partner.phone.isEmpty() ? "." : " • " + partner.phone + ".")
                    + "\nPhiếu đã được lưu trong Lịch sử. Có thể sửa hoặc xóa ở đó.";
            chatDao.insert(new ChatMessage(reply, ChatMessage.TYPE_AI));
        }, callback);
    }

    private void persistMovement(Transaction movement) {
            List<Transaction> history = new java.util.ArrayList<>(transactionDao.getTransactionsForExport());
            if (movement.getId() != 0) {
                Transaction existing = null;
                for (Transaction t : history) if (t.getId() == movement.getId()) existing = t;
                if (existing == null) throw new IllegalArgumentException("Phiếu không còn tồn tại.");
                String key = com.group14.expensemanager.utils.StockDocuments.key(existing);
                int count = 0; for (Transaction t : history) if (key.equals(com.group14.expensemanager.utils.StockDocuments.key(t))) count++;
                if (count > 1) throw new IllegalArgumentException("Vui lòng sửa toàn bộ phiếu nhiều sản phẩm.");
                movement.setDocumentId(key); movement.setPartnerId(existing.getPartnerId());
                movement.setPartnerName(existing.getPartnerName()); movement.setPartnerPhone(existing.getPartnerPhone()); movement.setNote(existing.getNote());
                history.remove(existing);
            } else movement.setDocumentId(java.util.UUID.randomUUID().toString());
            prepareMovement(movement);
            history.add(movement);
            if (!com.group14.expensemanager.utils.StockCalculator.validHistory(history))
                throw new IllegalArgumentException("Không đủ tồn kho tại ngày xuất. Kiểm tra số lượng và ngày nhập–xuất.");
            if (movement.getId() == 0) transactionDao.insert(movement);
            else transactionDao.update(movement);
    }

    private void prepareMovement(Transaction movement) {
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
            if (movement.getPartnerId() != 0) {
                com.group14.expensemanager.model.Partner partner = database.partnerDao().find(movement.getPartnerId());
                String expected = movement.isIncome() ? com.group14.expensemanager.model.Partner.SUPPLIER : com.group14.expensemanager.model.Partner.CUSTOMER;
                if (partner == null || !expected.equals(partner.type)) throw new IllegalArgumentException("Loại đối tác không phù hợp với phiếu kho.");
            }
            movement.setTitle(product.name);
            movement.setSku(product.sku);
            movement.setUnit(product.unit);
            movement.setCategory(product.category);
            movement.setAmount(movement.getQuantity() * movement.getUnitPrice());
    }

    public void deleteMovement(Transaction movement, WriteCallback callback) {
        deleteDocument(movement, callback);
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
