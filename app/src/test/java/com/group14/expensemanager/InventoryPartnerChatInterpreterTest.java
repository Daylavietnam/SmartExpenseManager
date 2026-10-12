package com.group14.expensemanager;
import com.group14.expensemanager.model.*;
import com.group14.expensemanager.utils.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class InventoryPartnerChatInterpreterTest {
    private Product product() { Product p=new Product();p.id=1;p.sku="KH001";p.name="Nước suối";p.unit="chai";p.referencePrice=5000;return p; }
    private Partner partner(int id,String name,String type,String phone) { Partner p=new Partner();p.id=id;p.name=name;p.type=type;p.phone=phone;return p; }
    private List<Partner> partners() { return Arrays.asList(partner(1,"Công ty An Nam",Partner.SUPPLIER,"0900123456"),partner(2,"Chị Lan",Partner.CUSTOMER,"0912345678")); }
    private InventoryPartnerChatInterpreter.Result parse(String text,InventoryChatInterpreter.Draft pending) { return InventoryPartnerChatInterpreter.interpret(text,Collections.singletonList(product()),partners(),pending); }
    @Test public void receiptWaitsForSupplierThenResolvesPhoneWithoutChangingQuantity() {
        InventoryPartnerChatInterpreter.Result first=parse("nhập 10 KH001",null);assertFalse(first.ready());assertTrue(first.reply.contains("nhà cung cấp"));
        InventoryPartnerChatInterpreter.Result done=parse("0900123456",first.draft);assertTrue(done.ready());assertEquals(1,done.draft.partnerId);assertEquals(10,done.draft.quantity,0);
    }
    @Test public void issueAsksPriceThenCustomerAndKeepsPrice() {
        InventoryPartnerChatInterpreter.Result price=parse("xuất 3 KH001",null);assertFalse(price.ready());assertTrue(price.reply.contains("đơn giá"));
        InventoryPartnerChatInterpreter.Result customer=parse("giá 6000",price.draft);assertFalse(customer.ready());assertTrue(customer.reply.contains("khách hàng"));
        InventoryPartnerChatInterpreter.Result done=parse("chi lan",customer.draft);assertTrue(done.ready());assertEquals(2,done.draft.partnerId);assertEquals(6000,done.draft.unitPrice,0);
    }
    @Test public void inlineSupplierNamesAndPhoneDoNotAffectStockParser() {
        InventoryPartnerChatInterpreter.Result done=parse("nhập 2 KH001 từ nhà cung cấp Công ty An Nam, giá 5000",null);
        assertTrue(done.ready());assertEquals(2,done.draft.quantity,0);assertEquals(5000,done.draft.unitPrice,0);assertEquals(1,done.draft.partnerId);
        assertTrue(parse("xuất 2 KH001 khách hàng 0912345678 giá 7000",null).ready());
    }
    @Test public void inlinePartnerSurvivesPriceFollowup() {
        InventoryPartnerChatInterpreter.Result first=parse("xuất 2 KH001 khách hàng Chị Lan",null);assertFalse(first.ready());assertEquals(2,first.draft.partnerId);
        InventoryPartnerChatInterpreter.Result done=parse("5k",first.draft);assertTrue(done.ready());assertEquals(2,done.draft.partnerId);
    }
    @Test public void wrongRoleUnknownAndDuplicatesNeverWrite() {
        InventoryPartnerChatInterpreter.Result first=parse("nhập 2 KH001 khách hàng Chị Lan",null);assertFalse(first.ready());assertEquals(0,first.draft.partnerId);
        InventoryPartnerChatInterpreter.Result unknown=parse("Đối tác chưa có",first.draft);assertFalse(unknown.ready());assertTrue(unknown.draft.awaitingPartner);
        List<Partner> duplicate=Arrays.asList(partner(4,"An",Partner.SUPPLIER,"111"),partner(5,"An",Partner.SUPPLIER,"222"));
        InventoryPartnerChatInterpreter.Result ambiguous=InventoryPartnerChatInterpreter.interpret("nhập 2 KH001 nhà cung cấp An",Collections.singletonList(product()),duplicate,null);
        assertFalse(ambiguous.ready());assertTrue(ambiguous.reply.contains("trùng"));
        InventoryPartnerChatInterpreter.Result done=InventoryPartnerChatInterpreter.interpret("mã đối tác 5",Collections.singletonList(product()),duplicate,ambiguous.draft);assertTrue(done.ready());assertEquals(5,done.draft.partnerId);
    }
    @Test public void cancelAndQuestionNeverWriteOrDiscardWaitingDraft() {
        InventoryPartnerChatInterpreter.Result first=parse("nhập 2 KH001",null);
        assertNull(parse("hủy",first.draft).draft);
        InventoryPartnerChatInterpreter.Result question=parse("nhà cung cấp nào?",first.draft);assertFalse(question.handled);assertEquals(first.draft,question.draft);
        assertFalse(parse("Tôi chưa nhập 2 KH001 nhà cung cấp Công ty An Nam",null).handled);
    }
    @Test public void emptyCatalogKeepsDraftUntilPartnerIsCreated() {
        InventoryPartnerChatInterpreter.Result empty=InventoryPartnerChatInterpreter.interpret("nhập 2 KH001",Collections.singletonList(product()),Collections.emptyList(),null);
        assertFalse(empty.ready());assertTrue(empty.reply.contains("Chưa có nhà cung cấp"));
        assertTrue(parse("Công ty An Nam",empty.draft).ready());
    }
    @Test public void fullPartnerNameMayContainActionOrRoleWords() {
        List<Partner> contacts=Collections.singletonList(partner(8,"Công ty Xuất Nhập Khẩu NCC",Partner.SUPPLIER,""));
        InventoryPartnerChatInterpreter.Result first=InventoryPartnerChatInterpreter.interpret("nhập 2 KH001",Collections.singletonList(product()),contacts,null);
        assertTrue(InventoryPartnerChatInterpreter.interpret("Công ty Xuất Nhập Khẩu NCC",Collections.singletonList(product()),contacts,first.draft).ready());
    }
}
