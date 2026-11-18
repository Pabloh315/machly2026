package com.machly.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Equipment implements Serializable {
    private int id;
    @SerializedName("owner_id") private int ownerId;
    private String type;
    private String category;
    private String name;
    private String brand;
    private String model;
    private int year;
    @SerializedName("price_day") private double priceDay;
    private double deposit;
    private String city;
    private String province;
    private String description;
    private List<String> features;
    private String image;
    private String status;
    private int featured;
    @SerializedName("owner_name") private String ownerName;
    @SerializedName("owner_phone") private String ownerPhone;
    private double rating;
    @SerializedName("reviews_count") private int reviewsCount;
    @SerializedName("is_favorite") private boolean favorite;

    public int getId() { return id; }
    public int getOwnerId() { return ownerId; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public double getPriceDay() { return priceDay; }
    public double getDeposit() { return deposit; }
    public String getCity() { return city; }
    public String getProvince() { return province; }
    public String getDescription() { return description; }
    public List<String> getFeatures() { return features == null ? Collections.emptyList() : features; }
    public String getImage() { return image; }
    public String getStatus() { return status; }
    public int getFeatured() { return featured; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerPhone() { return ownerPhone; }
    public double getRating() { return rating; }
    public int getReviewsCount() { return reviewsCount; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) return name;
        String brandModel = ((brand == null ? "" : brand) + " " + (model == null ? "" : model)).trim();
        return brandModel.isEmpty() ? "Equipo Machly" : brandModel;
    }

    public String getLocationLabel() {
        String c = city == null ? "" : city;
        String p = province == null ? "" : province;
        if (c.isEmpty()) return p;
        if (p.isEmpty()) return c;
        return c + ", " + p;
    }

    public boolean isAvailable() {
        return "available".equalsIgnoreCase(status);
    }

    public String getStatusLabel() {
        if (status == null) return "";
        switch (status) {
            case "available": return "Disponible";
            case "maintenance": return "Mantenimiento";
            case "inactive": return "Inactivo";
            default: return status;
        }
    }
}
