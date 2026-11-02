package br.com.quindou.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.webkit.*;
import android.widget.FrameLayout;
import androidx.webkit.WebViewAssetLoader;
import org.json.*;
import java.io.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private WebView web;
    private AuthClient auth;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;
    private String pendingImportUser;
    private static final int PICK_PDF=41;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);auth=new AuthClient(this);web=new WebView(this);web.setBackgroundColor(0xfff7f7f2);
        FrameLayout root=new FrameLayout(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});setContentView(root);root.requestApplyInsets();
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(false);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setUserAgentString(settings.getUserAgentString()+" QuindouAndroid/1.1");
        final WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){
                WebResourceResponse response=loader.shouldInterceptRequest(req.getUrl());if(response!=null)return response;
                Uri uri=req.getUrl();if("appassets.androidplatform.net".equals(uri.getHost())&&uri.getPath()!=null&&uri.getPath().startsWith("/covers/")&&auth.userId()!=null){
                    try{String id=uri.getLastPathSegment().replace(".png","");return new WebResourceResponse("image/png",null,new FileInputStream(new BookStore(MainActivity.this,auth.userId()).thumbnail(id)));}catch(Exception ignored){}
                }
                return new WebResourceResponse("text/plain","UTF-8",404,"Not found",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){return true;}
        });
        web.addJavascriptInterface(new Bridge(),"QuindouNative");web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }
    private String requireUser()throws Exception{String id=auth.userId();if(id==null)throw new IOException("Entre na sua conta para continuar.");return id;}
    private JSONObject state()throws Exception{
        JSONObject result=new JSONObject();JSONObject user=auth.user();
        if(user==null)return result.put("user",JSONObject.NULL).put("books",new JSONArray());
        JSONObject meta=user.optJSONObject("user_metadata");String email=user.optString("email");String name=meta==null?"":meta.optString("display_name");if(name.isEmpty())name=email.split("@")[0];
        return result.put("user",new JSONObject().put("id",requireUser()).put("email",email).put("name",name)).put("books",new BookStore(this,requireUser()).list());
    }
    private void emit(JSONObject value){runOnUiThread(()->{if(!destroyed&&web!=null)web.evaluateJavascript("window.onNative("+value.toString()+")",null);});}
    private void error(Exception e){try{emit(new JSONObject().put("error",e instanceof java.net.UnknownHostException||e instanceof java.net.SocketTimeoutException?"Sem conexão. Verifique sua internet.":e.getMessage()==null?"Não foi possível concluir a operação.":e.getMessage()));}catch(Exception ignored){}}
    public final class Bridge {
        @JavascriptInterface public void request(String raw){worker.execute(()->{try{
            JSONObject request=new JSONObject(raw);String action=request.getString("action");JSONObject result;
            switch(action){
                case "state":auth.refreshIfNeeded();result=state();break;
                case "login":auth.login(request.getString("email"),request.getString("password"));result=state();break;
                case "signup":if(request.getString("password").length()<8)throw new IOException("Use uma senha com pelo menos 8 caracteres.");boolean signed=auth.signup(request.getString("name"),request.getString("email"),request.getString("password"));result=state();if(!signed)result.put("message","Conta criada. Confirme pelo link enviado ao seu e-mail e volte para entrar.");break;
                case "resend":auth.resend(request.getString("email"));result=new JSONObject().put("message","Se houver uma conta pendente, um novo link de confirmação será enviado.");break;
                case "logout":auth.signOut();result=state();break;
                case "favorite":new BookStore(MainActivity.this,requireUser()).favorite(request.getString("id"));result=state();break;
                case "open":openBook(requireUser(),request.getString("id"));result=new JSONObject().put("busy",false);break;
                case "import":String owner=requireUser();runOnUiThread(()->choose(owner));return;
                default:throw new IOException("Operação inválida.");
            }
            emit(result);
        }catch(Exception e){error(e);}});}
    }
    private void choose(String owner){
        if(destroyed)return;pendingImportUser=owner;Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("application/pdf");intent.addCategory(Intent.CATEGORY_OPENABLE);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{startActivityForResult(intent,PICK_PDF);}catch(Exception e){pendingImportUser=null;error(new IOException("Seletor de PDF indisponível."));}
    }
    private void openBook(String owner,String id)throws Exception{
        new BookStore(this,owner).get(id);runOnUiThread(()->{if(!destroyed&&owner.equals(auth.userId()))startActivity(new Intent(this,PdfActivity.class).putExtra("user",owner).putExtra("book",id));});
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK_PDF)return;String owner=pendingImportUser;pendingImportUser=null;
        if(result!=RESULT_OK||data==null||data.getData()==null){try{emit(new JSONObject().put("busy",false));}catch(Exception ignored){}return;}
        Uri uri=data.getData();worker.execute(()->{try{
            if(owner==null||!owner.equals(requireUser()))throw new IOException("Entre novamente antes de importar.");String title="Meu livro.pdf";
            try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())title=cursor.getString(0);}
            JSONObject book;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Arquivo indisponível.");book=new BookStore(this,owner).importPDF(in,title);}
            emit(state().put("message","PDF guardado no app. Você pode abri-lo sem o arquivo original."));openBook(owner,book.getString("id"));
        }catch(Exception e){error(e);}});
    }
    @Override protected void onResume(){super.onResume();if(web!=null)web.evaluateJavascript("if(window.refreshLibrary)window.refreshLibrary()",null);}
    @Override public void onBackPressed(){finish();}
    @Override protected void onDestroy(){destroyed=true;if(web!=null){web.removeJavascriptInterface("QuindouNative");web.destroy();web=null;}worker.shutdown();super.onDestroy();}
}
