package br.com.quindou.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private PdfRenderer renderer;
    private ParcelFileDescriptor descriptor;
    private ImageView image;
    private TextView label;
    private Button previous, next, zoomIn, zoomOut;
    private Bitmap shown;
    private int pageIndex, pageCount;
    private float zoom = 1f;
    private volatile boolean closed;
    private boolean loading;
    private BookStore store;
    private String bookId;
    private long lastTick;
    private boolean resumed, rendered;
    private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable checkpoint=new Runnable(){public void run(){saveReading();timer.postDelayed(this,15000);}};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            AuthClient auth=new AuthClient(this);String owner=getIntent().getStringExtra("user");bookId=getIntent().getStringExtra("book");
            if(owner==null||!owner.equals(auth.userId())){finish();return;}
            store=new BookStore(this,owner);pageIndex=store.get(bookId).optInt("page",0);
        }catch(Exception e){finish();return;}
        if(state != null) { pageIndex = state.getInt("page",pageIndex); zoom=state.getFloat("zoom",1f); }
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xfff7f7f2);
        root.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(8,4,8,4);
        Button back = new Button(this); back.setText("Voltar"); back.setOnClickListener(v->finish());bar.addView(back);
        label = new TextView(this);label.setText("Abrindo PDF…");label.setGravity(Gravity.CENTER);bar.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button done=new Button(this);done.setText("Concluir");done.setOnClickListener(v->{new AlertDialog.Builder(this).setTitle("Concluir leitura?").setMessage("Marcar este livro como concluído na sua biblioteca.").setNegativeButton("Cancelar",null).setPositiveButton("Concluir",(d,w)->worker.execute(()->{try{store.complete(bookId,true);runOnUiThread(()->{Toast.makeText(this,"Leitura concluída!",Toast.LENGTH_SHORT).show();finish();});}catch(Exception e){showError("Não foi possível salvar.");}})).show();});bar.addView(done);root.addView(bar);
        ScrollView vertical = new ScrollView(this); HorizontalScrollView horizontal = new HorizontalScrollView(this);
        image = new ImageView(this);image.setAdjustViewBounds(true);image.setContentDescription("Página do documento PDF");horizontal.addView(image);vertical.addView(horizontal);root.addView(vertical,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER);
        previous=button("‹ Página", controls,()->render(pageIndex-1)); zoomOut=button("−", controls,()->{zoom=Math.max(.5f,zoom-.25f);render(pageIndex);});
        zoomIn=button("+", controls,()->{zoom=Math.min(2f,zoom+.25f);render(pageIndex);});next=button("Página ›",controls,()->render(pageIndex+1));
        zoomOut.setContentDescription("Diminuir zoom");zoomIn.setContentDescription("Aumentar zoom");root.addView(controls);setContentView(root);root.requestApplyInsets();setLoading(true);
        worker.execute(()->{
            try {
                descriptor=ParcelFileDescriptor.open(store.pdf(bookId),ParcelFileDescriptor.MODE_READ_ONLY);
                renderer=new PdfRenderer(descriptor);pageCount=renderer.getPageCount();
                if(pageCount==0)throw new Exception("Documento vazio");
                runOnUiThread(()->{if(!closed){setLoading(false);render(Math.min(pageIndex,pageCount-1));}});
            } catch(Exception e){showError("Não foi possível abrir este PDF. Arquivos protegidos por senha não são suportados nesta versão.");}
        });
    }
    private Button button(String title, LinearLayout parent, Runnable action) {
        Button b=new Button(this);b.setText(title);b.setTextSize(12);parent.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->action.run());return b;
    }
    private void setLoading(boolean value){loading=value;previous.setEnabled(!value&&pageIndex>0);next.setEnabled(!value&&pageIndex<pageCount-1);zoomIn.setEnabled(!value&&zoom<2f);zoomOut.setEnabled(!value&&zoom>.5f);}
    private void render(int target){
        if(closed||loading||target<0||target>=pageCount)return;
        saveReading();pageIndex=target;rendered=false;setLoading(true);label.setText("Página "+(pageIndex+1)+" de "+pageCount);
        final int index=pageIndex;final float scale=zoom;
        final int screenWidth=getResources().getDisplayMetrics().widthPixels;
        worker.execute(()->{
            Bitmap bitmap=null;
            try(PdfRenderer.Page page=renderer.openPage(index)){
                int width=Math.min(2200,Math.max(320,(int)(screenWidth*scale)));
                // Bound allocation for unusually tall pages and low-memory devices.
                double ratio=(double)page.getHeight()/page.getWidth();
                width=(int)Math.min(width,Math.sqrt(6000000/ratio));
                int height=Math.max(1,(int)(width*ratio));
                bitmap=Bitmap.createBitmap(Math.max(1,width),height,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);
                page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                final Bitmap ready=bitmap;
                runOnUiThread(()->{if(closed){ready.recycle();return;}image.setLayoutParams(new android.widget.FrameLayout.LayoutParams(ready.getWidth(),ready.getHeight()));image.setImageBitmap(ready);Bitmap old=shown;shown=ready;if(old!=null)old.recycle();image.setContentDescription("Página "+(index+1)+" de "+pageCount);rendered=true;lastTick=android.os.SystemClock.elapsedRealtime();saveReading();setLoading(false);});
            }catch(Exception|OutOfMemoryError e){if(bitmap!=null)bitmap.recycle();showError("Não foi possível renderizar esta página. Tente outro PDF.");}
        });
    }
    private void showError(String message){runOnUiThread(()->{if(!closed)new AlertDialog.Builder(this).setTitle("Quindou").setMessage(message).setPositiveButton("Voltar",(d,w)->finish()).setOnCancelListener(d->finish()).show();});}
    private void saveReading(){
        if(store==null||!rendered||closed)return;long now=android.os.SystemClock.elapsedRealtime();long elapsed=resumed&&lastTick>0?Math.max(0,now-lastTick):0;lastTick=now;final int page=pageIndex;
        worker.execute(()->{try{store.progress(bookId,page,elapsed);}catch(Exception ignored){}});
    }
    @Override protected void onResume(){super.onResume();resumed=true;lastTick=android.os.SystemClock.elapsedRealtime();timer.postDelayed(checkpoint,15000);}
    @Override protected void onPause(){saveReading();resumed=false;timer.removeCallbacks(checkpoint);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("page",pageIndex);state.putFloat("zoom",zoom);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){timer.removeCallbacks(checkpoint);closed=true;worker.execute(()->{try{if(renderer!=null)renderer.close();if(descriptor!=null)descriptor.close();}catch(Exception ignored){}});worker.shutdown();if(image!=null)image.setImageDrawable(null);if(shown!=null)shown.recycle();super.onDestroy();}
}
