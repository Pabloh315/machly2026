package com.machly.app.models;

public class BookingRequest {
    private final int equipmentId;
    private final String startDate;
    private final String endDate;
    private final String message;

    public BookingRequest(int equipmentId, String startDate, String endDate, String message) {
        this.equipmentId = equipmentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.message = message;
    }
}
