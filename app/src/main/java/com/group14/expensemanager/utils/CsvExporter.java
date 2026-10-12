package com.group14.expensemanager.utils;

import android.content.Context;
import com.group14.expensemanager.model.Transaction;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CsvExporter {

    public static File exportToExcel(Context context, List<Transaction> transactions) {
        String month = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());
        File file = new File(context.getExternalFilesDir(null), "Bao-cao-kho-" + month + ".xls");

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("<html><head><meta charset='utf-8'></head><body>");
            writer.write("<table border='1'>");
            writer.write("<tr style='background-color: #8AC9FF; font-weight: bold;'>");
            writer.write("<td>Mã phiếu</td><td>Ngày</td><td>Đối tác</td><td>Điện thoại</td><td>Ghi chú</td><td>Mã hàng</td><td>Sản phẩm</td><td>Nhóm sản phẩm</td><td>Loại</td><td>Số lượng</td><td>Đơn vị</td><td>Đơn giá (VNĐ)</td><td>Thành tiền (VNĐ)</td>");
            writer.write("</tr>");

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            for (Transaction t : transactions) {
                String type = t.isIncome() ? "Nhập kho" : "Xuất kho";
                String color = t.isIncome() ? "#22C55E" : "#FF4444";
                
                writer.write("<tr>");
                writer.write("<td>" + escape(StockDocuments.number(t)) + "</td>");
                writer.write("<td>" + sdf.format(new Date(t.getTimestamp())) + "</td>");
                writer.write("<td>" + escape(t.getPartnerName()) + "</td><td>" + escape(t.getPartnerPhone()) + "</td><td>" + escape(t.getNote()) + "</td>");
                writer.write("<td>" + escape(t.getSku()) + "</td>");
                writer.write("<td>" + escape(t.getTitle()) + "</td>");
                writer.write("<td>" + escape(t.getCategory()) + "</td>");
                writer.write("<td style='color: " + color + "'>" + type + "</td>");
                writer.write("<td>" + t.getQuantity() + "</td>");
                writer.write("<td>" + escape(t.getUnit()) + "</td>");
                writer.write("<td>" + t.getUnitPrice() + "</td>");
                writer.write("<td>" + String.format(Locale.US, "%.0f", t.getAmount()) + "</td>");
                writer.write("</tr>");
            }

            writer.write("</table></body></html>");
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
