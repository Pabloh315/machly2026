package com.machly.app.models;

import android.content.ContentValues;

public class User {
    private long id;
    private Integer userIdServer;
    private String fullName;
    private String email;
    private String passwordHash;
    private long roleIdLocal;
    private String roleName; // For display purposes (joined)
    private long lastUpdated;

    public User() {}

    public User(Integer userIdServer, String fullName, String email, String passwordHash, long roleIdLocal, long lastUpdated) {
        this.userIdServer = userIdServer;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleIdLocal = roleIdLocal;
        this.lastUpdated = lastUpdated;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Integer getUserIdServer() { return userIdServer; }
    public void setUserIdServer(Integer userIdServer) { this.userIdServer = userIdServer; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public long getRoleIdLocal() { return roleIdLocal; }
    public void setRoleIdLocal(long roleIdLocal) { this.roleIdLocal = roleIdLocal; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        if (userIdServer != null) values.put("userIdServer", userIdServer);
        values.put("fullName", fullName);
        values.put("email", email);
        values.put("passwordHash", passwordHash);
        values.put("roleIdLocal", roleIdLocal);
        values.put("lastUpdated", lastUpdated);
        return values;
    }
}
