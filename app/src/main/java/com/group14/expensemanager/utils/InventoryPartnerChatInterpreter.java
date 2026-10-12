package com.group14.expensemanager.utils;

import com.group14.expensemanager.model.*;
import java.util.*;
import java.util.regex.*;

/** Resolve contacts separately so names/phone numbers never become quantities or prices. */
public final class InventoryPartnerChatInterpreter {
    private static final Pattern LABEL = Pattern.compile("\\b(?:(?:tu|cho|cua|ben)\\s+)?(nha cung cap|ncc|khach hang|kh)\\b\\s*[:=]?\\s*");
    private static final Pattern ACTION = Pattern.compile("\\b(nhap|them|xuat|suat|ban)\\b");
    public static class Result {
        public final boolean handled;
        public final InventoryChatInterpreter.Draft draft;
        public final Product product;
        public final String reply;
        Result(boolean handled, InventoryChatInterpreter.Draft draft, Product product, String reply) {
            this.handled=handled;this.draft=draft;this.product=product;this.reply=reply;
        }
        public boolean ready() { return handled && reply==null && product!=null && draft!=null && draft.partnerId!=0
                && draft.quantity!=null && (Transaction.TYPE_INCOME.equals(draft.type)||draft.unitPrice!=null); }
    }
    public static Result interpret(String content,List<Product> products,List<Partner> partners,InventoryChatInterpreter.Draft pending) {
        String text=InventoryChatInterpreter.normalize(content);
        if(pending!=null && (text.equals("huy")||text.equals("bo qua")||text.equals("thoi")||text.equals("khong")))
            return convert(InventoryChatInterpreter.interpret(content,products,pending));
        Matcher label=LABEL.matcher(text);
        String answer=null,role=null,stockText=text;
        boolean namedAnswer=pending!=null && pending.awaitingPartner && partners!=null && !find(text,partners,pending.type).isEmpty();
        if(namedAnswer) { answer=text;stockText=""; }
        else if(label.find()) {
            int start=label.start(),end=label.end();role=label.group(1);
            Matcher boundary=Pattern.compile("[,;\\n]|\\b(?:don gia|gia)\\b").matcher(text);boundary.region(end,text.length());
            int stop=boundary.find()?boundary.start():text.length();
            answer=text.substring(end,stop).trim();stockText=text.substring(0,start)+" "+text.substring(stop);
            if(LABEL.matcher(stockText).find()) return new Result(true,null,null,"Hãy chọn một đối tác cho mỗi phiếu. Tồn kho chưa thay đổi.");
        }
        boolean partnerAnswer=pending!=null && pending.awaitingPartner && (namedAnswer||!ACTION.matcher(stockText).find());
        InventoryChatInterpreter.Result stock;
        if(partnerAnswer) {
            if(text.contains("?")||Pattern.compile("\\b(neu|gia su|lam sao|bao nhieu)\\b").matcher(text).find())
                return new Result(false,pending,null,null);
            if(answer==null)answer=text;
            stock=InventoryChatInterpreter.interpret("",products,pending);
        } else stock=InventoryChatInterpreter.interpret(stockText,products,pending);
        if(!stock.handled || stock.draft==null) return convert(stock);
        InventoryChatInterpreter.Draft draft=stock.draft;
        draft.awaitingPartner=false;
        if(partners==null) return new Result(true,draft,stock.product,"Đang tải danh bạ đối tác. Gửi lại yêu cầu sau một chút; tồn kho chưa thay đổi.");
        if(answer!=null) {
            draft.partnerId=0;
            boolean incoming=Transaction.TYPE_INCOME.equals(draft.type);
            if(role!=null && ((incoming&&!role.equals("nha cung cap")&&!role.equals("ncc"))
                    ||(!incoming&&!role.equals("khach hang")&&!role.equals("kh"))))
                return ask(draft,stock.product,partners,"Nhập kho cần nhà cung cấp, xuất kho cần khách hàng. ");
            List<Partner> matches=find(answer,partners,draft.type);
            if(matches.size()!=1) return ask(draft,stock.product,matches.isEmpty()?partners:matches,
                    matches.isEmpty()?"Chưa tìm thấy đối tác phù hợp trong danh bạ. ":"Có nhiều đối tác trùng thông tin. ");
            draft.partnerId=matches.get(0).id;
        }
        if(draft.partnerId!=0) {
            boolean exists=false;
            for(Partner p:partners)if(p.id==draft.partnerId && expected(draft.type).equals(p.type))exists=true;
            if(!exists)draft.partnerId=0;
        }
        if(!stock.ready()) return new Result(true,draft,stock.product,stock.reply);
        if(draft.partnerId==0) return ask(draft,stock.product,partners,"");
        return new Result(true,draft,stock.product,null);
    }
    private static Result convert(InventoryChatInterpreter.Result result) {
        return new Result(result.handled,result.draft,result.product,result.reply);
    }
    private static String expected(String type) { return Transaction.TYPE_INCOME.equals(type)?Partner.SUPPLIER:Partner.CUSTOMER; }
    private static List<Partner> find(String answer,List<Partner> partners,String type) {
        String query=InventoryChatInterpreter.normalize(answer).replaceAll("^[\\s,:;]+|[\\s,:;.!]+$", "");
        List<Partner> matches=new ArrayList<>();
        if(query.isEmpty())return matches;
        for(Partner p:partners) {
            if(!expected(type).equals(p.type))continue;
            String name=InventoryChatInterpreter.normalize(p.name),phone=InventoryChatInterpreter.normalize(p.phone);
            if(query.equals(name)||(!phone.isEmpty()&&(query.equals(phone)||query.equals(name+" "+phone)||query.equals(name+" • "+phone)))
                    ||query.equals("ma doi tac "+p.id))matches.add(p);
        }
        return matches;
    }
    private static Result ask(InventoryChatInterpreter.Draft draft,Product product,List<Partner> partners,String prefix) {
        draft.awaitingPartner=true;
        String kind=Transaction.TYPE_INCOME.equals(draft.type)?"nhà cung cấp":"khách hàng";
        StringBuilder reply=new StringBuilder(prefix).append("Phiếu này thuộc ").append(kind).append(" nào? Gửi tên đầy đủ, số điện thoại hoặc mã đối tác trong danh bạ.");
        int count=0;
        for(Partner p:partners)if(expected(draft.type).equals(p.type)) {
            if(count++<8)reply.append("\nMã đối tác ").append(p.id).append(" — ").append(p.name).append(p.phone.isEmpty()?"":" • "+p.phone);
        }
        if(count==0)reply.append("\nChưa có ").append(kind).append(". Vào Trang chủ → Khách hàng & nhà cung cấp để thêm, rồi quay lại trả lời.");
        reply.append("\nGõ Hủy để bỏ phiếu. Tồn kho chưa thay đổi.");
        return new Result(true,draft,product,reply.toString());
    }
}
