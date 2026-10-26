package com.machly.app.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("expiresIn")
    private long expiresIn;

    @SerializedName("user")
    private UserResponse user;

    public String getToken() { return token; }
    public long getExpiresIn() { return expiresIn; }
    public UserResponse getUser() { return user; }
}
