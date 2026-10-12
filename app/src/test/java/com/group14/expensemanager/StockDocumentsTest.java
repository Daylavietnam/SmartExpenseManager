package com.group14.expensemanager;
import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.utils.StockDocuments;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class StockDocumentsTest {
    @Test public void legacyRowsRemainSeparateWhileDocumentLinesGroupAndSum() {
        Transaction a=new Transaction("A","",1000,"income",1);a.setId(1);
        Transaction b=new Transaction("B","",2000,"income",1);b.setId(2);
        assertEquals(2,StockDocuments.group(Arrays.asList(a,b)).size());
        a.setDocumentId("document-one");b.setDocumentId("document-one");
        assertEquals(1,StockDocuments.group(Arrays.asList(a,b)).size());
        assertEquals(3000,StockDocuments.total(Arrays.asList(a,b)),0);
        assertEquals("NK-DOCUMENT",StockDocuments.number(a));
    }
}
