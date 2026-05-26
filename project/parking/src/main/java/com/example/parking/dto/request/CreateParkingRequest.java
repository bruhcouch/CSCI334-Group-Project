package com.example.parking.dto.request;

import java.time.LocalDateTime;

public class CreateParkingRequest {

    private Long accountId;

    private String parkingLot;

    private String parkingSpace;

    private String vehicle;

    private String mobilityParkingPermitNumber;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double cost;

    private String status;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getParkingLot() {
        return parkingLot;
    }

    public void setParkingLot(String parkingLot) {
        this.parkingLot = parkingLot;
    }

    public String getParkingSpace() {
        return parkingSpace;
    }

    public void setParkingSpace(String parkingSpace) {
        this.parkingSpace = parkingSpace;
    }

    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public String getMobilityParkingPermitNumber() {
        return mobilityParkingPermitNumber;
    }

    public void setMobilityParkingPermitNumber(String mobilityParkingPermitNumber) {
        this.mobilityParkingPermitNumber = mobilityParkingPermitNumber;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
