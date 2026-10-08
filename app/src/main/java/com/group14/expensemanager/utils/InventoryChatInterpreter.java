package com.group14.expensemanager.utils;

import com.group14.expensemanager.model.Product;
import com.group14.expensemanager.model.Transaction;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Parses explicit stock instructions. Questions and guesses never authorize a stock write. */
public final class InventoryChatInterpreter {
    private static final Pattern ACTION = Pattern.compile("\\b(nhap|them|xuat|suat|ban)\\b");
    private static final Pattern NUMBER = Pattern.compile("(?<![\\p{L}\\p{N}])[-+]?\\d+(?:[.,]\\d+)*(?![\\p{L}\\p{N}])");
    private static final Pattern PRICE = Pattern.compile("\\b(?:don gia|gia)\\s*(?:la|:|=)?\\s*([-+]?\\d+(?:[.,]\\d+)*)(?:\\s*(k|nghin|ngan|trieu))?(?:\\s*(?:vnd|dong|d))?(?![\\p{L}\\p{N}])");
    private static final Pattern QUESTION = Pattern.compile("\\b(khong|chua|neu|gia su|du kien|se|co the|huong dan|lam sao|bao nhieu|co nen|vi du|dung|khoan)\\b|\\?");
    private static final Pattern OTHER_DATE = Pattern.compile("\\b(hom qua|hom kia|ngay mai|ngay|thang|nam)\\b|\\d+[/]\\d+");
    private static final Pattern UNIT = Pattern.compile("^\\s*(chai|cai|hop|thung|kg|gram|g|lit|l|goi|tui|bo|cuon|chiec|san pham|sp)\\b");

    public static class Draft {
        public String type;
        public int productId;
        public Double quantity;
        public Double unitPrice;
        public String suppliedUnit;
        Draft copy() {
            Draft d = new Draft(); d.type = type; d.productId = productId;
            d.quantity = quantity; d.unitPrice = unitPrice; d.suppliedUnit = suppliedUnit; return d;
        }
    }
    public static class Result {
        public final boolean handled;
        public final Draft draft;
        public final Product product;
        public final String reply;
        private Result(boolean handled, Draft draft, Product product, String reply) {
            this.handled = handled; this.draft = draft; this.product = product; this.reply = reply;
        }
        public boolean ready() { return handled && reply == null && product != null && draft.quantity != null
                && (!Transaction.TYPE_EXPENSE.equals(draft.type) || draft.unitPrice != null); }
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("\\s+", " ").trim();
    }

    public static Result interpret(String content, List<Product> products, Draft pending) {
        String text = normalize(content);
        if (pending != null && (text.equals("huy") || text.equals("bo qua") || text.equals("thoi") || text.equals("khong")))
            return new Result(true, null, null, "Đã hủy yêu cầu nhập–xuất đang chờ. Tồn kho chưa thay đổi.");
        Matcher actions = ACTION.matcher(text);
        boolean receipt = false, issue = false, explicit = false;
        while (actions.find()) {
            explicit = true;
            if (actions.group().equals("nhap") || actions.group().equals("them")) receipt = true;
            else issue = true;
        }
        if ((!explicit && pending == null) || QUESTION.matcher(text).find())
            return new Result(false, pending, null, null);
        if (receipt && issue) return reject("Hãy gửi riêng từng phiếu nhập hoặc xuất, mỗi tin nhắn một sản phẩm.");
        if (OTHER_DATE.matcher(text).find())
            return reject("Chat ghi phiếu cho hôm nay. Với ngày khác, hãy dùng màn hình Nhập–xuất để chọn ngày.");

        Draft draft = explicit ? new Draft() : pending.copy();
        if (explicit) draft.type = receipt ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;
        if (products == null) return reject("Đang tải danh mục kho. Vui lòng gửi lại yêu cầu sau một chút.");
        if (products.isEmpty()) return reject("Kho chưa có sản phẩm. Hãy thêm sản phẩm trong Sản phẩm & tồn kho trước.");

        // A number supplied after the price question is a unit price, never a new quantity or SKU.
        if (!explicit && pending != null && Transaction.TYPE_EXPENSE.equals(pending.type)
                && pending.productId != 0 && pending.quantity != null && pending.unitPrice == null) {
            Product product = null;
            for (Product p : products) if (p.id == pending.productId) product = p;
            if (product == null) return reject("Sản phẩm không còn tồn tại. Hãy gửi lại yêu cầu.");
            Pattern answer = Pattern.compile("^(?:(?:don gia|gia)\\s*(?:la|:|=)?\\s*)?([-+]?\\d+(?:[.,]\\d+)*)"
                    + "(?:\\s*(k|nghin|ngan|trieu))?(?:\\s*(?:vnd|dong|d|₫))?"
                    + "(?:\\s*/\\s*" + Pattern.quote(normalize(product.unit)) + ")?\\s*$");
            Matcher value = answer.matcher(text);
            if (!value.matches()) return askPrice(draft, product, "Hãy gửi đơn giá bằng số, ví dụ 5000 hoặc giá 5000đ. ");
            try {
                double amount = number(value.group(1));
                if (value.group(2) != null) amount *= value.group(2).equals("trieu") ? 1000000 : 1000;
                if (!Double.isFinite(amount) || amount < 0)
                    return askPrice(draft, product, "Đơn giá phải là số không âm. ");
                draft.unitPrice = amount;
            } catch (NumberFormatException e) { return askPrice(draft, product, "Đơn giá không hợp lệ. "); }
            return new Result(true, draft, product, null);
        }

        List<Product> matches = new ArrayList<>();
        List<Product> skuMatches = new ArrayList<>();
        for (Product p : products) {
            boolean numericSku = p.sku.matches("\\d+(?:[.,]\\d+)*");
            boolean markedSku = Pattern.compile("\\bma(?: hang)?\\s*[:=]?\\s*" + Pattern.quote(normalize(p.sku)) + "(?![\\p{L}\\p{N}])").matcher(text).find();
            boolean bareSkuReply = pending != null && pending.productId == 0 && text.equals(normalize(p.sku));
            if ((!numericSku || markedSku || bareSkuReply)
                    && contains(content.toLowerCase(Locale.ROOT), p.sku.toLowerCase(Locale.ROOT))) skuMatches.add(p);
            if (!normalize(p.name).matches("\\d+(?:[.,]\\d+)*") && contains(text, normalize(p.name))) matches.add(p);
        }
        // Prefer a unique SKU when duplicate names exist; still reject multiple explicitly named goods.
        if (skuMatches.size() > 1) return reject("Có nhiều mã hàng trong tin nhắn. Hãy gửi mỗi lần một sản phẩm.");
        Product selected = null;
        if (skuMatches.size() == 1) {
            selected = skuMatches.get(0);
            for (Product p : matches) if (p.id != selected.id && !normalize(p.name).equals(normalize(selected.name)))
                return reject("Có nhiều sản phẩm trong tin nhắn. Hãy gửi mỗi lần một sản phẩm.");
        } else if (matches.size() == 1) selected = matches.get(0);
        else if (matches.size() > 1) {
            draft.productId = 0;
            // Continue extracting quantity so a subsequent SKU reply can complete the same instruction.
        }
        if (selected != null) draft.productId = selected.id;
        else if (matches.size() > 1) draft.productId = 0;

        String remaining = text;
        for (Product p : skuMatches) remaining = mask(remaining, normalize(p.sku));
        for (Product p : matches) remaining = mask(remaining, normalize(p.name));
        Matcher price = PRICE.matcher(remaining);
        if (price.find()) {
            try {
                double value = number(price.group(1));
                String multiplier = price.group(2);
                if (multiplier != null) value *= multiplier.equals("trieu") ? 1000000 : 1000;
                if (!Double.isFinite(value) || value < 0) return reject("Đơn giá phải là số không âm.");
                draft.unitPrice = value;
            } catch (NumberFormatException e) { return reject("Đơn giá không hợp lệ."); }
            remaining = remaining.substring(0, price.start()) + " " + remaining.substring(price.end());
        }
        Matcher numbers = NUMBER.matcher(remaining);
        String parsedUnit = null;
        if (numbers.find()) {
            try { draft.quantity = number(numbers.group()); }
            catch (NumberFormatException e) { return reject("Số lượng không hợp lệ."); }
            String suffix = remaining.substring(numbers.end());
            List<String> units = new ArrayList<>();
            for (Product p : products) if (!normalize(p.unit).isEmpty()) units.add(normalize(p.unit));
            units.sort((a, b) -> Integer.compare(b.length(), a.length()));
            for (String candidate : units) {
                if (Pattern.compile("^\\s*" + Pattern.quote(candidate) + "(?![\\p{L}\\p{N}])").matcher(suffix).find()) {
                    parsedUnit = candidate; break;
                }
            }
            Matcher unit = UNIT.matcher(suffix);
            if (unit.find()) draft.suppliedUnit = unit.group(1);
            if (parsedUnit != null) draft.suppliedUnit = parsedUnit;
            if (numbers.find()) return reject("Có nhiều số trong tin nhắn. Hãy ghi rõ: xuất 5 NS001, đơn giá 5000.");
        }
        if (draft.quantity != null && (!Double.isFinite(draft.quantity) || draft.quantity <= 0))
            return reject("Số lượng phải lớn hơn 0.");
        String cleaned = parsedUnit == null ? remaining : mask(remaining, parsedUnit);
        String unknown = NUMBER.matcher(cleaned).replaceAll(" ")
                .replaceAll("\\b(toi|da|vua|nhap|them|xuat|suat|ban|vao|kho|ra|ma|hang|hoa|san|pham|sp|so|luong|la|duoc|giup|minh|cho|hom|nay|chai|cai|hop|thung|kg|gram|g|lit|l|goi|tui|bo|cuon|chiec|vnd|dong|d|moi|muon|can|nha|nhe)\\b", " ")
                .replaceAll("[\\s\\p{Punct}]", "");
        if (!unknown.isEmpty()) {
            if (selected != null) return reject("Chưa hiểu đầy đủ yêu cầu. Hãy ghi: nhập 20 " + selected.sku + " hoặc xuất 5 " + selected.sku + ".");
            // A different/unknown product in a follow-up must not use the old selected product.
            draft.productId = 0;
        }
        if (draft.productId == 0) {
            StringBuilder reply = new StringBuilder("Bạn muốn ").append(receipt || Transaction.TYPE_INCOME.equals(draft.type) ? "nhập" : "xuất")
                    .append(" sản phẩm nào? Hãy gửi mã hàng hoặc tên đầy đủ có trong danh mục.");
            if (matches.size() > 1) for (Product p : matches) reply.append("\n").append(p.sku).append(" — ").append(p.name);
            return new Result(true, draft, null, reply.toString());
        }
        Product product = null;
        for (Product p : products) if (p.id == draft.productId) product = p;
        if (product == null) return reject("Sản phẩm không còn tồn tại. Hãy gửi lại mã hàng.");
        if (draft.suppliedUnit != null && !draft.suppliedUnit.equals("san pham") && !draft.suppliedUnit.equals("sp")
                && !draft.suppliedUnit.equals(normalize(product.unit)))
            return reject("Đơn vị của " + product.name + " là " + product.unit + ". Hãy gửi lại số lượng theo đơn vị này.");
        if (draft.quantity == null)
            return new Result(true, draft, product, "Bạn muốn " + (Transaction.TYPE_INCOME.equals(draft.type) ? "nhập" : "xuất")
                    + " bao nhiêu " + product.unit + " " + product.name + "? Gửi số lượng, hoặc gõ Hủy.");
        if (Transaction.TYPE_EXPENSE.equals(draft.type) && draft.unitPrice == null)
            return askPrice(draft, product, "");
        return new Result(true, draft, product, null);
    }

    private static Result askPrice(Draft draft, Product product, String prefix) {
        return new Result(true, draft, product, prefix + "Bạn xuất " + draft.quantity + " " + product.unit
                + " " + product.name + " (" + product.sku + ") với đơn giá bao nhiêu VNĐ/" + product.unit
                + "? Gửi đơn giá hoặc gõ Hủy. Tồn kho chưa thay đổi.");
    }

    private static Result reject(String message) { return new Result(true, null, null, message + " Tồn kho chưa thay đổi."); }
    private static boolean contains(String text, String term) {
        return !term.isEmpty() && Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(term) + "(?![\\p{L}\\p{N}])").matcher(text).find();
    }
    private static String mask(String text, String term) {
        if (term.isEmpty()) return text;
        return text.replaceAll("(?<![\\p{L}\\p{N}])" + Pattern.quote(term) + "(?![\\p{L}\\p{N}])", " ");
    }
    private static double number(String input) {
        String value = input;
        if (value.matches("[-+]?\\d{1,3}(?:[.,]\\d{3})+")) value = value.replace(",", "").replace(".", "");
        else value = value.replace(',', '.');
        return Double.parseDouble(value);
    }
}
