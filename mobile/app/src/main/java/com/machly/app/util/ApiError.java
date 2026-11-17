package com.machly.app.util;

import org.json.JSONObject;

import retrofit2.Response;

public final class ApiError {
    private ApiError() {}

    public static String message(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                JSONObject json = new JSONObject(raw);
                if (json.has("error")) return json.getString("error");
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
