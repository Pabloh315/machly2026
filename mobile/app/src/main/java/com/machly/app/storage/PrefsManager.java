package com.machly.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.machly.app.AppConstants;
import com.machly.app.models.LoginResponse;
import com.machly.app.models.User;

public class PrefsManager {
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final Gson gson = new Gson();

    public PrefsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(LoginResponse response) {
        if (response == null || response.getToken() == null) return;
        editor.putString(AppConstants.KEY_TOKEN, response.getToken());
        editor.putLong(AppConstants.KEY_TOKEN_EXPIRY, System.currentTimeMillis() + Math.max(response.getExpiresIn(), 3600) * 1000L);
        if (response.getUser() != null) {
            editor.putString(AppConstants.KEY_USER_JSON, gson.toJson(response.getUser()));
        }
        editor.apply();
    }

    public void saveUser(User user) {
        if (user == null) return;
        editor.putString(AppConstants.KEY_USER_JSON, gson.toJson(user));
        editor.apply();
    }

    public String getToken() {
        String token = prefs.getString(AppConstants.KEY_TOKEN, null);
        if (token != null && prefs.getLong(AppConstants.KEY_TOKEN_EXPIRY, 0) <= System.currentTimeMillis()) {
            clearSession();
            return null;
        }
        return token;
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public User getUser() {
        if (getToken() == null) return null;
        String json = prefs.getString(AppConstants.KEY_USER_JSON, null);
        if (json == null || json.isEmpty()) return null;
        try {
            return gson.fromJson(json, User.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void clearSession() {
        editor.remove(AppConstants.KEY_TOKEN);
        editor.remove(AppConstants.KEY_TOKEN_EXPIRY);
        editor.remove(AppConstants.KEY_USER_JSON);
        editor.apply();
    }

    /** @deprecated use clearSession */
    public void clearToken() { clearSession(); }

    public void saveToken(String token, long expiry) {
        editor.putString(AppConstants.KEY_TOKEN, token);
        editor.putLong(AppConstants.KEY_TOKEN_EXPIRY, System.currentTimeMillis() + expiry * 1000L);
        editor.apply();
    }
}
