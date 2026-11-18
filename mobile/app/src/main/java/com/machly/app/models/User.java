package com.machly.app.models;

import com.google.gson.annotations.SerializedName;

public class User {
    private int id;
    @SerializedName("fullName") private String fullName;
    private String email;
    private String phone;
    private String role;
    @SerializedName("createdAt") private String createdAt;

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getCreatedAt() { return createdAt; }

    public boolean isOwner() {
        return "owner".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
    }

    public boolean isRenter() {
        return "renter".equalsIgnoreCase(role);
    }

    public String getRoleLabel() {
        if (isOwner() && !"admin".equalsIgnoreCase(role)) return "Propietario";
        if ("admin".equalsIgnoreCase(role)) return "Administrador";
        return "Arrendatario";
    }

    public String getFirstName() {
        if (fullName == null || fullName.trim().isEmpty()) return "Usuario";
        return fullName.trim().split("\\s+")[0];
    }
}
