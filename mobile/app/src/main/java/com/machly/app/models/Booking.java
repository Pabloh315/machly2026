package com.machly.app.models;

import com.google.gson.annotations.SerializedName;

public class Booking {
    private int id;
    @SerializedName("equipment_id") private int equipmentId;
    @SerializedName("renter_id") private int renterId;
    @SerializedName("owner_id") private int ownerId;
    @SerializedName("start_date") private String startDate;
    @SerializedName("end_date") private String endDate;
    private int days;
    private double subtotal;
    @SerializedName("service_fee") private double serviceFee;
    private double total;
    private String message;
    private String status;
    @SerializedName("equipment_name") private String equipmentName;
    private String image;
    private String city;
    @SerializedName("price_day") private double priceDay;
    @SerializedName("renter_name") private String renterName;
    @SerializedName("renter_phone") private String renterPhone;

    public int getId() { return id; }
    public int getEquipmentId() { return equipmentId; }
    public int getRenterId() { return renterId; }
    public int getOwnerId() { return ownerId; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public int getDays() { return days; }
    public double getSubtotal() { return subtotal; }
    public double getServiceFee() { return serviceFee; }
    public double getTotal() { return total; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public String getEquipmentName() { return equipmentName; }
    public String getImage() { return image; }
    public String getCity() { return city; }
    public double getPriceDay() { return priceDay; }
    public String getRenterName() { return renterName; }
    public String getRenterPhone() { return renterPhone; }

    public String getStatusLabel() {
        if (status == null) return "";
        switch (status) {
            case "requested": return "Solicitada";
            case "confirmed": return "Confirmada";
            case "active": return "En curso";
            case "completed": return "Completada";
            case "cancelled": return "Cancelada";
            case "rejected": return "Rechazada";
            default: return status;
        }
    }
}
