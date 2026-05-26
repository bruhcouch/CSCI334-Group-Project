package com.example.parking.dto.response;

public class SpotterSpaceResponse {
    private Long id;
    private String lotName;
    private String zone;
    private String bayNumber;
    private String displayName;
    private String sensorId;
    private boolean occupied;

    public Long getId() {
        return id;
    }

    public String getLotName() {
        return lotName;
    }

    public String getZone() {
        return zone;
    }

    public String getBayNumber() {
        return bayNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSensorId() {
        return sensorId;
    }

    public boolean isOccupied() {
        return occupied;
    }
}
