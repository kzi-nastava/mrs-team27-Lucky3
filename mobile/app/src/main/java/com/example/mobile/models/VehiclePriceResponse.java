package com.example.mobile.models;

/**
 * Response model for vehicle pricing data.
 * Maps to backend VehiclePriceResponse DTO.
 */
public class VehiclePriceResponse {
    private Long id;
    private String vehicleType;
    private Double baseFare;
    private Double pricePerKm;

    public VehiclePriceResponse() {}

    public VehiclePriceResponse(Long id, String vehicleType, Double baseFare, Double pricePerKm) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.baseFare = baseFare;
        this.pricePerKm = pricePerKm;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Double getBaseFare() { return baseFare; }
    public void setBaseFare(Double baseFare) { this.baseFare = baseFare; }

    public Double getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(Double pricePerKm) { this.pricePerKm = pricePerKm; }
}
