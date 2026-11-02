package br.com.quindou.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.UUID;

/** Private, per-account copies. Never relies on the original document URI. */
public final class BookStore {
    private final File directory;
    public BookStore(Context context, String userId) throws IOException {
        if(!userId.matches("[a-fA-F0-9-]{36}")) throw new IOException("Conta inválida.");
        directory=new File(context.getFilesDir(),"library/"+userId);
        if(!directory.exists()&&!directory.mkdirs())throw new IOException("Não foi possível criar a biblioteca.");
    }
    private void validId(String id)throws IOException{if(id==null||!id.matches("[a-f0-9-]{36}"))throw new IOException("Livro inválido.");}
    private JSONObject metadata(File file)throws Exception{
        try(InputStream in=new AtomicFile(file).openRead()){return new JSONObject(new String(readBytes(in),java.nio.charset.StandardCharsets.UTF_8));}
    }
    private void write(String id,JSONObject value)throws Exception{
        validId(id);AtomicFile atomic=new AtomicFile(new File(directory,id+".json"));FileOutputStream out=null;
        try{out=atomic.startWrite();out.write(value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));atomic.finishWrite(out);}
        catch(Exception e){if(out!=null)atomic.failWrite(out);throw e;}
    }
    public synchronized JSONArray list()throws Exception{
        JSONArray result=new JSONArray();File[] files=directory.listFiles((dir,name)->name.endsWith(".json"));
        if(files!=null)for(File file:files){try{JSONObject data=metadata(file);if(new File(directory,data.getString("id")+".pdf").isFile())result.put(data);}catch(Exception ignored){}}
        return result;
    }
    public synchronized JSONObject get(String id)throws Exception{validId(id);return metadata(new File(directory,id+".json"));}
    public synchronized File pdf(String id)throws Exception{validId(id);get(id);File f=new File(directory,id+".pdf");if(!f.isFile())throw new IOException("Arquivo não encontrado.");return f;}
    public synchronized File thumbnail(String id)throws Exception{validId(id);return new File(directory,id+".png");}
    public synchronized JSONObject importPDF(InputStream source,String title)throws Exception{
        String id=UUID.randomUUID().toString();File temporary=new File(directory,id+".part");File destination=new File(directory,id+".pdf");File thumb=new File(directory,id+".png");
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");long size=0;
            try(OutputStream out=new FileOutputStream(temporary)){
                byte[] buffer=new byte[65536];int n;while((n=source.read(buffer))!=-1){size+=n;if(size>300L*1024*1024)throw new IOException("O PDF excede o limite de 300 MB.");out.write(buffer,0,n);digest.update(buffer,0,n);}
            }
            StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            JSONArray current=list();for(int i=0;i<current.length();i++){JSONObject book=current.getJSONObject(i);if(hash.toString().equals(book.optString("sha256"))){temporary.delete();return book;}}
            int total;
            try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(temporary,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd)){
                total=renderer.getPageCount();if(total<1)throw new IOException("PDF sem páginas.");
                try(PdfRenderer.Page page=renderer.openPage(0)){
                    int width=280;int height=Math.max(1,Math.min(700,(int)(width*(double)page.getHeight()/page.getWidth())));
                    Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);
                    try{page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);try(OutputStream out=new FileOutputStream(thumb)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}}finally{bitmap.recycle();}
                }
            }
            if(!temporary.renameTo(destination))throw new IOException("Não foi possível guardar o PDF.");
            String name=title==null?"Meu livro":title.replaceAll("(?i)\\.pdf$","").trim();if(name.isEmpty())name="Meu livro";
            JSONObject book=new JSONObject().put("id",id).put("title",name).put("pages",total).put("page",0).put("furthest",0).put("completed",false).put("favorite",false).put("readMillis",0).put("size",size).put("sha256",hash.toString()).put("added",System.currentTimeMillis()).put("updated",0);
            write(id,book);return book;
        }catch(Exception|OutOfMemoryError e){temporary.delete();destination.delete();thumb.delete();if(e instanceof Exception)throw (Exception)e;throw new IOException("Sem memória para importar este PDF.");}
    }
    public synchronized JSONObject progress(String id,int page,long millis)throws Exception{
        JSONObject book=get(id);int total=book.getInt("pages");if(page<0||page>=total)throw new IOException("Página inválida.");
        book.put("page",page).put("furthest",Math.max(book.optInt("furthest"),page+1)).put("readMillis",book.optLong("readMillis")+Math.max(0,Math.min(millis,300000))).put("updated",System.currentTimeMillis());
        write(id,book);return book;
    }
    public synchronized void complete(String id,boolean completed)throws Exception{JSONObject b=get(id);b.put("completed",completed);write(id,b);}
    public synchronized void favorite(String id)throws Exception{JSONObject b=get(id);b.put("favorite",!b.optBoolean("favorite"));write(id,b);}
    private static byte[] readBytes(InputStream in)throws IOException { ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray(); }

}
