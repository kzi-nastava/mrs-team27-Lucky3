package com.example.mobile.models;

import com.google.gson.annotations.SerializedName;

public class DailyReport {
    private String date; // E.g. "2024-05-15"
    @SerializedName("rideCount")
    private Double rides;
    private Double kilometers;
    private Double money;

    public DailyReport() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Double getRides() {
        return rides;
    }

    public void setRides(Double rides) {
        this.rides = rides;
    }

    public Double getKilometers() {
        return kilometers;
    }

    public void setKilometers(Double kilometers) {
        this.kilometers = kilometers;
    }

    public Double getMoney() {
        return money;
    }

    public void setMoney(Double money) {
        this.money = money;
    }
}
