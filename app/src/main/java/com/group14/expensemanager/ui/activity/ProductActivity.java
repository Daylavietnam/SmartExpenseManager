package com.group14.expensemanager.ui.activity;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import com.group14.expensemanager.data.TransactionRepository;
import com.group14.expensemanager.model.Product;
import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.utils.StockCalculator;
import java.text.NumberFormat;
import java.util.*;

/** Product catalog and stock on hand, using the same Room repository as the existing screens. */
public class ProductActivity extends AppCompatActivity {
    private TransactionRepository repository;
    private List<Product> products = new ArrayList<>();
    private List<Transaction> movements = new ArrayList<>();
    private LinearLayout rows;
    private EditText search;
    private final NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        repository = TransactionRepository.getInstance(getApplication());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(com.group14.expensemanager.R.color.ui_background));
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom);
            return insets;
        });
        MaterialButton back = new MaterialButton(this);
        back.setText("‹ Tổng quan kho");
        back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color(com.group14.expensemanager.R.color.ui_accent)));
        back.setTextColor(color(com.group14.expensemanager.R.color.ui_primary));
        back.setOnClickListener(v -> finish());
        root.addView(back);
        TextView title = new TextView(this);
        title.setText("Sản phẩm & tồn kho"); title.setTextSize(26);
        title.setTextColor(color(com.group14.expensemanager.R.color.ui_text));
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        title.setPadding(0, dp(16), 0, dp(6));
        root.addView(title);
        TextView subtitle = text("Theo dõi số lượng và bổ sung hàng đúng lúc", 14, com.group14.expensemanager.R.color.ui_muted);
        subtitle.setPadding(0, 0, 0, dp(20)); root.addView(subtitle);
        search = new EditText(this);
        search.setHint("Tìm tên hoặc mã hàng"); search.setSingleLine(true);
        search.setTextSize(15); search.setTextColor(color(com.group14.expensemanager.R.color.ui_text));
        search.setHintTextColor(color(com.group14.expensemanager.R.color.ui_muted));
        search.setBackgroundResource(com.group14.expensemanager.R.drawable.bg_stock_field);
        search.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.addView(search);
        MaterialButton add = new MaterialButton(this);
        add.setText("+ Thêm sản phẩm"); add.setOnClickListener(v -> showForm(null));
        root.addView(add);
        ScrollView scroll = new ScrollView(this);
        rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            public void afterTextChanged(android.text.Editable s) {}
        });
        repository.getProducts().observe(this, list -> { products = list; render(); });
        repository.getTransactions().observe(this, list -> { movements = list; render(); });
    }

    private void render() {
        rows.removeAllViews();
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (Product p : products) {
            if (!(p.sku + " " + p.name).toLowerCase(Locale.ROOT).contains(query)) continue;
            count++;
            double stock = StockCalculator.stock(p.id, movements);
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
            card.setRadius(dp(20)); card.setCardElevation(0); card.setStrokeWidth(dp(1));
            card.setStrokeColor(color(com.group14.expensemanager.R.color.ui_border));
            card.setCardBackgroundColor(color(com.group14.expensemanager.R.color.ui_surface));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2); cardParams.bottomMargin = dp(12);
            rows.addView(card, cardParams);
            LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20), dp(18), dp(20), dp(12)); card.addView(content);
            TextView name = text(p.name, 19, com.group14.expensemanager.R.color.ui_text);
            name.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0)); content.addView(name);
            TextView meta = text(p.sku + "  •  " + p.category, 12, com.group14.expensemanager.R.color.ui_muted);
            meta.setPadding(0, dp(4), 0, dp(14)); content.addView(meta);
            TextView quantity = text(format.format(stock) + " " + p.unit, 28, com.group14.expensemanager.R.color.ui_primary);
            quantity.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0)); content.addView(quantity);
            TextView status = text(stock <= p.minimumStock ? "● Cần nhập thêm" : "● Tồn kho ổn định", 12,
                    stock <= p.minimumStock ? com.group14.expensemanager.R.color.expense_red : com.group14.expensemanager.R.color.income_green);
            status.setPadding(0, dp(6), 0, dp(14)); content.addView(status);
            content.addView(text("Giá tham chiếu: " + format.format(p.referencePrice) + "đ/" + p.unit
                    + "\nTồn tối thiểu: " + format.format(p.minimumStock) + " " + p.unit, 13, com.group14.expensemanager.R.color.ui_muted));
            LinearLayout actions = new LinearLayout(this);
            MaterialButton edit = new MaterialButton(this); edit.setText("Sửa");
            styleAction(edit, com.group14.expensemanager.R.color.ui_primary);
            edit.setOnClickListener(v -> showForm(p)); actions.addView(edit);
            MaterialButton delete = new MaterialButton(this); delete.setText("Xóa");
            styleAction(delete, com.group14.expensemanager.R.color.expense_red);
            delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Xóa sản phẩm?")
                    .setMessage(p.toString()).setNegativeButton("Hủy", null)
                    .setPositiveButton("Xóa", (d, w) -> repository.deleteProduct(p, error ->
                            Toast.makeText(this, error == null ? "Đã xóa sản phẩm" : error, Toast.LENGTH_LONG).show())).show());
            actions.addView(delete); content.addView(actions);
        }
        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText(products.isEmpty() ? "Chưa có sản phẩm. Thêm sản phẩm, sau đó tạo phiếu nhập để ghi nhận tồn đầu kỳ." : "Không tìm thấy sản phẩm.");
            empty.setPadding(0, 24, 0, 0); rows.addView(empty);
            empty.setTextColor(color(com.group14.expensemanager.R.color.ui_muted)); empty.setTextSize(15);
        }
    }

    private EditText field(LinearLayout form, String hint, String value, boolean numeric) {
        com.google.android.material.textfield.TextInputLayout layout = new com.google.android.material.textfield.TextInputLayout(this);
        layout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE); layout.setBoxCornerRadii(dp(14), dp(14), dp(14), dp(14)); layout.setHint(hint);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(14); form.addView(layout, params);
        EditText edit = new com.google.android.material.textfield.TextInputEditText(this); edit.setText(value); edit.setSingleLine(true); edit.setTextSize(15);
        edit.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT);
        edit.setBackground(null);
        layout.addView(edit); return edit;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int color(int resource) { return androidx.core.content.ContextCompat.getColor(this, resource); }
    private TextView text(String value, int size, int resource) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color(resource)); view.setLineSpacing(dp(3), 1); return view;
    }
    private void styleAction(MaterialButton button, int resource) {
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        button.setTextColor(color(resource)); button.setMinWidth(0); button.setMinimumWidth(0); button.setTextSize(13);
    }

    private void showForm(Product existing) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(20), dp(20), dp(16));
        EditText sku = field(form, "Mã hàng (duy nhất)", existing == null ? "" : existing.sku, false);
        EditText name = field(form, "Tên sản phẩm", existing == null ? "" : existing.name, false);
        EditText unit = field(form, "Đơn vị tính", existing == null ? "cái" : existing.unit, false);
        Spinner category = new Spinner(this);
        List<String> groups = new ArrayList<>();
        for (com.group14.expensemanager.utils.AppAssets.CategoryItem c : com.group14.expensemanager.utils.AppAssets.EXPENSE_CATEGORIES) groups.add(c.name);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, groups));
        if (existing != null) category.setSelection(Math.max(0, groups.indexOf(existing.category)));
        TextView groupLabel = new TextView(this); groupLabel.setText("Nhóm sản phẩm"); form.addView(groupLabel); form.addView(category);
        EditText price = field(form, "Giá tham chiếu (VNĐ / đơn vị)", existing == null ? "0" : String.valueOf(existing.referencePrice), true);
        EditText minimum = field(form, "Mức tồn tối thiểu", existing == null ? "0" : String.valueOf(existing.minimumStock), true);
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(existing == null ? "Thêm sản phẩm" : "Sửa sản phẩm")
                .setView(scroll).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Product p = new Product(); p.id = existing == null ? 0 : existing.id;
            p.sku = sku.getText().toString().trim().toUpperCase(Locale.ROOT);
            p.name = name.getText().toString().trim(); p.unit = unit.getText().toString().trim();
            p.category = category.getSelectedItem().toString();
            try {
                p.referencePrice = Double.parseDouble(price.getText().toString().trim());
                p.minimumStock = Double.parseDouble(minimum.getText().toString().trim());
            } catch (NumberFormatException e) { Toast.makeText(this, "Giá và mức tồn phải là số hợp lệ", Toast.LENGTH_LONG).show(); return; }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            repository.saveProduct(p, error -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                if (error == null) dialog.dismiss();
                else Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            });
        }));
        dialog.show();
    }
}
