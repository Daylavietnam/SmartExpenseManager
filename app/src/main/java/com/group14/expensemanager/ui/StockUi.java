package com.group14.expensemanager.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.*;
import com.group14.expensemanager.R;

public final class StockUi {
    public static int dp(Context c, int n) { return Math.round(n * c.getResources().getDisplayMetrics().density); }
    public static int color(Context c, int id) { return ContextCompat.getColor(c, id); }
    public static LinearLayout column(Context c) { LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v; }
    public static TextView text(Context c, String value, int size, int color) {
        TextView v = new TextView(c); v.setText(value); v.setTextSize(size); v.setTextColor(color(c, color)); v.setLineSpacing(dp(c, 3), 1); return v;
    }
    public static EditText field(LinearLayout parent, String hint, String value, int inputType) {
        Context c = parent.getContext();
        TextInputLayout box = new TextInputLayout(c); box.setHint(hint);
        box.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE); box.setBoxCornerRadii(dp(c,14),dp(c,14),dp(c,14),dp(c,14));
        EditText edit = new TextInputEditText(c); edit.setInputType(inputType); edit.setTextSize(15); edit.setMinHeight(dp(c,56)); edit.setBackground(null);
        edit.setPadding(dp(c,16),dp(c,14),dp(c,16),dp(c,14));
        edit.setTextColor(color(c,R.color.ui_text)); edit.setText(value);
        box.addView(edit, new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(c,12); parent.addView(box,p); return edit;
    }
    public static MaterialButton button(LinearLayout parent, String title, Runnable action, boolean secondary) {
        Context c = parent.getContext(); MaterialButton b = new MaterialButton(c); b.setText(title);
        if (secondary) { b.setBackgroundTintList(ColorStateList.valueOf(color(c,R.color.ui_accent))); b.setTextColor(color(c,R.color.ui_primary)); }
        b.setOnClickListener(v -> action.run()); parent.addView(b,new LinearLayout.LayoutParams(-1,-2)); return b;
    }
    public static LinearLayout card(LinearLayout parent) {
        Context c=parent.getContext(); MaterialCardView card=new MaterialCardView(c); card.setRadius(dp(c,20)); card.setCardElevation(0);
        card.setStrokeWidth(dp(c,1)); card.setStrokeColor(color(c,R.color.ui_border)); card.setCardBackgroundColor(color(c,R.color.ui_surface));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(c,12); parent.addView(card,p);
        LinearLayout content=column(c); content.setPadding(dp(c,16),dp(c,16),dp(c,16),dp(c,12)); card.addView(content); return content;
    }
    public static void watch(EditText edit, Runnable action) {
        edit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { action.run(); }
            public void afterTextChanged(Editable s) {}
        });
    }
    public static Spinner spinner(LinearLayout parent) {
        Context c=parent.getContext(); Spinner s=new Spinner(c); s.setBackgroundResource(R.drawable.bg_stock_field);
        parent.addView(s,new LinearLayout.LayoutParams(-1,dp(c,56))); return s;
    }
}
