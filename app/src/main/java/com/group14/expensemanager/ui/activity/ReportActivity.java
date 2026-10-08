package com.group14.expensemanager.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.group14.expensemanager.R;
import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.utils.AppAssets;
import com.group14.expensemanager.utils.ThemeManager;
import com.group14.expensemanager.viewmodel.ReportViewModel;

import com.github.mikephil.charting.components.LimitLine;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class ReportActivity extends AppCompatActivity {
    private TextView mtvTotal, mtvIncome, mtvExpense, mtvMonthYear, mtvChartTitle, mtvLineChartTitle, mtvAiAnalysis;
    private PieChart pcReport;
    private BarChart lcReport, lcCashflow;
    private SwitchCompat mswTheme;
    private Button mbtnSettings, mbtnPrevMonth, mbtnNextMonth;
    private FloatingActionButton fabAi;
    private MaterialButtonToggleGroup mbtgReportType, mbtgTimeRange, mbtgCashflowTimeRange;
    private Button mbtnExpenseReport, mbtnIncomeReport, mbtnWeek, mbtnMonth, mbtnCashflowWeek, mbtnCashflowMonth;
    private BottomNavigationView bnvMain;
    private boolean isUpdatingThemeSwitch = false;

    private ReportViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        mtvTotal = findViewById(R.id.mtv_total);
        mtvIncome = findViewById(R.id.mtv_income);
        mtvExpense = findViewById(R.id.mtv_expense);
        mtvMonthYear = findViewById(R.id.mtv_monthyear);
        mtvChartTitle = findViewById(R.id.mtv_chart_title);
        mtvLineChartTitle = findViewById(R.id.mtv_line_chart_title);
        mtvAiAnalysis = findViewById(R.id.mtv_ai_analysis);
        pcReport = findViewById(R.id.pc_report);
        lcReport = findViewById(R.id.lc_report);
        mswTheme = findViewById(R.id.msw_theme);
        mbtnSettings = findViewById(R.id.mbtn_settings);
        mbtnPrevMonth = findViewById(R.id.mbtn_prev_month);
        mbtnNextMonth = findViewById(R.id.mbtn_next_month);
        mbtgReportType = findViewById(R.id.mbtg_report_type);
        mbtnExpenseReport = findViewById(R.id.mbtn_expense_report);
        mbtnIncomeReport = findViewById(R.id.mbtn_income_report);
        mbtgTimeRange = findViewById(R.id.mbtg_time_range);
        mbtnWeek = findViewById(R.id.mbtn_week);
        mbtnMonth = findViewById(R.id.mbtn_month);

        lcCashflow = findViewById(R.id.lc_cashflow);
        mbtgCashflowTimeRange = findViewById(R.id.mbtg_cashflow_time_range);
        mbtnCashflowWeek = findViewById(R.id.mbtn_cashflow_week);
        mbtnCashflowMonth = findViewById(R.id.mbtn_cashflow_month);

        fabAi = findViewById(R.id.fab_ai);
        bnvMain = findViewById(R.id.bnv_main);

        setupThemeSwitch();
        observeViewModel();
        setupNavigation();

        mbtnPrevMonth.setOnClickListener(v -> viewModel.prevMonth());
        mbtnNextMonth.setOnClickListener(v -> viewModel.nextMonth());

        mbtgReportType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.mbtn_expense_report) {
                mbtnExpenseReport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_accent)));
                mbtnIncomeReport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_surface)));
            } else {
                mbtnIncomeReport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_accent)));
                mbtnExpenseReport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_surface)));
            }
            viewModel.setShowExpense(checkedId == R.id.mbtn_expense_report);
        });

        mbtgTimeRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.mbtn_week) {
                mbtnWeek.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_accent)));
                mbtnMonth.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_surface)));
            } else if (checkedId == R.id.mbtn_month) {
                mbtnMonth.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_accent)));
                mbtnWeek.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_surface)));
            }
            updateLineChart();
        });

        mbtgCashflowTimeRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.mbtn_cashflow_week) {
                mbtnCashflowWeek.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_accent)));
                mbtnCashflowMonth.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_surface)));
            } else if (checkedId == R.id.mbtn_cashflow_month) {
                mbtnCashflowMonth.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_accent)));
                mbtnCashflowWeek.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.ui_surface)));
            }
            updateCashflowChart();
        });

        mbtgTimeRange.check(R.id.mbtn_week);
        mbtgCashflowTimeRange.check(R.id.mbtn_cashflow_week);
    }

    private void observeViewModel() {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        viewModel.getSelectedMonth().observe(this, calendar -> {
            int month = calendar.get(Calendar.MONTH);
            int year = calendar.get(Calendar.YEAR);
            mtvMonthYear.setText(String.format(Locale.forLanguageTag("vi-VN"), "Tháng %d - %d", month + 1, year));
        });
        viewModel.getMonthlySummary().observe(this, data -> {
            mtvTotal.setText(formatter.format(data.total) + "đ");
            mtvIncome.setText(formatter.format(data.income) + "đ");
            mtvExpense.setText(formatter.format(data.expense) + "đ");
        });
        viewModel.getReportTransactions().observe(this, transactions -> {
            updatePieChart(transactions);
            updateLineChart();
            updateCashflowChart();
        });

        viewModel.getAiAnalysis().observe(this, analysis -> {
            if (analysis != null) mtvAiAnalysis.setText(analysis);
        });
    }

    private void updatePieChart(List<Transaction> transactions) {
        boolean export = Boolean.TRUE.equals(viewModel.isShowExpense().getValue());
        mtvChartTitle.setText(export ? "Giá trị xuất theo nhóm (VNĐ)" : "Giá trị nhập theo nhóm (VNĐ)");
        if (transactions == null || transactions.isEmpty()) { pcReport.clear(); return; }
        boolean isDark = ThemeManager.isDarkMode(this);
        int textColor = isDark ? Color.WHITE : Color.BLACK;

        Map<String, Double> categoryTotals = new HashMap<>();
        for (Transaction t : transactions) {
            Double current = categoryTotals.get(t.getCategory());
            categoryTotals.put(t.getCategory(), (current != null ? current : 0.0) + t.getAmount());
        }
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(AppAssets.CHART_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(textColor);

        PieData data = new PieData(dataSet);
        pcReport.setData(data);
        pcReport.setDrawEntryLabels(false);
        pcReport.getLegend().setEnabled(true);
        pcReport.getLegend().setTextColor(textColor);
        pcReport.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        pcReport.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pcReport.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pcReport.getLegend().setDrawInside(false);
        pcReport.getDescription().setEnabled(false);
        pcReport.invalidate();
    }

    private void updateLineChart() {
        mtvLineChartTitle.setText("Giá trị nhập–xuất theo tuần / tháng (VNĐ)");
        boolean isWeek = mbtgTimeRange.getCheckedButtonId() == R.id.mbtn_week;
        boolean showExpense = viewModel.isShowExpense().getValue() != null && viewModel.isShowExpense().getValue();
        List<Transaction> allTransactions = viewModel.getAllTransactions();
        if (allTransactions == null || allTransactions.isEmpty()) { lcReport.clear(); return; }
        
        boolean isDark = ThemeManager.isDarkMode(this);
        int textColor = isDark ? Color.WHITE : Color.BLACK;

        TreeMap<Long, Double> chartData = new TreeMap<>();
        for (Transaction t : allTransactions) {
            if (t.isIncome() == showExpense) continue;
            
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(t.getTimestamp());
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
            if (isWeek) { while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) c.add(Calendar.DAY_OF_MONTH, 1); }
            else c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            long time = c.getTimeInMillis();
            chartData.put(time, (chartData.get(time) != null ? chartData.get(time) : 0.0) + t.getAmount());
        }

        while (chartData.size() > 8) {
            chartData.pollFirstEntry();
        }
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        int index = 0;
        for (Map.Entry<Long, Double> entry : chartData.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(sdf.format(new Date(entry.getKey())));
            index++;
        }
        
        if (entries.isEmpty()) {
            lcReport.clear();
            return;
        }
        
        BarDataSet dataSet = new BarDataSet(entries, showExpense ? "Xuất kho" : "Nhập kho");
        dataSet.setColor(showExpense ? AppAssets.COLOR_EXPENSE : AppAssets.COLOR_INCOME);
        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new PrettyValueFormatter());

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        lcReport.setData(data);

        XAxis xAxis = lcReport.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textColor);
        
        lcReport.getAxisLeft().setTextColor(textColor);
        lcReport.getAxisLeft().setValueFormatter(new PrettyValueFormatter());
        lcReport.getAxisLeft().setAxisMinimum(0f);
        lcReport.getAxisLeft().setLabelCount(5, false);
        lcReport.getAxisLeft().setDrawGridLines(true);
        
        float maxVal = data.getYMax();
        if (maxVal > 0) {
            lcReport.getAxisLeft().setGranularity(maxVal / 5f);
            lcReport.getAxisLeft().setGranularityEnabled(true);
        }
        
        lcReport.getAxisRight().setEnabled(false);
        lcReport.getLegend().setTextColor(textColor);
        lcReport.getDescription().setEnabled(false);
        lcReport.invalidate();
    }

    private void updateCashflowChart() {
        boolean isWeek = mbtgCashflowTimeRange.getCheckedButtonId() == R.id.mbtn_cashflow_week;
        List<Transaction> allTransactions = viewModel.getAllTransactions();
        if (allTransactions == null || allTransactions.isEmpty()) {
            lcCashflow.clear();
            return;
        }

        boolean isDark = ThemeManager.isDarkMode(this);
        int textColor = isDark ? Color.WHITE : Color.BLACK;

        TreeMap<Long, Double> balanceData = new TreeMap<>();
        for (Transaction t : allTransactions) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(t.getTimestamp());
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
            if (isWeek) { while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) c.add(Calendar.DAY_OF_MONTH, 1); }
            else c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            
            long time = c.getTimeInMillis();
            double val = t.isIncome() ? t.getAmount() : -t.getAmount();
            balanceData.put(time, (balanceData.get(time) != null ? balanceData.get(time) : 0.0) + val);
        }

        while (balanceData.size() > 8) {
            balanceData.pollFirstEntry();
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

        int index = 0;
        for (Map.Entry<Long, Double> entry : balanceData.entrySet()) {
            float val = entry.getValue().floatValue();
            entries.add(new BarEntry(index, val));
            labels.add(sdf.format(new Date(entry.getKey())));
            
            if (val >= 0) colors.add(AppAssets.COLOR_INCOME);
            else colors.add(AppAssets.COLOR_EXPENSE);
            
            index++;
        }

        if (entries.isEmpty()) {
            lcCashflow.clear();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Chênh lệch giá trị (Nhập - Xuất)");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new PrettyValueFormatter());
        
        lcCashflow.getAxisLeft().removeAllLimitLines();
        LimitLine zeroLine = new LimitLine(0f, ""); 
        zeroLine.setLineColor(AppAssets.COLOR_NEUTRAL);
        zeroLine.setLineWidth(1.5f);
        zeroLine.enableDashedLine(10f, 10f, 0f);
        lcCashflow.getAxisLeft().addLimitLine(zeroLine);
        lcCashflow.getAxisLeft().setTextColor(textColor);
        lcCashflow.getAxisLeft().setValueFormatter(new PrettyValueFormatter());
        lcCashflow.getAxisLeft().setLabelCount(5, false);
        lcCashflow.getAxisLeft().setDrawGridLines(true);
        
        float maxAbsVal = Math.max(Math.abs(dataSet.getYMax()), Math.abs(dataSet.getYMin()));
        if (maxAbsVal > 0) {
            lcCashflow.getAxisLeft().setGranularity(maxAbsVal / 5f);
            lcCashflow.getAxisLeft().setGranularityEnabled(true);
        }

        lcCashflow.setData(new BarData(dataSet));
        XAxis xAxis = lcCashflow.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textColor);
        
        lcCashflow.getAxisRight().setEnabled(false);
        lcCashflow.getLegend().setTextColor(textColor);
        lcCashflow.getDescription().setEnabled(false);
        lcCashflow.invalidate();
    }

    private static class PrettyValueFormatter extends ValueFormatter {
        private final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

        @Override
        public String getFormattedValue(float value) {
            return formatter.format((long) value);
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

    private void setupNavigation() {
        if (mbtnSettings != null) {
            mbtnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }
        if (fabAi != null) {
            fabAi.setOnClickListener(v -> startActivity(new Intent(this, AiChatActivity.class)));
        }
        bnvMain.setSelectedItemId(AppAssets.ICON_NAV_REPORT);
        bnvMain.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == AppAssets.ICON_NAV_HOME) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == AppAssets.ICON_NAV_ADD) {
                startActivity(new Intent(this, AddActivity.class));
                return true;
            } else if (itemId == AppAssets.ICON_NAV_DETAIL) {
                startActivity(new Intent(this, DetailActivity.class));
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
        bnvMain.setSelectedItemId(R.id.nav_report);
    }
}
