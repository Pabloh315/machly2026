package com.machly.app.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.util.Base64;

public class BCryptUtils {

    // Using SHA-256 as a fallback since external dependencies are causing issues.
    // In a real production app, use a strong library like Argon2 or BCrypt.
    
    public static String hashPassword(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plain.getBytes("UTF-8"));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean checkPassword(String plain, String hash) {
        String newHash = hashPassword(plain);
        return newHash != null && newHash.equals(hash);
    }
}
