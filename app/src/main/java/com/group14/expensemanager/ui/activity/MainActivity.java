package com.group14.expensemanager.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.group14.expensemanager.R;
import com.group14.expensemanager.utils.AppAssets;
import com.group14.expensemanager.utils.SecurityManager;
import com.group14.expensemanager.utils.ThemeManager;
import com.group14.expensemanager.viewmodel.MainViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView mtvMonthYear, mtvTotal, mtvIncome, mtvExpense;
    private TextView mtvBudgetInfo, mtvBudgetRemaining, mtvBudgetWarning;
    private LinearProgressIndicator lpiBudget;
    private TextInputEditText tietBudget;
    private TextInputLayout tilBudget;
    private View mcvBudgetContainer;

    private boolean isUpdatingThemeSwitch = false;
    private LinearLayout layoutLockScreen;
    private MaterialButton btnUnlock;

    private TextInputLayout tilPin;
    private TextInputEditText edtLoginPin;

    private TextView mtvAiExpensePlaceholder, mtvAiIncomePlaceholder;
    private SwitchCompat mswTheme;
    private Button mbtnSettings;
    private FloatingActionButton fabAi;
    private BottomNavigationView bnvMain;

    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mtvMonthYear = findViewById(R.id.mtv_monthyear);
        mtvTotal = findViewById(R.id.mtv_total);
        mtvIncome = findViewById(R.id.mtv_income);
        mtvExpense = findViewById(R.id.mtv_expense);
        mswTheme = findViewById(R.id.msw_theme);
        mbtnSettings = findViewById(R.id.mbtn_settings);
        fabAi = findViewById(R.id.fab_ai);
        bnvMain = findViewById(R.id.bnv_main);
        tilBudget = findViewById(R.id.til_budget);
        tietBudget = findViewById(R.id.tiet_budget);
        mcvBudgetContainer = findViewById(R.id.mcv_budget_container);
        mtvBudgetInfo = findViewById(R.id.mtv_budget_info);
        mtvBudgetRemaining = findViewById(R.id.mtv_budget_remaining);
        mtvBudgetWarning = findViewById(R.id.mtv_budget_warning);
        lpiBudget = findViewById(R.id.lpi_budget);
        mtvAiExpensePlaceholder = findViewById(R.id.mtv_ai_expense_placeholder);
        mtvAiIncomePlaceholder = findViewById(R.id.mtv_ai_income_placeholder);
        layoutLockScreen = findViewById(R.id.layoutLockScreen);
        btnUnlock = findViewById(R.id.btnUnlock);
        tilPin = findViewById(R.id.tilPin);
        edtLoginPin = findViewById(R.id.edtLoginPin);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        observeViewModel();
        setupStockOverview();
        setupThemeSwitch();
        setupLockScreen();
        setupNavigation();
    }

    private void observeViewModel() {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

        viewModel.getSummaryData().observe(this, data -> {
            mtvMonthYear.setText(String.format(Locale.forLanguageTag("vi-VN"), "Tháng %d - %d", data.month + 1, data.year));
            mtvIncome.setText(formatter.format(data.income) + "đ");
            mtvExpense.setText(formatter.format(data.expense) + "đ");
        });

        viewModel.getExpenseAnalysis().observe(this, analysis -> {
            if (analysis != null) {
                mtvAiExpensePlaceholder.setText(Html.fromHtml(analysis, Html.FROM_HTML_MODE_COMPACT));
                mtvAiExpensePlaceholder.setGravity(Gravity.START);
            } else {
                mtvAiExpensePlaceholder.setText("Chưa có dữ liệu");
                mtvAiExpensePlaceholder.setGravity(Gravity.CENTER);
            }
        });

        viewModel.getIncomeAnalysis().observe(this, analysis -> {
            if (analysis != null) {
                mtvAiIncomePlaceholder.setText(Html.fromHtml(analysis, Html.FROM_HTML_MODE_COMPACT));
                mtvAiIncomePlaceholder.setGravity(Gravity.START);
            } else {
                mtvAiIncomePlaceholder.setText("Chưa có dữ liệu");
                mtvAiIncomePlaceholder.setGravity(Gravity.CENTER);
            }
        });


    }

    private void setupStockOverview() {
        tilBudget.setVisibility(View.GONE);
        lpiBudget.setVisibility(View.GONE);
        mcvBudgetContainer.setVisibility(View.VISIBLE);
        android.widget.LinearLayout parent = (android.widget.LinearLayout) tilBudget.getParent();
        com.google.android.material.button.MaterialButton catalog = new com.google.android.material.button.MaterialButton(this);
        catalog.setText("Sản phẩm & tồn kho");
        catalog.setOnClickListener(v -> startActivity(new Intent(this, ProductActivity.class)));
        parent.addView(catalog, parent.indexOfChild(tilBudget));
        com.group14.expensemanager.data.TransactionRepository repository =
                com.group14.expensemanager.data.TransactionRepository.getInstance(getApplication());
        androidx.lifecycle.LiveData<java.util.List<com.group14.expensemanager.model.Product>> catalogData = repository.getProducts();
        // Keep the same observable instance captured by the renderer.
        catalogData.observe(this, products -> renderStock(products, repository.getTransactions().getValue()));
        repository.getTransactions().observe(this, movements -> renderStock(catalogData.getValue(), movements));
    }

    private void renderStock(java.util.List<com.group14.expensemanager.model.Product> products,
                             java.util.List<com.group14.expensemanager.model.Transaction> movements) {
        if (products == null || movements == null) return;
        int low = 0; double value = 0; StringBuilder warnings = new StringBuilder();
        java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        for (com.group14.expensemanager.model.Product p : products) {
            double stock = com.group14.expensemanager.utils.StockCalculator.stock(p.id, movements);
            value += stock * p.referencePrice;
            if (stock <= p.minimumStock) {
                low++; warnings.append(p.name).append(": ").append(format.format(stock)).append(" ").append(p.unit)
                        .append(" (tối thiểu ").append(format.format(p.minimumStock)).append(")\n");
            }
        }
        mtvTotal.setText(format.format(value) + "đ");
        mtvBudgetInfo.setText("Danh mục: " + products.size() + " sản phẩm");
        mtvBudgetRemaining.setText(low + " sản phẩm cần nhập thêm");
        mtvBudgetWarning.setVisibility(View.VISIBLE);
        mtvBudgetWarning.setText(products.isEmpty() ? "Thêm sản phẩm và tạo phiếu nhập để bắt đầu."
                : low == 0 ? "Tất cả sản phẩm đều trên mức tồn tối thiểu." : warnings.toString().trim());
    }

    private void setupLockScreen() {
        if (SecurityManager.isSessionAuthenticated()) {
            layoutLockScreen.setVisibility(View.GONE);
            return;
        }

        if (SecurityManager.isSecurityEnabled(this)) {
            layoutLockScreen.setVisibility(View.VISIBLE);
            if (SecurityManager.usePin(this)) {
                tilPin.setVisibility(View.VISIBLE);
                btnUnlock.setText("Xác nhận PIN");
                btnUnlock.setOnClickListener(v -> {
                    String input = edtLoginPin.getText() != null ? edtLoginPin.getText().toString().trim() : "";
                    String savedPin = SecurityManager.getSavedPin(this);
                    if (input.equals(savedPin)) {
                        SecurityManager.setAuthenticated(true);
                        layoutLockScreen.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "Sai mã PIN", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (SecurityManager.useFingerprint(this)) {
                tilPin.setVisibility(View.GONE);
                btnUnlock.setText("Mở khóa bằng vân tay");
                btnUnlock.setOnClickListener(v -> showBiometricPrompt());
                showBiometricPrompt();
            }
        } else {
            layoutLockScreen.setVisibility(View.GONE);
        }
    }

    private void showBiometricPrompt() {
        SecurityManager.authenticate(this, new SecurityManager.AuthCallback() {
            @Override
            public void onSuccess() {
                layoutLockScreen.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(String message) {
                if ("PIN_REQUIRED".equals(message)) {
                    tilPin.setVisibility(View.VISIBLE);
                    btnUnlock.setText("Xác nhận PIN");
                } else {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancel() {
                Toast.makeText(MainActivity.this, "Đã hủy xác thực", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupThemeSwitch() {
        mswTheme.setSaveEnabled(false);
        mswTheme.setChecked(ThemeManager.isDarkMode(this));
        mswTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingThemeSwitch) return;
            ThemeManager.setDarkMode(this, isChecked);
        });
    }

    private void setupNavigation() {
        bnvMain.setSelectedItemId(AppAssets.ICON_NAV_HOME);
        bnvMain.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == AppAssets.ICON_NAV_ADD) {
                startActivity(new Intent(this, AddActivity.class));
                return true;
            } else if (id == AppAssets.ICON_NAV_DETAIL) {
                startActivity(new Intent(this, DetailActivity.class));
                return true;
            } else if (id == AppAssets.ICON_NAV_REPORT) {
                startActivity(new Intent(this, ReportActivity.class));
                return true;
            }
            return true;
        });
        if (mbtnSettings != null) {
            mbtnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }
        if (fabAi != null) {
            fabAi.setOnClickListener(v -> startActivity(new Intent(this, AiChatActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mswTheme != null) {
            isUpdatingThemeSwitch = true;
            mswTheme.setChecked(ThemeManager.isDarkMode(this));
            isUpdatingThemeSwitch = false;
        }
        if (bnvMain != null) {
            bnvMain.setSelectedItemId(R.id.nav_home);
        }
    }
}
