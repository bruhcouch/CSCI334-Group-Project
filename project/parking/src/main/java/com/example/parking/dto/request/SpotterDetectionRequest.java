package com.example.parking.dto.request;

public class SpotterDetectionRequest {
    private final boolean occupied;
    private final double confidence;
    private final String source;

    public SpotterDetectionRequest(boolean occupied, double confidence, String source) {
        this.occupied = occupied;
        this.confidence = confidence;
        this.source = source;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSource() {
        return source;
    }
}
