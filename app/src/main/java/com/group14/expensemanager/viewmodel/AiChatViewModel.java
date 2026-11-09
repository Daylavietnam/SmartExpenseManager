package com.group14.expensemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.group14.expensemanager.data.TransactionRepository;
import com.group14.expensemanager.model.ChatMessage;
import com.group14.expensemanager.model.Partner;
import com.group14.expensemanager.model.Product;
import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.utils.AiManager;
import com.group14.expensemanager.utils.InventoryChatInterpreter;
import com.group14.expensemanager.utils.InventoryPartnerChatInterpreter;
import com.group14.expensemanager.utils.StockCalculator;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class AiChatViewModel extends AndroidViewModel {
    private static final Pattern ACTION_WORD = Pattern.compile("\\b(nhap|them|xuat|ban|tang|giam)\\b");
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)*");
    private static final Pattern QUESTION_START = Pattern.compile("^(bao nhieu|con bao nhieu|la gi|the nao|sao|o dau)\\b");
    private final TransactionRepository repository;
    private final LiveData<List<ChatMessage>> messages;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private InventoryChatInterpreter.Draft pendingMovement;
    private PendingConfirmation pendingConfirmation;
    private final LiveData<List<Partner>> partners;
    private final androidx.lifecycle.Observer<List<Partner>> partnerObserver = value -> {};
    private final androidx.lifecycle.Observer<List<Product>> productObserver = value -> {};
    private final androidx.lifecycle.Observer<List<Transaction>> movementObserver = value -> {};

    public AiChatViewModel(@NonNull Application application) {
        super(application);
        repository = TransactionRepository.getInstance(application);
        messages = repository.getChatMessages();
        partners = repository.getPartners();
        partners.observeForever(partnerObserver);
        repository.getProducts().observeForever(productObserver);
        repository.getTransactions().observeForever(movementObserver);
    }

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void sendMessage(String content) {
        if (content == null || content.trim().isEmpty() || Boolean.TRUE.equals(isLoading.getValue())) return;
        String text = InventoryChatInterpreter.normalize(content);
        repository.addChatMessage(new ChatMessage(content, ChatMessage.TYPE_USER));

        if (isCancel(text)) {
            pendingMovement = null;
            pendingConfirmation = null;
            reply("Đã hủy yêu cầu nhập–xuất đang chờ. Tồn kho chưa thay đổi.");
            return;
        }
        if (pendingConfirmation != null) {
            if (isConfirmation(text)) {
                confirmPendingMovement();
            } else {
                reply("Phiếu đang chờ xác nhận. Gửi “xác nhận” để lưu hoặc “hủy” để bỏ phiếu.");
            }
            return;
        }

        if (pendingMovement == null && isQuestion(content, text)) {
            isLoading.setValue(true);
            answerGeneralQuestion(content, repository.getProducts().getValue(), repository.getTransactions().getValue());
            return;
        }

        isLoading.setValue(true);
        List<Product> currentProducts = repository.getProducts().getValue();
        List<Partner> currentPartners = partners.getValue();
        if (currentProducts == null || currentPartners == null) {
            reply("Đang tải danh mục sản phẩm và đối tác. Gửi lại câu lệnh sau một chút; tồn kho chưa thay đổi.");
            return;
        }

        if (pendingMovement != null && pendingMovement.awaitingPartner) {
            InventoryPartnerChatInterpreter.Result local = InventoryPartnerChatInterpreter.interpret(
                    content, currentProducts, currentPartners, pendingMovement);
            if (local.handled && local.draft != null && !local.draft.awaitingPartner) {
                if (local.reply != null || !local.ready()) {
                    pendingMovement = local.draft;
                    reply(local.reply == null ? "Còn thiếu thông tin phiếu. Hãy gửi bổ sung; tồn kho chưa thay đổi." : local.reply);
                } else {
                    stageConfirmation(local, currentPartners);
                }
                return;
            }
        }

        if (pendingMovement != null || isInventoryCommand(text)) {
            AiManager.interpretInventoryCommand(content, currentProducts, currentPartners, pendingMovement,
                    new AiManager.InventoryCallback() {
                        @Override public void onSuccess(AiManager.InventoryCommand command) {
                            runOnUiThread(() -> handleInterpretation(command, content, currentProducts, currentPartners));
                        }
                        @Override public void onFailure(Exception error) {
                            runOnUiThread(() -> fallbackToLocalInterpreter(content, currentProducts, currentPartners, error));
                        }
                    });
            return;
        }
        answerGeneralQuestion(content, currentProducts, repository.getTransactions().getValue());
    }

    private void handleInterpretation(AiManager.InventoryCommand command, String original,
                                      List<Product> products, List<Partner> contacts) {
        if ("question".equals(command.intent) || "other".equals(command.intent)) {
            answerGeneralQuestion(original, products, repository.getTransactions().getValue());
            return;
        }
        if (!"receipt".equals(command.intent) && !"issue".equals(command.intent)) {
            reply("Mình chưa xác định được đây là phiếu nhập hay xuất. Hãy nói rõ “nhập” hoặc “xuất”. Tồn kho chưa thay đổi.");
            return;
        }

        Product product = resolveProduct(command, products);
        if (product == null) {
            pendingMovement = pendingFrom(command);
            StringBuilder message = new StringBuilder("Mình chưa xác định được sản phẩm trong danh mục. Hãy gửi đúng tên hoặc mã hàng.");
            String query = command.productName == null ? "" : InventoryChatInterpreter.normalize(command.productName);
            List<Product> options = findProductOptions(query, products);
            if (options.size() > 1) {
                for (int i = 0; i < Math.min(options.size(), 8); i++) {
                    Product option = options.get(i);
                    message.append("\n").append(option.sku).append(" — ").append(option.name);
                }
            }
            reply(message.append("\nTồn kho chưa thay đổi.").toString());
            return;
        }

        String canonical = buildCanonicalCommand(command, product);
        InventoryPartnerChatInterpreter.Result result = InventoryPartnerChatInterpreter.interpret(
                canonical, products, contacts, null);
        if (!result.handled || result.draft == null) {
            pendingMovement = null;
            reply("Mình chưa đọc được đủ thông tin phiếu. Hãy nói rõ sản phẩm và số lượng. Tồn kho chưa thay đổi.");
            return;
        }
        if (result.reply != null || !result.ready()) {
            pendingMovement = result.draft;
            reply(result.reply == null ? "Còn thiếu thông tin phiếu. Hãy gửi bổ sung; tồn kho chưa thay đổi." : result.reply);
            return;
        }
        stageConfirmation(result, contacts);
    }

    private void stageConfirmation(InventoryPartnerChatInterpreter.Result result, List<Partner> contacts) {
        Product product = result.product;
        if (product == null || result.draft == null) {
            pendingMovement = null;
            reply("Không tìm thấy sản phẩm trong danh mục. Tồn kho chưa thay đổi.");
            return;
        }
        pendingMovement = null;
        List<Transaction> movements = repository.getTransactions().getValue();
        if (Transaction.TYPE_EXPENSE.equals(result.draft.type)) {
            double stock = StockCalculator.stock(product.id, movements);
            if (result.draft.quantity > stock) {
                reply("Không đủ tồn kho " + product.name + ". Hiện còn " + number(stock) + " " + product.unit
                        + ", yêu cầu xuất " + number(result.draft.quantity) + ". Tồn kho chưa thay đổi.");
                return;
            }
        }

        double unitPrice = result.draft.unitPrice == null ? product.referencePrice : result.draft.unitPrice;
        pendingConfirmation = new PendingConfirmation(result.draft, product);
        StringBuilder summary = new StringBuilder("Mình hiểu phiếu như sau:\n")
                .append("Loại: ").append(Transaction.TYPE_INCOME.equals(result.draft.type) ? "Nhập kho" : "Xuất kho")
                .append("\nSản phẩm: ").append(product.name).append(" (").append(product.sku).append(")")
                .append("\nSố lượng: ").append(number(result.draft.quantity)).append(" ").append(product.unit)
                .append("\nĐơn giá: ").append(number(unitPrice)).append("đ/").append(product.unit)
                .append("\nTổng tiền: ").append(number(result.draft.quantity * unitPrice)).append("đ");
        if (result.draft.partnerId != 0) {
            for (Partner partner : contacts) if (partner.id == result.draft.partnerId) {
                summary.append("\nĐối tác: ").append(partner.name);
                break;
            }
        } else if (Transaction.TYPE_EXPENSE.equals(result.draft.type)) {
            pendingConfirmation = null;
            pendingMovement = result.draft;
            reply("Phiếu xuất cần chọn khách hàng. Gửi tên khách hàng trong danh bạ để tiếp tục; tồn kho chưa thay đổi.");
            return;
        }
        summary.append("\n\nGửi “xác nhận” để ghi phiếu hoặc “hủy” để bỏ.");
        reply(summary.toString());
    }

    private void confirmPendingMovement() {
        PendingConfirmation confirmed = pendingConfirmation;
        if (confirmed == null) return;
        isLoading.setValue(true);
        InventoryChatInterpreter.Draft draft = confirmed.draft;
        Product product = confirmed.product;
        double price = draft.unitPrice == null ? product.referencePrice : draft.unitPrice;
        Transaction movement = new Transaction("", "", draft.quantity * price, draft.type, System.currentTimeMillis());
        movement.setProductId(product.id);
        movement.setQuantity(draft.quantity);
        movement.setUnitPrice(price);
        movement.setPartnerId(draft.partnerId);
        repository.saveChatMovement(movement, draft.unitPrice == null, error -> {
            if (error != null) {
                reply("Không tạo được phiếu: " + error + " Tồn kho chưa thay đổi.");
            } else {
                pendingConfirmation = null;
                isLoading.setValue(false);
            }
        });
    }

    private void fallbackToLocalInterpreter(String content, List<Product> products, List<Partner> contacts,
                                            Exception error) {
        InventoryPartnerChatInterpreter.Result local = InventoryPartnerChatInterpreter.interpret(
                content, products, contacts, pendingMovement);
        if (local.handled && local.draft != null) {
            if (local.reply != null || !local.ready()) {
                pendingMovement = local.draft;
                reply(local.reply == null ? "Hãy gửi thêm thông tin phiếu. Tồn kho chưa thay đổi." : local.reply);
            } else {
                stageConfirmation(local, contacts);
            }
            return;
        }
        pendingMovement = null;
        String errorText = error == null ? "" : error.getMessage();
        if (errorText != null && errorText.contains("API key")) {
            reply("Gemini chưa được cấu hình hợp lệ, nên chưa thể hiểu câu lệnh tự nhiên này. "
                    + "Bạn có thể dùng dạng “nhập 10 MÃ_HÀNG” hoặc cấu hình API key hợp lệ. Tồn kho chưa thay đổi.");
        } else if (AiManager.isConfigured()) {
            reply("Gemini hiện không phân tích được câu lệnh. Hãy thử “nhập 10 MÃ_HÀNG” hoặc “xuất 2 MÃ_HÀNG đơn giá 5000 khách hàng TÊN”. Tồn kho chưa thay đổi.");
        } else {
            reply("Chưa có Gemini API key, nên chỉ hiểu được câu lệnh theo mã hàng: “nhập 10 MÃ_HÀNG” hoặc “xuất 2 MÃ_HÀNG đơn giá 5000 khách hàng TÊN”. Tồn kho chưa thay đổi.");
        }
    }

    private String buildCanonicalCommand(AiManager.InventoryCommand command, Product product) {
        boolean income = "receipt".equals(command.intent);
        StringBuilder result = new StringBuilder(income ? "nhập " : "xuất ").append(product.sku);
        if (validNumber(command.quantity)) result.append(" ").append(command.quantity);
        else if (!command.quantity.isEmpty()) result.append(" ").append(command.quantity);
        if (!command.unit.isEmpty()) result.append(" ").append(command.unit);
        if (!command.unitPrice.isEmpty()) result.append(" đơn giá ").append(command.unitPrice);
        if (!command.partnerName.isEmpty()) result.append(income ? " nhà cung cấp " : " khách hàng ").append(command.partnerName);
        return result.toString();
    }

    private InventoryChatInterpreter.Draft pendingFrom(AiManager.InventoryCommand command) {
        InventoryChatInterpreter.Draft draft = new InventoryChatInterpreter.Draft();
        draft.type = "receipt".equals(command.intent) ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;
        draft.quantity = parseAmount(command.quantity);
        draft.unitPrice = parseAmount(command.unitPrice);
        return draft;
    }

    private Double parseAmount(String value) {
        if (!validNumber(value)) return null;
        String normalized = value.trim();
        if (normalized.matches("\\d{1,3}(?:[.,]\\d{3})+")) normalized = normalized.replace(",", "").replace(".", "");
        else normalized = normalized.replace(',', '.');
        try {
            double amount = Double.parseDouble(normalized);
            return Double.isFinite(amount) ? amount : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Product resolveProduct(AiManager.InventoryCommand command, List<Product> products) {
        if (command.productSku != null && !command.productSku.trim().isEmpty()) {
            for (Product product : products) if (product.sku.equalsIgnoreCase(command.productSku.trim())) return product;
        }
        String query = InventoryChatInterpreter.normalize(command.productName == null ? "" : command.productName);
        List<Product> options = findProductOptions(query, products);
        return options.size() == 1 ? options.get(0) : null;
    }

    private List<Product> findProductOptions(String query, List<Product> products) {
        java.util.ArrayList<Product> options = new java.util.ArrayList<>();
        if (query == null || query.trim().isEmpty()) return options;
        String normalized = InventoryChatInterpreter.normalize(query);
        for (Product product : products) {
            String name = InventoryChatInterpreter.normalize(product.name);
            if (name.equals(normalized) || name.contains(normalized) || normalized.contains(name)) {
                options.add(product);
            }
        }
        if (!options.isEmpty()) return options;
        java.util.ArrayList<String> tokens = new java.util.ArrayList<>();
        for (String token : normalized.split(" ")) {
            if (token.length() > 1 && !java.util.Arrays.asList("chai", "cai", "hop", "thung", "goi", "tui", "bo", "cuon", "chiec", "san", "pham", "hang").contains(token)) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty()) return options;
        for (Product product : products) {
            String name = InventoryChatInterpreter.normalize(product.name);
            boolean all = true;
            for (String token : tokens) if (!name.contains(token)) { all = false; break; }
            if (all) options.add(product);
        }
        return options;
    }

    private boolean isInventoryCommand(String normalizedText) {
        return ACTION_WORD.matcher(normalizedText).find()
                || normalizedText.contains("kho hang") || normalizedText.contains("ton kho");
    }

    private boolean isQuestion(String content, String normalizedText) {
        if (ACTION_WORD.matcher(normalizedText).find()) return false;
        return content.trim().endsWith("?") || QUESTION_START.matcher(normalizedText).find();
    }

    private boolean isCancel(String text) {
        return text.equals("huy") || text.equals("bo qua") || text.equals("thoi") || text.equals("khong");
    }

    private boolean isConfirmation(String text) {
        return text.equals("xac nhan") || text.equals("dong y") || text.equals("luu") || text.equals("ok");
    }

    private boolean validNumber(String value) {
        return value != null && NUMBER.matcher(value.trim()).matches();
    }

    private void answerGeneralQuestion(String content, List<Product> products, List<Transaction> movements) {
        StringBuilder prompt = new StringBuilder("Bạn là trợ lý quản lý kho. Trả lời ngắn gọn bằng tiếng Việt. ")
                .append("Bạn chỉ trả lời câu hỏi, tuyệt đối không nói đã nhập/xuất/thay đổi dữ liệu. ")
                .append("Khi người dùng muốn cập nhật kho, hãy hướng dẫn gửi câu lệnh nhập hoặc xuất có tên/mã hàng. ")
                .append("Phiếu nhập có thể không cần đối tác; phiếu xuất cần khách hàng và đơn giá. ")
                .append("Không suy đoán tồn kho khi chưa có dữ liệu.\n");
        if (products != null && movements != null) {
            Map<Integer, Double> stocks = StockCalculator.stocks(movements);
            for (Product product : products) {
                prompt.append("Mã: ").append(product.sku).append("; tên: ").append(product.name)
                        .append("; tồn: ").append(number(stocks.getOrDefault(product.id, 0d)))
                        .append(" ").append(product.unit).append("; tồn tối thiểu: ").append(number(product.minimumStock)).append("\n");
            }
        } else prompt.append("Chưa tải được dữ liệu tồn kho.\n");
        prompt.append("Câu hỏi: ").append(content);
        AiManager.askAi(prompt.toString(), new AiManager.AiCallback() {
            @Override public void onSuccess(String response) {
                repository.addChatMessage(new ChatMessage(response, ChatMessage.TYPE_AI));
                isLoading.postValue(false);
            }
            @Override public void onFailure(Exception e) {
                runOnUiThread(() -> reply("Xin lỗi, tôi gặp lỗi khi kết nối: " + e.getMessage()));
            }
        });
    }

    private String number(double value) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }

    private void runOnUiThread(Runnable action) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(action);
    }

    public void clearHistory() {
        pendingMovement = null;
        pendingConfirmation = null;
        repository.clearChatHistory();
    }

    private void reply(String text) {
        repository.addChatMessage(new ChatMessage(text, ChatMessage.TYPE_AI));
        isLoading.setValue(false);
    }

    @Override protected void onCleared() {
        repository.getProducts().removeObserver(productObserver);
        repository.getTransactions().removeObserver(movementObserver);
        partners.removeObserver(partnerObserver);
        super.onCleared();
    }

    private static final class PendingConfirmation {
        final InventoryChatInterpreter.Draft draft;
        final Product product;
        PendingConfirmation(InventoryChatInterpreter.Draft draft, Product product) {
            this.draft = draft;
            this.product = product;
        }
    }
}
