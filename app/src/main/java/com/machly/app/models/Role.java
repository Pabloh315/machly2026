package com.machly.app.models;

import android.content.ContentValues;

public class Role {
    private long id;
    private Integer roleIdServer;
    private String name;
    private String description;

    public Role() {}

    public Role(Integer roleIdServer, String name, String description) {
        this.roleIdServer = roleIdServer;
        this.name = name;
        this.description = description;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Integer getRoleIdServer() { return roleIdServer; }
    public void setRoleIdServer(Integer roleIdServer) { this.roleIdServer = roleIdServer; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        if (roleIdServer != null) values.put("roleIdServer", roleIdServer);
        values.put("name", name);
        values.put("description", description);
        return values;
    }
}
