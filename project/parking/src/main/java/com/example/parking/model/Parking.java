package com.example.parking.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Parking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public Parking() {}

    public Parking(Long accountId, String parkingLot, String parkingSpace, String vehicle,
                   LocalDateTime startTime, LocalDateTime endTime, Double cost, String status) {
        this.accountId = accountId;
        this.parkingLot = parkingLot;
        this.parkingSpace = parkingSpace;
        this.vehicle = vehicle;
        this.startTime = startTime;
        this.endTime = endTime;
        this.cost = cost;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getParkingLot() {
        return parkingLot;
    }

    public String getParkingSpace() {
        return parkingSpace;
    }

    public String getVehicle() {
        return vehicle;
    }

    public String getMobilityParkingPermitNumber() {
        return mobilityParkingPermitNumber;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Double getCost() {
        return cost;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setParkingLot(String parkingLot) {
        this.parkingLot = parkingLot;
    }

    public void setParkingSpace(String parkingSpace) {
        this.parkingSpace = parkingSpace;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public void setMobilityParkingPermitNumber(String mobilityParkingPermitNumber) {
        this.mobilityParkingPermitNumber = mobilityParkingPermitNumber;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
