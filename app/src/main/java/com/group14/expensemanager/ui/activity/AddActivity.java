package com.group14.expensemanager.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.group14.expensemanager.R;
import com.group14.expensemanager.data.TransactionRepository;
import com.group14.expensemanager.model.*;
import com.group14.expensemanager.ui.StockUi;
import com.group14.expensemanager.utils.*;
import java.io.Serializable;
import java.text.*;
import java.util.*;

/** A document editor: metadata and every line are saved atomically. */
public class AddActivity extends AppCompatActivity {
    private TransactionRepository repo;
    private List<Product> products=new ArrayList<>();
    private List<Partner> partners=new ArrayList<>(), choices=new ArrayList<>();
    private List<Transaction> movements=new ArrayList<>();
    private final List<LineRow> rows=new ArrayList<>();
    private List<Draft> drafts=new ArrayList<>();
    private LinearLayout lineContainer;
    private Spinner partnerSpinner;
    private EditText note;
    private TextView dateView,totalView,partnerLabel;
    private Button save;
    private MaterialButtonToggleGroup toggle;
    private String type=Transaction.TYPE_INCOME,editingKey;
    private long date=System.currentTimeMillis();
    private int partnerId;
    private String noteText="";
    private boolean loaded=true,binding=false,saving=false,partnersLoaded=false;
    private final NumberFormat money=NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

    public static class Draft implements Serializable {
        int productId; String quantity="1",price="";
        Draft() {}
        Draft(Transaction t) { productId=t.getProductId();quantity=String.valueOf(t.getQuantity());price=String.valueOf(t.getUnitPrice()); }
    }
    private class LineRow {
        Draft draft; Spinner product; EditText quantity,price; TextView stock,subtotal;
        List<Product> options;
        void capture() {
            int index=product.getSelectedItemPosition();
            if(index>=0 && index<options.size()) draft.productId=options.get(index).id;
            draft.quantity=quantity.getText().toString(); draft.price=price.getText().toString();
        }
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_add);
        repo=TransactionRepository.getInstance(getApplication());
        Transaction selected=(Transaction)getIntent().getSerializableExtra("TRANSACTION");
        if(selected!=null) { editingKey=StockDocuments.key(selected);type=selected.getType();date=selected.getTimestamp();loaded=false; }
        if(state!=null) {
            type=state.getString("type",type);date=state.getLong("date",date);partnerId=state.getInt("partner");
            noteText=state.getString("note","");editingKey=state.getString("key",editingKey);loaded=state.getBoolean("loaded",loaded);
            ArrayList<Draft> restored=(ArrayList<Draft>)state.getSerializable("drafts");if(restored!=null)drafts=restored;
        }
        if(drafts.isEmpty() && loaded) drafts.add(new Draft());
        save=findViewById(R.id.mbtn_save);dateView=findViewById(R.id.mtv_date);
        toggle=findViewById(R.id.mbtg_transaction_type);
        toggle.setSaveFromParentEnabled(false);
        buildFields();
        toggle.check(Transaction.TYPE_INCOME.equals(type)?R.id.mbtn_income:R.id.mbtn_expense);
        refreshMode();
        toggle.addOnButtonCheckedListener((group,id,checked)->{
            if(!checked)return;
            captureRows();
            type=id==R.id.mbtn_income?Transaction.TYPE_INCOME:Transaction.TYPE_EXPENSE;
            partnerId=0;refreshMode();
            // Outbound prices must be entered; never silently use a reference price.
            for(Draft d:drafts) d.price=Transaction.TYPE_INCOME.equals(type)?referencePrice(d.productId):"";
            renderRows();
        });
        dateView.setOnClickListener(v->showDatePicker());
        save.setOnClickListener(v->saveDocument());
        findViewById(R.id.mbtn_clear).setOnClickListener(v->clearForm());
        if(editingKey!=null) {
            save.setText("Cập nhật phiếu kho");findViewById(R.id.mbtn_clear).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.mtv_toolbar_title)).setText("Chỉnh sửa phiếu kho");
        }
        setupNavigation(); updateDate();updateTotal();
        repo.getProducts().observe(this,list->{captureRows();products=list;renderRows();updateEnabled();});
        repo.getPartners().observe(this,list->{partners=list;partnersLoaded=true;refreshPartners();});
        repo.getTransactions().observe(this,list->{
            movements=list;
            for(LineRow row:rows)updateStock(row);
        });
        if(!loaded) repo.loadDocument(editingKey,list->{
            if(isFinishing()||isDestroyed())return;
                drafts.clear();
                for(Transaction t:list) {
                    drafts.add(new Draft(t));partnerId=t.getPartnerId();noteText=t.getNote();type=t.getType();date=t.getTimestamp();
                }
                if(drafts.isEmpty()) { Toast.makeText(this,"Phiếu không còn tồn tại",Toast.LENGTH_LONG).show();finish();return; }
                loaded=true;note.setText(noteText);refreshPartners();updateDate();renderRows();updateEnabled();
        });
    }
    private void buildFields() {
        LinearLayout fields=findViewById(R.id.stock_document_fields);
        StockUi.button(fields,"Sản phẩm & tồn kho",()->startActivity(new Intent(this,ProductActivity.class)),true);
        partnerLabel=StockUi.text(this,"Nhà cung cấp (không bắt buộc)",14,R.color.ui_muted);fields.addView(partnerLabel);
        partnerSpinner=StockUi.spinner(fields);partnerSpinner.setId(R.id.stock_partner);
        partnerSpinner.setSaveEnabled(false);
        partnerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int position,long id){
                if(!binding && partnersLoaded && position==partnerSpinner.getSelectedItemPosition())partnerId=position<=0 || position>choices.size()?0:choices.get(position-1).id;
            }
        });
        StockUi.button(fields,"+ Quản lý khách hàng / nhà cung cấp",()->startActivity(new Intent(this,PartnerActivity.class)),true);
        note=StockUi.field(fields,"Ghi chú phiếu",noteText,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);note.setId(R.id.stock_note);note.setMaxLines(4);
        fields.addView(StockUi.text(this,"Sản phẩm trên phiếu",18,R.color.ui_text));
        lineContainer=StockUi.column(this);fields.addView(lineContainer);
        Button add=StockUi.button(fields,"+ Thêm dòng sản phẩm",()->{
            captureRows();drafts.add(new Draft());renderRows();
        },true);add.setId(R.id.stock_add_line);
        totalView=StockUi.text(this,"Tổng tiền: 0đ",22,R.color.ui_primary);totalView.setId(R.id.stock_total);fields.addView(totalView);
    }
    private String referencePrice(int id) { for(Product p:products)if(p.id==id)return String.valueOf(p.referencePrice);return ""; }
    private void captureRows() { for(LineRow row:rows)row.capture(); }
    private void renderRows() {
        rows.clear();lineContainer.removeAllViews();binding=true;
        for(int index=0;index<drafts.size();index++) {
            Draft draft=drafts.get(index);LineRow row=new LineRow();row.draft=draft;
            LinearLayout card=StockUi.card(lineContainer);card.addView(StockUi.text(this,"Dòng "+(index+1),14,R.color.ui_muted));
            row.product=StockUi.spinner(card);row.product.setId(R.id.stock_product);
            row.product.setSaveEnabled(false);
            row.options=new ArrayList<>(products);
            boolean found=false;for(Product p:row.options)if(p.id==draft.productId)found=true;
            if(draft.productId!=0&&!found) { Product missing=new Product();missing.id=draft.productId;missing.name="Sản phẩm không còn tồn tại";missing.sku="?";row.options.add(missing); }
            row.product.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,row.options));
            int position=0;for(int i=0;i<row.options.size();i++)if(row.options.get(i).id==draft.productId)position=i;
            if(!row.options.isEmpty()) {
                draft.productId=row.options.get(position).id;
                if(draft.price.isEmpty()&&Transaction.TYPE_INCOME.equals(type))draft.price=referencePrice(draft.productId);
                row.product.setSelection(position);
            }
            row.stock=StockUi.text(this,"",13,R.color.ui_muted);card.addView(row.stock);
            row.quantity=StockUi.field(card,"Số lượng *",draft.quantity,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);row.quantity.setId(R.id.stock_quantity);
            row.quantity.setSaveEnabled(false);
            row.price=StockUi.field(card,"Đơn giá (VNĐ / đơn vị) *",draft.price,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);row.price.setId(R.id.stock_price);
            row.price.setSaveEnabled(false);
            row.subtotal=StockUi.text(this,"",15,R.color.ui_primary);card.addView(row.subtotal);
            StockUi.button(card,"Bỏ dòng này",()->{captureRows();drafts.remove(draft);if(drafts.isEmpty())drafts.add(new Draft());renderRows();},true);
            rows.add(row);updateStock(row);
            row.product.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onNothingSelected(AdapterView<?> p){}
                public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                    if(pos<0||pos>=row.options.size())return;
                    int selected=row.options.get(pos).id;
                    if(selected!=draft.productId) {
                        draft.productId=selected;
                        row.price.setText(Transaction.TYPE_INCOME.equals(type)?referencePrice(selected):"");
                    }
                    updateStock(row);updateTotal();
                }
            });
            StockUi.watch(row.quantity,this::updateTotal);StockUi.watch(row.price,this::updateTotal);
        }
        binding=false;updateTotal();
    }
    private void updateStock(LineRow row) {
        for(Product p:products)if(p.id==row.draft.productId) {
            row.stock.setText("Tồn hiện tại: "+money.format(StockCalculator.stock(p.id,movements))+" "+p.unit);return;
        }
        row.stock.setText("Chưa có sản phẩm. Mở Sản phẩm & tồn kho để thêm.");
    }
    private void refreshMode() {
        findViewById(R.id.mbtn_income).setBackgroundTintList(android.content.res.ColorStateList.valueOf(StockUi.color(this,Transaction.TYPE_INCOME.equals(type)?R.color.ui_accent:R.color.ui_surface)));
        findViewById(R.id.mbtn_expense).setBackgroundTintList(android.content.res.ColorStateList.valueOf(StockUi.color(this,Transaction.TYPE_EXPENSE.equals(type)?R.color.ui_accent:R.color.ui_surface)));
        refreshPartners();
    }
    private void refreshPartners() {
        if(partnerSpinner==null)return;
        boolean incoming=Transaction.TYPE_INCOME.equals(type);
        partnerLabel.setText((incoming?"Nhà cung cấp":"Khách hàng")+" (không bắt buộc)");
        binding=true;choices=new ArrayList<>();List<String> labels=new ArrayList<>();labels.add("Chưa chọn "+(incoming?"nhà cung cấp":"khách hàng"));
        int selected=0;
        for(Partner p:partners)if((incoming?Partner.SUPPLIER:Partner.CUSTOMER).equals(p.type)) {
            choices.add(p);labels.add(p.toString());if(p.id==partnerId)selected=choices.size();
        }
        // Do not silently erase an id while LiveData is loading.
        partnerSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));partnerSpinner.setSelection(selected);binding=false;
    }
    private void updateTotal() {
        if(totalView==null)return;
        double total=0;boolean valid=true;
        for(LineRow row:rows) {
            try {
                double q=number(row.quantity),p=number(row.price),sum=q*p;
                if(q<=0||p<0||!Double.isFinite(sum))throw new NumberFormatException();
                total+=sum;row.subtotal.setText("Thành tiền: "+money.format(sum)+"đ");
            }catch(Exception e){valid=false;row.subtotal.setText("Nhập số lượng và đơn giá để tính tiền");}
        }
        totalView.setText(valid&&Double.isFinite(total)?"Tổng tiền: "+money.format(total)+"đ":"Tổng tiền: chờ nhập đủ thông tin");
    }
    private double number(EditText field) { return Double.parseDouble(field.getText().toString().trim().replace(',','.')); }
    private void saveDocument() {
        if(!loaded||saving)return;captureRows();List<Transaction> lines=new ArrayList<>();
        try {
            for(LineRow row:rows) {
                if(row.draft.productId==0)throw new IllegalArgumentException("Hãy thêm và chọn sản phẩm.");
                double quantity=number(row.quantity),price=number(row.price);
                if(!Double.isFinite(quantity)||quantity<=0||!Double.isFinite(price)||price<0)throw new IllegalArgumentException("Số lượng phải lớn hơn 0, đơn giá không được âm.");
                Transaction t=new Transaction("","",0,type,date);t.setProductId(row.draft.productId);t.setQuantity(quantity);t.setUnitPrice(price);lines.add(t);
            }
        }catch(NumberFormatException e){Toast.makeText(this,"Nhập đủ số lượng và đơn giá cho mọi dòng hàng",Toast.LENGTH_LONG).show();return;}
        catch(IllegalArgumentException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();return;}
        saving=true;updateEnabled();toggle.setEnabled(false);
        repo.saveDocument(editingKey,type,date,partnerId,note.getText().toString(),lines,error->{
            saving=false;updateEnabled();toggle.setEnabled(true);
            if(error!=null){Toast.makeText(this,error,Toast.LENGTH_LONG).show();return;}
            Toast.makeText(this,"Đã lưu phiếu "+lines.size()+" dòng sản phẩm",Toast.LENGTH_SHORT).show();
            if(editingKey!=null)finish();else clearForm();
        });
    }
    private void updateEnabled(){save.setEnabled(loaded&&!saving&&!products.isEmpty());}
    private void clearForm(){drafts.clear();drafts.add(new Draft());partnerId=0;note.setText("");date=System.currentTimeMillis();updateDate();refreshPartners();renderRows();}
    private void updateDate(){dateView.setText("Ngày phiếu: "+new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date(date)));}
    private void showDatePicker(){
        Calendar local=Calendar.getInstance();local.setTimeInMillis(date);
        Calendar utc=Calendar.getInstance(TimeZone.getTimeZone("UTC"));utc.clear();utc.set(local.get(Calendar.YEAR),local.get(Calendar.MONTH),local.get(Calendar.DAY_OF_MONTH));
        MaterialDatePicker<Long> picker=MaterialDatePicker.Builder.datePicker().setTitleText("Ngày phiếu kho").setSelection(utc.getTimeInMillis()).build();
        picker.addOnPositiveButtonClickListener(value->{
            utc.setTimeInMillis(value);Calendar chosen=Calendar.getInstance();chosen.clear();chosen.set(utc.get(Calendar.YEAR),utc.get(Calendar.MONTH),utc.get(Calendar.DAY_OF_MONTH));date=chosen.getTimeInMillis();updateDate();
        });picker.show(getSupportFragmentManager(),"DATE_PICKER");
    }
    private void setupNavigation(){
        SwitchCompat theme=findViewById(R.id.msw_theme);theme.setSaveEnabled(false);theme.setChecked(ThemeManager.isDarkMode(this));theme.setOnCheckedChangeListener((b,checked)->ThemeManager.setDarkMode(this,checked));
        findViewById(R.id.mbtn_settings).setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        findViewById(R.id.fab_ai).setOnClickListener(v->startActivity(new Intent(this,AiChatActivity.class)));
        BottomNavigationView nav=findViewById(R.id.bnv_main);nav.setSelectedItemId(R.id.nav_add);
        nav.setOnItemSelectedListener(item->{
            if(item.getItemId()==R.id.nav_home)startActivity(new Intent(this,MainActivity.class));
            else if(item.getItemId()==R.id.nav_detail)startActivity(new Intent(this,DetailActivity.class));
            else if(item.getItemId()==R.id.nav_report)startActivity(new Intent(this,ReportActivity.class));
            return true;
        });
    }
    @Override protected void onSaveInstanceState(Bundle state){
        captureRows();state.putSerializable("drafts",new ArrayList<>(drafts));state.putString("type",type);state.putLong("date",date);state.putInt("partner",partnerId);
        state.putString("note",note.getText().toString());state.putString("key",editingKey);state.putBoolean("loaded",loaded);super.onSaveInstanceState(state);
    }
}
