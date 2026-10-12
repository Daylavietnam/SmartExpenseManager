package com.group14.expensemanager.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.group14.expensemanager.R;
import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.ui.adapter.TransactionAdapter;
import com.group14.expensemanager.utils.AppAssets;
import com.group14.expensemanager.utils.ThemeManager;
import com.group14.expensemanager.viewmodel.DetailViewModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {
    private SwitchCompat mswTheme;
    private boolean isUpdatingThemeSwitch = false;
    private Button mbtnSettings, mbtnFilter;
    private FloatingActionButton fabAi;
    private TextView mtvFilterLabel, mtvTotal, mtvIncome, mtvExpense;
    private RecyclerView rvTransactions;
    private TransactionAdapter transactionAdapter;
    private BottomNavigationView bnvMain;

    private DetailViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);

        mswTheme = findViewById(R.id.msw_theme);
        mbtnSettings = findViewById(R.id.mbtn_settings);
        mbtnFilter = findViewById(R.id.mbtn_filter);
        mtvFilterLabel = findViewById(R.id.mtv_filter_label);
        mtvTotal = findViewById(R.id.mtv_total);
        mtvIncome = findViewById(R.id.mtv_income);
        mtvExpense = findViewById(R.id.mtv_expense);
        rvTransactions = findViewById(R.id.rv_transactions);
        fabAi = findViewById(R.id.fab_ai);
        bnvMain = findViewById(R.id.bnv_main);

        setupRecyclerView();
        setupThemeSwitch();
        observeViewModel();
        setupNavigation();
        android.widget.EditText search = findViewById(R.id.stock_search_documents);
        com.group14.expensemanager.ui.StockUi.watch(search, () -> viewModel.setSearch(search.getText().toString()));
        viewModel.setPartner(getIntent().getIntExtra("PARTNER_ID", 0));
        if (getIntent().getIntExtra("PARTNER_ID", 0) != 0) {
            search.setHint("Phiếu của " + getIntent().getStringExtra("PARTNER_NAME"));
            ((TextView) findViewById(R.id.mtv_toolbar_title)).setText(getIntent().getStringExtra("PARTNER_NAME"));
        }

        mbtnFilter.setOnClickListener(v -> showFilterMenu());
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(new TransactionAdapter.OnTransactionActionListener() {
            @Override
            public void onEdit(Transaction transaction) {
                Intent intent = new Intent(DetailActivity.this, AddActivity.class);
                intent.putExtra("TRANSACTION", transaction);
                startActivity(intent);
            }

            @Override
            public void onDelete(Transaction transaction) {
                showDeleteConfirmation(transaction);
            }
        });
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
    }

    private void showDeleteConfirmation(Transaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Xóa toàn bộ phiếu " + com.group14.expensemanager.utils.StockDocuments.number(transaction) + " và tất cả dòng sản phẩm? Tồn kho sẽ được tính lại.")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    com.group14.expensemanager.data.TransactionRepository.getInstance(getApplication())
                            .deleteMovement(transaction, error -> Toast.makeText(this,
                                    error == null ? "Đã xóa phiếu kho" : error, Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void observeViewModel() {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        viewModel.getFilteredTransactions().observe(this, transactions -> {
            transactionAdapter.setTransactions(transactions);
        });
        viewModel.getSummaryData().observe(this, data -> {
            mtvTotal.setText(formatter.format(data.total) + "đ");
            mtvIncome.setText(formatter.format(data.income) + "đ");
            mtvExpense.setText(formatter.format(data.expense) + "đ");
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

    private void showFilterMenu() {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, mbtnFilter);
        popup.getMenu().add("Ngày");
        popup.getMenu().add("Tuần");
        popup.getMenu().add("Tháng");
        popup.getMenu().add("Toàn bộ");
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Ngày")) {
                showDatePicker(DetailViewModel.FilterType.DAY);
            } else if (title.equals("Tuần")) {
                showDatePicker(DetailViewModel.FilterType.WEEK);
            } else if (title.equals("Tháng")) {
                showMonthPicker();
            } else if (title.equals("Toàn bộ")) {
                viewModel.setFilter(DetailViewModel.FilterType.ALL);
                mtvFilterLabel.setText("Thời gian lọc: Toàn bộ");
            }
            return true;
        });
        popup.show();
    }

    private void showDatePicker(DetailViewModel.FilterType type) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Chọn ngày").build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);
            viewModel.setFilterDate(calendar);
            viewModel.setFilter(type);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            mtvFilterLabel.setText("Thời gian lọc: " + (type == DetailViewModel.FilterType.DAY ? "Ngày " : "Tuần chứa ngày ") + sdf.format(new Date(selection)));
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showMonthPicker() {
        final String[] months = {"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn tháng")
                .setItems(months, (dialog, which) -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.MONTH, which);
                    viewModel.setFilterDate(calendar);
                    viewModel.setFilter(DetailViewModel.FilterType.MONTH);
                    mtvFilterLabel.setText("Thời gian lọc: " + months[which]);
                })
                .show();
    }

    private void setupNavigation() {
        if (mbtnSettings != null) {
            mbtnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }
        if (fabAi != null) {
            fabAi.setOnClickListener(v -> startActivity(new Intent(this, AiChatActivity.class)));
        }
        bnvMain.setSelectedItemId(AppAssets.ICON_NAV_DETAIL);
        bnvMain.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == AppAssets.ICON_NAV_HOME) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == AppAssets.ICON_NAV_ADD) {
                startActivity(new Intent(this, AddActivity.class));
                return true;
            } else if (itemId == AppAssets.ICON_NAV_REPORT) {
                startActivity(new Intent(this, ReportActivity.class));
                return true;
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isUpdatingThemeSwitch = true;
        mswTheme.setChecked(ThemeManager.isDarkMode(this));
        isUpdatingThemeSwitch = false;
        bnvMain.setSelectedItemId(R.id.nav_detail);
    }
}
