package br.com.quindou.app;

import android.content.Context;
import android.graphics.pdf.PdfDocument;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BookStoreTest {
    private Context context;private String owner;private byte[] sample;
    @Before public void setup()throws Exception{
        context=ApplicationProvider.getApplicationContext();owner=UUID.randomUUID().toString();
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("sample.pdf")){assertNotNull(in);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);sample=out.toByteArray();}
    }
    @Test public void importedCopySurvivesSourceLossAndReopening()throws Exception{
        File original=new File(context.getCacheDir(),UUID.randomUUID()+".pdf");try(OutputStream out=new FileOutputStream(original)){out.write(sample);}
        BookStore store=new BookStore(context,owner);JSONObject book;try(InputStream in=new FileInputStream(original)){book=store.importPDF(in,"Meu PDF.pdf");}
        assertTrue(original.delete());BookStore reopened=new BookStore(context,owner);assertEquals(1,reopened.list().length());assertTrue(reopened.pdf(book.getString("id")).isFile());assertEquals(sample.length,reopened.pdf(book.getString("id")).length());assertEquals(3,book.getInt("pages"));
    }
    @Test public void progressFavoritesCompletionSurviveRestart()throws Exception{
        BookStore store=new BookStore(context,owner);String id=store.importPDF(new ByteArrayInputStream(sample),"Livro.pdf").getString("id");store.progress(id,1,15000);store.favorite(id);store.complete(id,true);
        JSONObject saved=new BookStore(context,owner).get(id);assertEquals(1,saved.getInt("page"));assertEquals(2,saved.getInt("furthest"));assertEquals(15000,saved.getLong("readMillis"));assertTrue(saved.getBoolean("favorite"));assertTrue(saved.getBoolean("completed"));
        store.progress(id,0,1000);assertEquals(2,store.get(id).getInt("furthest"));
    }
    @Test public void accountsAreIsolatedAndDuplicateIsNotAdded()throws Exception{
        BookStore first=new BookStore(context,owner);String id=first.importPDF(new ByteArrayInputStream(sample),"Livro.pdf").getString("id");assertEquals(id,first.importPDF(new ByteArrayInputStream(sample),"Outro nome.pdf").getString("id"));assertEquals(1,first.list().length());
        BookStore second=new BookStore(context,UUID.randomUUID().toString());assertEquals(0,second.list().length());try{second.pdf(id);fail("another account's file must not open");}catch(Exception expected){}
    }
    @Test public void invalidPdfLeavesNoLibraryEntry()throws Exception{
        BookStore store=new BookStore(context,owner);try{store.importPDF(new ByteArrayInputStream("not a PDF".getBytes()),"ruim.pdf");fail("must reject invalid PDF");}catch(Exception expected){}assertEquals(0,store.list().length());
    }
}
