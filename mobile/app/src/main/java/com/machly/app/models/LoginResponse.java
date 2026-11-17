package com.machly.app.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private String token;
    @SerializedName("expiresIn") private long expiresIn;
    private User user;

    public String getToken() { return token; }
    public long getExpiresIn() { return expiresIn; }
    public User getUser() { return user; }
}
