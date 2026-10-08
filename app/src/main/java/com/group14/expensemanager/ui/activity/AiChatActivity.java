package com.group14.expensemanager.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.group14.expensemanager.R;
import com.group14.expensemanager.model.ChatMessage;
import com.group14.expensemanager.ui.adapter.ChatAdapter;
import com.group14.expensemanager.utils.AppAssets;
import com.group14.expensemanager.utils.ThemeManager;
import com.group14.expensemanager.viewmodel.AiChatViewModel;

public class AiChatActivity extends AppCompatActivity {
    private SwitchCompat mswTheme;
    private Button mbtnSettings, mbtnSend;
    private FloatingActionButton fabAi;
    private TextInputEditText tietMessage;
    private RecyclerView rvChats;
    private ChatAdapter chatAdapter;
    private AiChatViewModel viewModel;
    private boolean isUpdatingThemeSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        viewModel = new ViewModelProvider(this).get(AiChatViewModel.class);

        mswTheme = findViewById(R.id.msw_theme);
        mbtnSettings = findViewById(R.id.mbtn_settings);
        fabAi = findViewById(R.id.fab_ai);
        mbtnSend = findViewById(R.id.mbtn_send);
        tietMessage = findViewById(R.id.tiet_message);
        rvChats = findViewById(R.id.rv_chats);

        if (fabAi != null) fabAi.setVisibility(View.GONE);

        setupRecyclerView();
        setupThemeSwitch();
        setupNavigation();
        observeViewModel();

        mbtnSend.setOnClickListener(v -> {
            String text = tietMessage.getText().toString();
            viewModel.sendMessage(text);
            tietMessage.setText("");
        });
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        rvChats.setAdapter(chatAdapter);
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(this, messages -> {
            if (messages != null) {
                chatAdapter.setMessages(new java.util.ArrayList<>(messages));
                if (chatAdapter.getItemCount() > 0) {
                    rvChats.scrollToPosition(chatAdapter.getItemCount() - 1);
                }
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            mbtnSend.setEnabled(!isLoading);
            if (isLoading) mbtnSend.setText("...");
            else mbtnSend.setText("Gửi");
        });
    }

    private void setupThemeSwitch() {
        if (mswTheme != null) {
            mswTheme.setSaveEnabled(false);
            mswTheme.setChecked(ThemeManager.isDarkMode(this));
            mswTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdatingThemeSwitch) return;
                ThemeManager.setDarkMode(this, isChecked);
            });
        }
    }

    private void setupNavigation() {
        if (mbtnSettings != null) {
            mbtnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }
        BottomNavigationView bnvMain = findViewById(R.id.bnv_main);
        if (bnvMain != null) {
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
                } else if (itemId == AppAssets.ICON_NAV_REPORT) {
                    startActivity(new Intent(this, ReportActivity.class));
                    return true;
                }
                return true;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isUpdatingThemeSwitch = true;
        if (mswTheme != null) mswTheme.setChecked(ThemeManager.isDarkMode(this));
        isUpdatingThemeSwitch = false;
    }
}
