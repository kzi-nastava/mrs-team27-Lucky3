package com.example.mobile.models;

/**
 * Request model for updating vehicle pricing.
 * Maps to backend UpdateVehiclePriceRequest DTO.
 */
public class UpdateVehiclePriceRequest {
    private String vehicleType;
    private Double baseFare;
    private Double pricePerKm;

    public UpdateVehiclePriceRequest() {}

    public UpdateVehiclePriceRequest(String vehicleType, Double baseFare, Double pricePerKm) {
        this.vehicleType = vehicleType;
        this.baseFare = baseFare;
        this.pricePerKm = pricePerKm;
    }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Double getBaseFare() { return baseFare; }
    public void setBaseFare(Double baseFare) { this.baseFare = baseFare; }

    public Double getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(Double pricePerKm) { this.pricePerKm = pricePerKm; }
}
