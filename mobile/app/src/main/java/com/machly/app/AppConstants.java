package com.machly.app;

public class AppConstants {
    public static final String PHONE_BASE_URL = "http://10.26.11.172:3000/api/";
    public static final String EMULATOR_BASE_URL = "http://10.0.2.2:3000/api/";
    public static final String PREF_NAME = "machly_prefs";
    public static final String KEY_TOKEN = "jwt_token";
    public static final String KEY_TOKEN_EXPIRY = "token_expiry";
    public static final String KEY_USER_JSON = "user_json";
    public static final long CONNECT_TIMEOUT = 30;
    public static final long READ_TIMEOUT = 30;

    public static String getBaseUrl() {
        return isEmulator() ? EMULATOR_BASE_URL : PHONE_BASE_URL;
    }

    public static String getOriginUrl() {
        String base = getBaseUrl();
        if (base.endsWith("/api/")) return base.substring(0, base.length() - 5);
        if (base.endsWith("/api")) return base.substring(0, base.length() - 4);
        return base;
    }

    public static boolean isEmulator() {
        String fingerprint = android.os.Build.FINGERPRINT;
        String model = android.os.Build.MODEL;
        String product = android.os.Build.PRODUCT;
        String hardware = android.os.Build.HARDWARE;
        String brand = android.os.Build.BRAND;
        String device = android.os.Build.DEVICE;
        return (fingerprint != null && (
                fingerprint.startsWith("generic")
                        || fingerprint.toLowerCase(java.util.Locale.ROOT).contains("emulator")
                        || fingerprint.toLowerCase(java.util.Locale.ROOT).contains("generic")))
                || (model != null && (
                model.toLowerCase(java.util.Locale.ROOT).contains("emulator")
                        || model.toLowerCase(java.util.Locale.ROOT).contains("sdk_gphone")
                        || model.equalsIgnoreCase("google_sdk")
                        || model.toLowerCase(java.util.Locale.ROOT).contains("android sdk built for")))
                || (product != null && (
                product.toLowerCase(java.util.Locale.ROOT).contains("sdk")
                        || product.toLowerCase(java.util.Locale.ROOT).contains("emulator")
                        || product.toLowerCase(java.util.Locale.ROOT).contains("google_sdk")))
                || "goldfish".equals(hardware)
                || "ranchu".equals(hardware)
                || (brand != null && brand.startsWith("generic") && device != null && device.startsWith("generic"));
    }
}
