package com.machly.app.storage;

import android.content.Context;
import android.content.SharedPreferences;
import com.machly.app.AppConstants;

public class PrefsManager {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public PrefsManager(Context context) {
        this.context = context;
        // TODO: Use EncryptedSharedPreferences in production
        prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveToken(String token, long expiry) {
        editor.putString(AppConstants.KEY_TOKEN, token);
        editor.putLong(AppConstants.KEY_TOKEN_EXPIRY, expiry);
        editor.apply();
    }

    public String getToken() {
        return prefs.getString(AppConstants.KEY_TOKEN, null);
    }

    public void clearToken() {
        editor.remove(AppConstants.KEY_TOKEN);
        editor.remove(AppConstants.KEY_TOKEN_EXPIRY);
        editor.apply();
    }

    public void setLastSync(long timestamp) {
        editor.putLong(AppConstants.KEY_LAST_SYNC, timestamp);
        editor.apply();
    }

    public long getLastSync() {
        return prefs.getLong(AppConstants.KEY_LAST_SYNC, 0);
    }
}
