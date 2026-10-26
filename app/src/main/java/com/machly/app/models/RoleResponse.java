package com.machly.app.models;

import com.google.gson.annotations.SerializedName;

public class RoleResponse {
    @SerializedName("roleId")
    private int roleId;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    public int getRoleId() { return roleId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
