package com.example.parking.dto.response;

import java.time.LocalDateTime;

import com.example.parking.model.Parking;

public class ParkingResponse {

    private Long id;

    private Long accountId;

    private String parkingLot;

    private String parkingSpace;

    private String vehicle;

    private String mobilityParkingPermitNumber;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double cost;

    private String status;

    private LocalDateTime createdAt;

    public ParkingResponse() {}

    public ParkingResponse(Parking parking) {
        this.id = parking.getId();
        this.accountId = parking.getAccountId();
        this.parkingLot = parking.getParkingLot();
        this.parkingSpace = parking.getParkingSpace();
        this.vehicle = parking.getVehicle();
        this.mobilityParkingPermitNumber = parking.getMobilityParkingPermitNumber();
        this.startTime = parking.getStartTime();
        this.endTime = parking.getEndTime();
        this.cost = parking.getCost();
        this.status = parking.getStatus();
        this.createdAt = parking.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
