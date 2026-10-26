package com.machly.mobile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

/** WebView autenticada; solo recibe sesiones previamente validadas por MainActivity. */
public class WebAppActivity extends AppCompatActivity {
    private WebView webView; private String server,token,user; private boolean injected;
    @Override protected void onCreate(Bundle state){super.onCreate(state);server=getIntent().getStringExtra(MainActivity.KEY_SERVER);token=getIntent().getStringExtra(MainActivity.KEY_TOKEN);user=getIntent().getStringExtra(MainActivity.KEY_USER);if(server==null||token==null||user==null){finish();return;}build();webView.loadUrl(server);}
    @SuppressLint("SetJavaScriptEnabled") private void build(){FrameLayout root=new FrameLayout(this);webView=new WebView(this);WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);s.setUserAgentString(s.getUserAgentString()+" MachlyAndroid/2.0");webView.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView view,String url){if(!injected&&url.startsWith(server)){injected=true;String js="localStorage.setItem('machly_token',"+JSONObject.quote(token)+");localStorage.setItem('machly_user',"+JSONObject.quote(user)+");location.replace('/');";view.evaluateJavascript(js,null);}}@Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())Toast.makeText(WebAppActivity.this,"Servidor Machly no disponible",Toast.LENGTH_LONG).show();}});root.addView(webView,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        TextView menu=new TextView(this);menu.setText("⋮");menu.setTextSize(28);menu.setTextColor(Color.WHITE);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Opciones de sesión");GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(20,60,44));bg.setShape(GradientDrawable.OVAL);menu.setBackground(bg);menu.setElevation(dp(8));menu.setOnClickListener(v->sessionMenu());FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(46),dp(46));p.gravity=Gravity.TOP|Gravity.END;p.setMargins(0,dp(13),dp(13),0);root.addView(menu,p);setContentView(root);}
    private void sessionMenu(){new AlertDialog.Builder(this).setTitle("Sesión Machly").setItems(new String[]{"Recargar","Cambiar servidor","Cerrar sesión"},(d,which)->{if(which==0)webView.reload();else{if(which==2)getSharedPreferences(MainActivity.PREFS,MODE_PRIVATE).edit().clear().apply();Intent i=new Intent(this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);finish();}}).show();}
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    @Override protected void onDestroy(){if(webView!=null){webView.stopLoading();webView.destroy();}super.onDestroy();}
}
