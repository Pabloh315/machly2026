package com.machly.app.models;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    @SerializedName("userId")
    private int userId;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private RoleResponse role;

    @SerializedName("passwordHash")
    private String passwordHash;

    @SerializedName("lastUpdated")
    private long lastUpdated;

    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public RoleResponse getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public long getLastUpdated() { return lastUpdated; }
}
