package com.group14.expensemanager.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.group14.expensemanager.R;
import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.utils.AppAssets;
import com.group14.expensemanager.utils.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddActivity extends AppCompatActivity {
    private SwitchCompat mswTheme;
    private boolean isUpdatingThemeSwitch = false;
    private MaterialButtonToggleGroup mbtgTransactionType;
    private Button mbtnExpense, mbtnIncome;
    private String selectedTransactionType = Transaction.TYPE_INCOME;
    private ChipGroup cgCategories;
    private String selectedCategory = "Thực phẩm";

    private TextInputEditText tietTitle, tietAmount;
    private TextView mtvDate;
    private Button mbtnClear, mbtnSave, mbtnSettings;
    private FloatingActionButton fabAi;
    private long selectedDateMillis;
    private BottomNavigationView bnvMain;


    private Transaction editingTransaction;

    private com.group14.expensemanager.data.TransactionRepository stockRepository;
    private android.widget.Spinner productSpinner;
    private TextInputEditText tietQuantity;
    private TextView stockHint;
    private java.util.List<com.group14.expensemanager.model.Product> products = new java.util.ArrayList<>();
    private java.util.List<Transaction> movements = new java.util.ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        mswTheme = findViewById(R.id.msw_theme);
        mbtnSettings = findViewById(R.id.mbtn_settings);
        mbtgTransactionType = findViewById(R.id.mbtg_transaction_type);
        mbtnExpense = findViewById(R.id.mbtn_expense);
        mbtnIncome = findViewById(R.id.mbtn_income);
        cgCategories = findViewById(R.id.cg_categories);
        tietTitle = findViewById(R.id.tiet_title);
        tietAmount = findViewById(R.id.tiet_amount);
        mtvDate = findViewById(R.id.mtv_date);
        mbtnClear = findViewById(R.id.mbtn_clear);
        mbtnSave = findViewById(R.id.mbtn_save);
        fabAi = findViewById(R.id.fab_ai);
        bnvMain = findViewById(R.id.bnv_main);

        setupThemeSwitch();
        setupTransactionType();
        setupCurrentDate();
        setupNavigation();


        mtvDate.setOnClickListener(v -> showDatePicker());
        mbtnClear.setOnClickListener(v -> clearForm());
        mbtnSave.setOnClickListener(v -> saveTransaction());

        checkEditMode();
        setupProducts();
    }

    private void setupProducts() {
        stockRepository = com.group14.expensemanager.data.TransactionRepository.getInstance(getApplication());
        android.widget.LinearLayout parent = (android.widget.LinearLayout) findViewById(R.id.til_title).getParent();
        int position = parent.indexOfChild(findViewById(R.id.til_title));
        android.widget.LinearLayout fields = new android.widget.LinearLayout(this);
        fields.setOrientation(android.widget.LinearLayout.VERTICAL);
        Button catalog = new com.google.android.material.button.MaterialButton(this);
        catalog.setText("Sản phẩm & tồn kho");
        catalog.setOnClickListener(v -> startActivity(new Intent(this, ProductActivity.class)));
        fields.addView(catalog);
        TextView label = new TextView(this); label.setText("Sản phẩm"); label.setTextSize(14);
        label.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ui_muted));
        label.setPadding(0, dp(16), 0, dp(8)); fields.addView(label);
        productSpinner = new android.widget.Spinner(this); productSpinner.setBackgroundResource(R.drawable.bg_stock_field);
        fields.addView(productSpinner, new android.widget.LinearLayout.LayoutParams(-1, dp(58)));
        stockHint = new TextView(this); stockHint.setTextSize(13); stockHint.setPadding(0, dp(10), 0, dp(16));
        stockHint.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ui_primary)); fields.addView(stockHint);
        com.google.android.material.textfield.TextInputLayout quantityLayout = new com.google.android.material.textfield.TextInputLayout(this);
        quantityLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE); quantityLayout.setBoxCornerRadii(dp(14), dp(14), dp(14), dp(14)); quantityLayout.setHint("Số lượng");
        tietQuantity = new TextInputEditText(this);
        tietQuantity.setBackground(null);
        tietQuantity.setMinHeight(dp(56)); tietQuantity.setTextSize(16);
        tietQuantity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        quantityLayout.addView(tietQuantity); fields.addView(quantityLayout);
        parent.addView(fields, position);
        findViewById(R.id.til_title).setVisibility(android.view.View.GONE);
        cgCategories.setVisibility(android.view.View.GONE);
        tietQuantity.setText(editingTransaction == null ? "1" : String.valueOf(editingTransaction.getQuantity()));
        if (editingTransaction != null) tietAmount.setText(String.valueOf(editingTransaction.getUnitPrice()));
        productSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
            public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int index, long id) {
                if (index >= products.size()) return;
                com.group14.expensemanager.model.Product product = products.get(index);
                tietTitle.setText(product.name); selectedCategory = product.category;
                if (editingTransaction == null || editingTransaction.getProductId() != product.id)
                    tietAmount.setText(String.valueOf(product.referencePrice));
                updateStockHint();
            }
        });
        stockRepository.getProducts().observe(this, list -> {
            int selectedId = editingTransaction != null ? editingTransaction.getProductId() : selectedProductId();
            products = list;
            productSpinner.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, products));
            for (int i = 0; i < products.size(); i++) if (products.get(i).id == selectedId) productSpinner.setSelection(i);
            mbtnSave.setEnabled(!products.isEmpty()); updateStockHint();
        });
        stockRepository.getTransactions().observe(this, list -> { movements = list; updateStockHint(); });
    }

    private int selectedProductId() {
        int index = productSpinner == null ? -1 : productSpinner.getSelectedItemPosition();
        return index >= 0 && index < products.size() ? products.get(index).id : 0;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void updateStockHint() {
        int index = productSpinner.getSelectedItemPosition();
        if (index < 0 || index >= products.size()) { stockHint.setText("Chưa có sản phẩm. Hãy thêm sản phẩm trước."); return; }
        com.group14.expensemanager.model.Product p = products.get(index);
        stockHint.setText("Tồn hiện tại: " + java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(com.group14.expensemanager.utils.StockCalculator.stock(p.id, movements)) + " " + p.unit + " • " + p.category);
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TRANSACTION")) {
            editingTransaction = (Transaction) intent.getSerializableExtra("TRANSACTION");
            if (editingTransaction != null) {
                tietTitle.setText(editingTransaction.getTitle());
                tietAmount.setText(String.valueOf((long) editingTransaction.getAmount()));
                selectedDateMillis = editingTransaction.getTimestamp();
                updateDateDisplay();
                
                if (Transaction.TYPE_INCOME.equals(editingTransaction.getType())) {
                    setIncomeMode();
                    mbtgTransactionType.check(R.id.mbtn_expense);
                    mbtgTransactionType.check(R.id.mbtn_income);
                } else {
                    setExpenseMode();
                    mbtgTransactionType.check(R.id.mbtn_expense);
                }
                
                selectedCategory = editingTransaction.getCategory();
                for (int i = 0; i < cgCategories.getChildCount(); i++) {
                    Chip chip = (Chip) cgCategories.getChildAt(i);
                    if (selectedCategory.equals(chip.getText().toString())) {
                        chip.setChecked(true);
                        break;
                    }
                }
                
                mbtnSave.setText("Cập nhật");
                mbtnClear.setVisibility(android.view.View.GONE);
                TextView mtvToolbarTitle = findViewById(R.id.mtv_toolbar_title);
                if (mtvToolbarTitle != null) mtvToolbarTitle.setText("Chỉnh sửa phiếu kho");
            }
        }
    }

    private void setupThemeSwitch() {
        mswTheme.setSaveEnabled(false);
        mswTheme.setChecked(ThemeManager.isDarkMode(this));
        mswTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingThemeSwitch) return;
            ThemeManager.setDarkMode(this, isChecked);
        });
    }

    private void setupTransactionType() {
        mbtgTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.mbtn_expense) setExpenseMode();
            else if (checkedId == R.id.mbtn_income) setIncomeMode();
        });
        mbtgTransactionType.check(R.id.mbtn_income);
        setIncomeMode();
    }

    private void setExpenseMode() {
        selectedTransactionType = Transaction.TYPE_EXPENSE;
        mbtnExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_accent)));
        mbtnIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_surface)));
        updateCategoryChips(AppAssets.EXPENSE_CATEGORIES);
    }

    private void setIncomeMode() {
        selectedTransactionType = Transaction.TYPE_INCOME;
        mbtnIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_accent)));
        mbtnExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_surface)));
        updateCategoryChips(AppAssets.INCOME_CATEGORIES);
    }

    private void updateCategoryChips(java.util.List<AppAssets.CategoryItem> categories) {
        cgCategories.removeAllViews();
        for (AppAssets.CategoryItem item : categories) {
            Chip chip = new Chip(this);
            chip.setText(item.name);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setCheckedIconVisible(true);
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedCategory = item.name;
                }
            });
            cgCategories.addView(chip);
        }
        if (cgCategories.getChildCount() > 0) {
            ((Chip) cgCategories.getChildAt(0)).setChecked(true);
            selectedCategory = categories.get(0).name;
        }
    }

    private void setupCurrentDate() {
        selectedDateMillis = System.currentTimeMillis();
        updateDateDisplay();
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        mtvDate.setText("Ngày phiếu kho: " + sdf.format(new Date(selectedDateMillis)));
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày phiếu kho")
                .setSelection(selectedDateMillis)
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDateMillis = selection;
            updateDateDisplay();
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void setupNavigation() {
        if (mbtnSettings != null) {
            mbtnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }
        if (fabAi != null) {
            fabAi.setOnClickListener(v -> startActivity(new Intent(this, AiChatActivity.class)));
        }
        bnvMain.setSelectedItemId(R.id.nav_add);
        bnvMain.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_detail) {
                startActivity(new Intent(this, DetailActivity.class));
                return true;
            } else if (itemId == R.id.nav_report) {
                startActivity(new Intent(this, ReportActivity.class));
                return true;
            }
            return true;
        });
    }

    private void clearForm() {
        tietTitle.setText("");
        tietAmount.setText("");
        if (tietQuantity != null) { tietQuantity.setText("1"); updateStockHint(); }
        setupCurrentDate();
    }

    private void saveTransaction() {
        int productId = selectedProductId();
        if (productId == 0) { Toast.makeText(this, "Hãy thêm và chọn sản phẩm", Toast.LENGTH_LONG).show(); return; }
        double quantity, price;
        try {
            quantity = Double.parseDouble(tietQuantity.getText().toString().trim());
            price = Double.parseDouble(tietAmount.getText().toString().trim());
        } catch (Exception e) { Toast.makeText(this, "Nhập số lượng và đơn giá hợp lệ", Toast.LENGTH_LONG).show(); return; }
        if (!Double.isFinite(quantity) || quantity <= 0 || !Double.isFinite(price) || price < 0) {
            Toast.makeText(this, "Số lượng phải lớn hơn 0, đơn giá không được âm", Toast.LENGTH_LONG).show(); return;
        }
        if (selectedDateMillis > System.currentTimeMillis()) {
            Toast.makeText(this, "Ngày phiếu không được ở tương lai", Toast.LENGTH_LONG).show(); return;
        }
        Transaction movement = new Transaction("", selectedCategory, quantity * price, selectedTransactionType, selectedDateMillis);
        if (editingTransaction != null) movement.setId(editingTransaction.getId());
        movement.setProductId(productId); movement.setQuantity(quantity); movement.setUnitPrice(price);
        mbtnSave.setEnabled(false);
        stockRepository.saveMovement(movement, error -> {
            mbtnSave.setEnabled(true);
            if (error != null) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); return; }
            Toast.makeText(this, "Đã lưu phiếu kho", Toast.LENGTH_SHORT).show();
            if (editingTransaction != null) finish(); else clearForm();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isUpdatingThemeSwitch = true;
        mswTheme.setChecked(ThemeManager.isDarkMode(this));
        isUpdatingThemeSwitch = false;
    }
}
