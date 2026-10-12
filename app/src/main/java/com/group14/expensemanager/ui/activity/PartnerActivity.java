package com.group14.expensemanager.ui.activity;

import android.os.Bundle;
import android.content.Intent;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.*;
import com.group14.expensemanager.R;
import com.group14.expensemanager.data.TransactionRepository;
import com.group14.expensemanager.model.Partner;
import com.group14.expensemanager.ui.StockUi;
import java.util.*;

public class PartnerActivity extends AppCompatActivity {
    private TransactionRepository repo;
    private List<Partner> partners = new ArrayList<>();
    private LinearLayout rows;
    private EditText search;
    private Spinner filter;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); repo=TransactionRepository.getInstance(getApplication());
        LinearLayout root=StockUi.column(this); root.setBackgroundColor(StockUi.color(this,R.color.ui_background));
        int pad=StockUi.dp(this,16); root.setPadding(pad,pad,pad,pad);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(pad+bars.left,pad+bars.top,pad+bars.right,pad+bars.bottom); return insets;
        });
        StockUi.button(root,"‹ Quay lại",this::finish,true);
        root.addView(StockUi.text(this,"Khách hàng & nhà cung cấp",24,R.color.ui_text));
        search=StockUi.field(root,"Tìm tên hoặc số điện thoại","",InputType.TYPE_CLASS_TEXT); search.setSingleLine(true);
        filter=StockUi.spinner(root); filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Tất cả đối tác","Khách hàng","Nhà cung cấp"}));
        StockUi.button(root,"+ Thêm đối tác",()->showForm(null),false);
        ScrollView scroll=new ScrollView(this); rows=StockUi.column(this); scroll.addView(rows); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        StockUi.watch(search,this::render);
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> p) {}
            public void onItemSelected(AdapterView<?> p,android.view.View v,int pos,long id) { render(); }
        });
        repo.getPartners().observe(this,list->{partners=list;render();});
    }
    private void render() {
        rows.removeAllViews(); String query=search.getText().toString().trim().toLowerCase(Locale.ROOT); int count=0;
        for (Partner p:partners) {
            if (!(p.name+" "+p.phone).toLowerCase(Locale.ROOT).contains(query)) continue;
            if (filter.getSelectedItemPosition()==1 && !Partner.CUSTOMER.equals(p.type)) continue;
            if (filter.getSelectedItemPosition()==2 && !Partner.SUPPLIER.equals(p.type)) continue;
            count++; LinearLayout card=StockUi.card(rows);
            card.addView(StockUi.text(this,p.name,19,R.color.ui_text));
            card.addView(StockUi.text(this,(Partner.CUSTOMER.equals(p.type)?"Khách hàng":"Nhà cung cấp")+"\n"+(p.phone.isEmpty()?"Chưa có số điện thoại":p.phone)+(p.address.isEmpty()?"":"\n"+p.address),14,R.color.ui_muted));
            StockUi.button(card,"Xem phiếu của đối tác",()->{
                Intent i=new Intent(this,DetailActivity.class); i.putExtra("PARTNER_ID",p.id); i.putExtra("PARTNER_NAME",p.name); startActivity(i);
            },true);
            LinearLayout actions=new LinearLayout(this);card.addView(actions);
            com.google.android.material.button.MaterialButton edit=new com.google.android.material.button.MaterialButton(this);edit.setText("Sửa");
            edit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));edit.setTextColor(StockUi.color(this,R.color.ui_primary));edit.setOnClickListener(v->showForm(p));actions.addView(edit,new LinearLayout.LayoutParams(0,-2,1));
            com.google.android.material.button.MaterialButton delete=new com.google.android.material.button.MaterialButton(this);delete.setText("Xóa");
            delete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));delete.setTextColor(StockUi.color(this,R.color.expense_red));
            delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Xóa đối tác?").setMessage(p.name)
                    .setNegativeButton("Hủy",null).setPositiveButton("Xóa",(d,w)->repo.deletePartner(p,error->Toast.makeText(this,error==null?"Đã xóa đối tác":error,Toast.LENGTH_LONG).show())).show());actions.addView(delete,new LinearLayout.LayoutParams(0,-2,1));
        }
        if(count==0) rows.addView(StockUi.text(this,"Chưa có đối tác phù hợp. Bấm + Thêm đối tác để tạo.",15,R.color.ui_muted));
    }
    private void showForm(Partner existing) {
        LinearLayout form=StockUi.column(this); int pad=StockUi.dp(this,20); form.setPadding(pad,pad,pad,pad);
        form.addView(StockUi.text(this,"Loại đối tác",14,R.color.ui_muted));
        Spinner type=StockUi.spinner(form); type.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Khách hàng","Nhà cung cấp"}));
        if(existing!=null) type.setSelection(Partner.SUPPLIER.equals(existing.type)?1:0);
        EditText name=StockUi.field(form,"Tên đối tác *",existing==null?"":existing.name,InputType.TYPE_CLASS_TEXT);
        EditText phone=StockUi.field(form,"Số điện thoại",existing==null?"":existing.phone,InputType.TYPE_CLASS_PHONE);
        EditText address=StockUi.field(form,"Địa chỉ",existing==null?"":existing.address,InputType.TYPE_CLASS_TEXT);
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"Thêm đối tác":"Sửa đối tác").setView(scroll).setNegativeButton("Hủy",null).setPositiveButton("Lưu",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            Partner p=new Partner();p.id=existing==null?0:existing.id;p.name=name.getText().toString();p.phone=phone.getText().toString();p.address=address.getText().toString();p.type=type.getSelectedItemPosition()==0?Partner.CUSTOMER:Partner.SUPPLIER;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            repo.savePartner(p,error->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);if(error==null)dialog.dismiss();else Toast.makeText(this,error,Toast.LENGTH_LONG).show();});
        }));dialog.show();
    }
}
