package com.machly.app.domain;

public final class SessionState {
    private SessionState() { }
    public static boolean isValid(String token, long expiryEpochMillis, long nowEpochMillis) {
        return token != null && !token.trim().isEmpty() && expiryEpochMillis > nowEpochMillis;
    }
}
