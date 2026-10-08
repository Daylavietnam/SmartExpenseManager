package com.group14.expensemanager.utils;

import com.group14.expensemanager.R;
import java.util.ArrayList;
import java.util.List;

public class AppAssets {

    public static class CategoryItem {
        public final String name;
        public final int iconRes;
        public final int colorRes;

        public CategoryItem(String name, int iconRes, int colorRes) {
            this.name = name;
            this.iconRes = iconRes;
            this.colorRes = colorRes;
        }
    }

    public static final List<CategoryItem> EXPENSE_CATEGORIES = new ArrayList<CategoryItem>() {{
        add(new CategoryItem("Thực phẩm", R.drawable.ic_launcher_foreground, R.color.expense_red));
        add(new CategoryItem("Đồ uống", R.drawable.ic_launcher_foreground, R.color.purple_primary));
        add(new CategoryItem("Gia dụng", R.drawable.ic_launcher_foreground, R.color.purple_200));
        add(new CategoryItem("Điện tử", R.drawable.ic_launcher_foreground, R.color.teal_700));
        add(new CategoryItem("Văn phòng phẩm", R.drawable.ic_launcher_foreground, R.color.purple_700));
        add(new CategoryItem("Nguyên liệu", R.drawable.ic_launcher_foreground, R.color.income_green));
        add(new CategoryItem("Thời trang", R.drawable.ic_launcher_foreground, R.color.purple_500));
        add(new CategoryItem("Khác", R.drawable.ic_launcher_foreground, R.color.card_secondary_text));
    }};

    public static final List<CategoryItem> INCOME_CATEGORIES = EXPENSE_CATEGORIES;


    public static final int ICON_EDIT = R.drawable.ic_edit;
    public static final int ICON_CHECK = R.drawable.ic_check;
    public static final int ICON_AI_CHAT = R.drawable.ic_ai_chat;
    public static final int ICON_SETTINGS = R.drawable.ic_settings;
    public static final int ICON_FILTER = R.drawable.ic_filter;


    public static final int ICON_NAV_HOME = R.id.nav_home;
    public static final int ICON_NAV_ADD = R.id.nav_add;
    public static final int ICON_NAV_REPORT = R.id.nav_report;
    public static final int ICON_NAV_DETAIL = R.id.nav_detail;


    public static final int COLOR_INCOME = 0xFF22C55E;
    public static final int COLOR_EXPENSE = 0xFFFF4444;
    public static final int COLOR_NEUTRAL = android.graphics.Color.GRAY;
    public static final int COLOR_PRIMARY_LIGHT = 0xFF8AC9FF;
    public static final int COLOR_NORMAL_GRAY = 0xFFDDDDDD;

    // màu cho biểu đồm lần lượt là: xanh ls, xanh dương, vàng, cam, tím, đỏ, xám, hơi trăng tí tẹo)
    public static final int[] CHART_COLORS = {
            0xFF4CAF50, 0xFF2196F3, 0xFFFFEB3B, 0xFFFF9800, 
            0xFF9C27B0, 0xFFF44336, 0xFF9E9E9E, 0xFFF5F5F5
    };

    public static int getCategoryColor(String categoryName) {
        for (CategoryItem item : EXPENSE_CATEGORIES) {
            if (item.name.equals(categoryName)) return item.colorRes;
        }
        for (CategoryItem item : INCOME_CATEGORIES) {
            if (item.name.equals(categoryName)) return item.colorRes;
        }
        return R.color.card_secondary_text;
    }

    public static int getCategoryIcon(String categoryName) {
        for (CategoryItem item : EXPENSE_CATEGORIES) {
            if (item.name.equals(categoryName)) return item.iconRes;
        }
        for (CategoryItem item : INCOME_CATEGORIES) {
            if (item.name.equals(categoryName)) return item.iconRes;
        }
        return R.drawable.ic_launcher_foreground;
    }
}
