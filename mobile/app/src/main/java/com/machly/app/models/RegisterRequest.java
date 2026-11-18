package com.machly.app.models;

public class RegisterRequest {
    private final String fullName;
    private final String email;
    private final String phone;
    private final String password;
    private final String role;

    public RegisterRequest(String fullName, String email, String phone, String password, String role) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }
}
