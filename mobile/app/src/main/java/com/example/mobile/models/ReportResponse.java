package com.example.mobile.models;

import java.util.List;

public class ReportResponse {
    private List<DailyReport> dailyData;

    private Double cumulativeRides;
    private Double cumulativeKilometers;
    private Double cumulativeMoney;

    private Double averageRides;
    private Double averageKilometers;
    private Double averageMoney;

    private int pendingRides;
    private int activeRides;
    private int inProgressRides;
    private int finishedRides;
    private int rejectedRides;
    private int panicRides;
    private int cancelledRides;

    public ReportResponse() {}

    public List<DailyReport> getDailyData() {
        return dailyData;
    }

    public void setDailyData(List<DailyReport> dailyData) {
        this.dailyData = dailyData;
    }

    public Double getCumulativeRides() {
        return cumulativeRides;
    }

    public void setCumulativeRides(Double cumulativeRides) {
        this.cumulativeRides = cumulativeRides;
    }

    public Double getCumulativeKilometers() {
        return cumulativeKilometers;
    }

    public void setCumulativeKilometers(Double cumulativeKilometers) {
        this.cumulativeKilometers = cumulativeKilometers;
    }

    public Double getCumulativeMoney() {
        return cumulativeMoney;
    }

    public void setCumulativeMoney(Double cumulativeMoney) {
        this.cumulativeMoney = cumulativeMoney;
    }

    public Double getAverageRides() {
        return averageRides;
    }

    public void setAverageRides(Double averageRides) {
        this.averageRides = averageRides;
    }

    public Double getAverageKilometers() {
        return averageKilometers;
    }

    public void setAverageKilometers(Double averageKilometers) {
        this.averageKilometers = averageKilometers;
    }

    public Double getAverageMoney() {
        return averageMoney;
    }

    public void setAverageMoney(Double averageMoney) {
        this.averageMoney = averageMoney;
    }

    public int getPendingRides() {
        return pendingRides;
    }

    public void setPendingRides(int pendingRides) {
        this.pendingRides = pendingRides;
    }

    public int getActiveRides() {
        return activeRides;
    }

    public void setActiveRides(int activeRides) {
        this.activeRides = activeRides;
    }

    public int getInProgressRides() {
        return inProgressRides;
    }

    public void setInProgressRides(int inProgressRides) {
        this.inProgressRides = inProgressRides;
    }

    public int getFinishedRides() {
        return finishedRides;
    }

    public void setFinishedRides(int finishedRides) {
        this.finishedRides = finishedRides;
    }

    public int getRejectedRides() {
        return rejectedRides;
    }

    public void setRejectedRides(int rejectedRides) {
        this.rejectedRides = rejectedRides;
    }

    public int getPanicRides() {
        return panicRides;
    }

    public void setPanicRides(int panicRides) {
        this.panicRides = panicRides;
    }

    public int getCancelledRides() {
        return cancelledRides;
    }

    public void setCancelledRides(int cancelledRides) {
        this.cancelledRides = cancelledRides;
    }
}
