package com.group14.expensemanager;

import android.app.Application;

import com.group14.expensemanager.utils.ThemeManager;

public class QLCTApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applyTheme(ThemeManager.isDarkMode(this));
    }
}
