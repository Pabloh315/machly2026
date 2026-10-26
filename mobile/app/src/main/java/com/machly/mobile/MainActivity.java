package com.machly.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Inicio seguro: las credenciales siempre se validan en la API Machly. */
public class MainActivity extends AppCompatActivity {
    static final String PREFS = "machly_session", KEY_SERVER = "server", KEY_TOKEN = "token", KEY_USER = "user";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private EditText serverInput, emailInput, passwordInput;
    private TextView status;
    private Button loginButton;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildLoginScreen();
        String token = prefs.getString(KEY_TOKEN, "");
        if (!token.isEmpty()) validateSavedSession(token);
    }

    private void buildLoginScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245,244,238));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setPadding(dp(28),dp(55),dp(28),dp(30));
        TextView mark = text("M", 30, Color.rgb(190,220,117), true); mark.setGravity(Gravity.CENTER); mark.setBackground(round(Color.rgb(20,60,44), 18));
        root.addView(mark, new LinearLayout.LayoutParams(dp(68),dp(68)));
        TextView title = text("MACHLY",25,Color.rgb(20,60,44),true); title.setLetterSpacing(.12f); title.setPadding(0,dp(15),0,0); root.addView(title);
        TextView subtitle = text("Maquinaria que mueve tu campo",15,Color.rgb(99,112,103),false); subtitle.setPadding(0,0,0,dp(28)); root.addView(subtitle);
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(22),dp(24),dp(22),dp(24)); card.setBackground(round(Color.WHITE,18)); card.setElevation(dp(5));
        card.addView(text("Conecta e ingresa",23,Color.rgb(23,32,27),true));
        TextView helper = text("La computadora debe ejecutar Machly y estar en la misma red Wi‑Fi.",13,Color.rgb(104,115,108),false); helper.setPadding(0,dp(5),0,dp(18)); card.addView(helper);
        serverInput = field("Servidor (ej. http://192.168.1.50:3000)", InputType.TYPE_CLASS_TEXT); serverInput.setText(prefs.getString(KEY_SERVER,"http://10.0.2.2:3000")); card.addView(serverInput);
        emailInput = field("Correo electrónico", InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); card.addView(emailInput);
        passwordInput = field("Contraseña", InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); card.addView(passwordInput);
        status = text("",13,Color.rgb(173,62,52),false); status.setPadding(0,dp(8),0,dp(8)); card.addView(status);
        loginButton = new Button(this); loginButton.setText("INGRESAR"); loginButton.setTextColor(Color.WHITE); loginButton.setTypeface(Typeface.DEFAULT,Typeface.BOLD); loginButton.setBackground(round(Color.rgb(121,167,76),12)); loginButton.setOnClickListener(v -> login()); card.addView(loginButton,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));
        Button register = new Button(this); register.setText("Crear una cuenta"); register.setTextColor(Color.rgb(20,60,44)); register.setBackgroundColor(Color.TRANSPARENT); register.setOnClickListener(v -> showRegisterDialog()); card.addView(register,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));
        root.addView(card,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)); scroll.addView(root); setContentView(scroll);
    }

    private void login() {
        String server=normalizeServer(serverInput.getText().toString()), email=emailInput.getText().toString().trim(), password=passwordInput.getText().toString();
        if(server==null||email.isEmpty()||password.isEmpty()){ showStatus("Completa servidor, correo y contraseña."); return; }
        setBusy(true,"Validando credenciales…");
        executor.execute(() -> { try { JSONObject body=new JSONObject().put("email",email).put("password",password); acceptSession(server, request(server,"/api/auth/login","POST",body,null)); } catch(Exception e){ runOnUiThread(() -> { setBusy(false, readable(e)); }); } });
    }

    private void validateSavedSession(String token) {
        String server=prefs.getString(KEY_SERVER,""); if(server.isEmpty()) return;
        setBusy(true,"Restaurando sesión…");
        executor.execute(() -> { try { JSONObject user=request(server,"/api/auth/me","GET",null,token); runOnUiThread(() -> openApp(server,token,user.toString())); } catch(Exception e){ prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply(); runOnUiThread(() -> setBusy(false,"Tu sesión venció. Ingresa nuevamente.")); } });
    }

    private void showRegisterDialog() {
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),0,dp(20),0);
        EditText name=field("Nombre completo",InputType.TYPE_CLASS_TEXT), email=field("Correo electrónico",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS), phone=field("Teléfono",InputType.TYPE_CLASS_PHONE), password=field("Contraseña (mínimo 8 caracteres)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Spinner role=new Spinner(this); role.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Quiero alquilar equipos","Quiero publicar equipos"}));
        form.addView(name);form.addView(email);form.addView(phone);form.addView(password);form.addView(role);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Crear cuenta Machly").setView(form).setPositiveButton("Crear cuenta",null).setNegativeButton("Cancelar",null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String server=normalizeServer(serverInput.getText().toString()); if(server==null){showStatus("Primero ingresa una dirección de servidor válida.");dialog.dismiss();return;}
            if(name.getText().toString().trim().length()<3||password.getText().length()<8){password.setError("Revisa el nombre y usa al menos 8 caracteres");return;}
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            executor.execute(() -> { try { JSONObject body=new JSONObject().put("fullName",name.getText().toString().trim()).put("email",email.getText().toString().trim()).put("phone",phone.getText().toString().trim()).put("password",password.getText().toString()).put("role",role.getSelectedItemPosition()==1?"owner":"renter"); JSONObject response=request(server,"/api/auth/register","POST",body,null); runOnUiThread(dialog::dismiss); acceptSession(server,response); } catch(Exception e){runOnUiThread(() -> {dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);showStatus(readable(e));dialog.dismiss();});} });
        })); dialog.show();
    }

    private void acceptSession(String server, JSONObject response) throws Exception {
        String token=response.getString("token"), user=response.getJSONObject("user").toString(); prefs.edit().putString(KEY_SERVER,server).putString(KEY_TOKEN,token).putString(KEY_USER,user).apply(); runOnUiThread(() -> openApp(server,token,user));
    }
    private void openApp(String server,String token,String user){Intent i=new Intent(this,WebAppActivity.class);i.putExtra(KEY_SERVER,server);i.putExtra(KEY_TOKEN,token);i.putExtra(KEY_USER,user);startActivity(i);setBusy(false,"");}

    private JSONObject request(String server,String path,String method,JSONObject body,String token) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(server+path).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("Accept","application/json");
        if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode();InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();StringBuilder text=new StringBuilder();if(stream!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)text.append(line);}
        JSONObject json=text.length()>0?new JSONObject(text.toString()):new JSONObject();if(code<200||code>=300)throw new Exception(json.optString("error","Error del servidor ("+code+")"));return json;
    }
    private String normalizeServer(String raw){String v=raw==null?"":raw.trim();while(v.endsWith("/"))v=v.substring(0,v.length()-1);return (v.startsWith("http://")||v.startsWith("https://"))&&v.length()>9?v:null;}
    private String readable(Exception e){String message=e.getMessage();return message==null||message.contains("failed to connect")?"No se pudo conectar. Revisa el servidor y la red Wi‑Fi.":message;}
    private void setBusy(boolean busy,String message){loginButton.setEnabled(!busy);serverInput.setEnabled(!busy);emailInput.setEnabled(!busy);passwordInput.setEnabled(!busy);showStatus(message);}
    private void showStatus(String value){status.setText(value);}
    private EditText field(String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setInputType(type);e.setSingleLine(true);e.setBackground(round(Color.rgb(245,246,241),10));e.setPadding(dp(13),0,dp(13),0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(51));p.setMargins(0,0,0,dp(12));e.setLayoutParams(p);return e;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
