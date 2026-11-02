package br.com.quindou.app;

import android.content.Context;
import android.util.Base64;
import android.security.keystore.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.io.*;
import org.json.*;

/** Supabase Auth; only the publishable key belongs in client source. */
public final class AuthClient {
    static final String URL="https://wxhnayryviiddgfpjznt.supabase.co";
    static final String KEY="sb_publishable_J3W_QC9bv5NrnkjzHNIwfQ_qrmat91V";
    private final Context context;
    private JSONObject session;
    public AuthClient(Context context){this.context=context;try{String value=context.getSharedPreferences("session",0).getString("encrypted",null);if(value!=null){JSONObject stored=new JSONObject(value);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(stored.getString("iv"),0)));session=new JSONObject(new String(c.doFinal(Base64.decode(stored.getString("data"),0)),java.nio.charset.StandardCharsets.UTF_8));}}catch(Exception e){clear();}}
    private javax.crypto.SecretKey key()throws Exception{KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(!store.containsAlias("quindou-session")){KeyGenerator gen=KeyGenerator.getInstance("AES","AndroidKeyStore");gen.init(new KeyGenParameterSpec.Builder("quindou-session",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();}return (javax.crypto.SecretKey)store.getKey("quindou-session",null);}
    private void save(JSONObject response)throws Exception{
        if(response.optString("access_token").isEmpty()||response.optJSONObject("user")==null)throw new IOException("Sessão inválida.");
        response.put("expires_at",System.currentTimeMillis()/1000+response.optLong("expires_in",3600));
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());byte[] data=cipher.doFinal(response.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JSONObject encrypted=new JSONObject().put("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).put("data",Base64.encodeToString(data,Base64.NO_WRAP));
        if(!context.getSharedPreferences("session",0).edit().putString("encrypted",encrypted.toString()).commit())throw new IOException("Não foi possível salvar a sessão.");session=response;
    }
    public synchronized JSONObject user(){return session==null?null:session.optJSONObject("user");}
    public synchronized String userId(){JSONObject u=user();return u==null?null:u.optString("id",null);}
    public synchronized void clear(){session=null;context.getSharedPreferences("session",0).edit().clear().commit();}
    static class ApiError extends IOException { final String code;ApiError(String message,String code){super(message);this.code=code;} }
    static JSONObject request(String path,JSONObject body,String access)throws Exception{
        HttpsURLConnection c=(HttpsURLConnection)new URL(URL+"/auth/v1/"+path).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestMethod("POST");c.setRequestProperty("apikey",KEY);c.setRequestProperty("Content-Type","application/json");if(access!=null)c.setRequestProperty("Authorization","Bearer "+access);c.setDoOutput(true);
        try{try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}int status=c.getResponseCode();InputStream stream=status<400?c.getInputStream():c.getErrorStream();String text;try(InputStream in=stream){text=in==null?"{}":new String(readBytes(in),java.nio.charset.StandardCharsets.UTF_8);}JSONObject value=text.trim().isEmpty()?new JSONObject():new JSONObject(text);if(status>=400){String code=value.optString("error_code",value.optString("code"));String message=value.optString("msg",value.optString("message",value.optString("error_description","Não foi possível autenticar.")));throw new ApiError(translate(code,message),code);}return value;}finally{c.disconnect();}
    }
    private static String translate(String code,String fallback){switch(code){case "invalid_credentials":return "E-mail ou senha incorretos.";case "email_not_confirmed":return "Confirme seu e-mail antes de entrar.";case "over_email_send_rate_limit":return "Limite temporário de e-mails. Aguarde e tente novamente.";case "email_address_not_authorized":return "O envio para este e-mail ainda precisa ser habilitado no servidor.";case "user_already_exists":return "Já existe uma conta com este e-mail. Use Entrar.";default:return fallback;}}
    public synchronized JSONObject login(String email,String password)throws Exception{JSONObject r=request("token?grant_type=password",new JSONObject().put("email",email).put("password",password),null);save(r);return user();}
    public synchronized boolean signup(String name,String email,String password)throws Exception{JSONObject r=request("signup",new JSONObject().put("email",email).put("password",password).put("data",new JSONObject().put("display_name",name)),null);if(!r.optString("access_token").isEmpty()){save(r);return true;}return false;}
    public synchronized void refreshIfNeeded()throws Exception{
        if(session==null||session.optLong("expires_at")>System.currentTimeMillis()/1000+60)return;
        try{save(request("token?grant_type=refresh_token",new JSONObject().put("refresh_token",session.getString("refresh_token")),null));}
        catch(ApiError e){clear();throw e;}
        // An unavailable network leaves a previously authenticated user's local library usable offline.
        catch(IOException offline){}
    }
    public synchronized void resend(String email)throws Exception{request("resend",new JSONObject().put("type","signup").put("email",email),null);}
    public synchronized void signOut(){String token=session==null?null:session.optString("access_token");clear();if(token!=null)try{request("logout?scope=local",new JSONObject(),token);}catch(Exception ignored){}}
    private static byte[] readBytes(InputStream in)throws IOException { ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray(); }

}
