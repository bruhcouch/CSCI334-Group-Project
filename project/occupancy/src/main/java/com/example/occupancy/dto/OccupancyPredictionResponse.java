package com.example.occupancy.dto;

import java.time.Instant;

public class OccupancyPredictionResponse {
    private final String lotId;
    private final double predictedOccupancyRate;
    private final double availabilityProbability;
    private final int estimatedAvailableSpaces;
    private final int totalSpaces;
    private final Instant targetTime;

    public OccupancyPredictionResponse(
            String lotId,
            double predictedOccupancyRate,
            double availabilityProbability,
            int estimatedAvailableSpaces,
            int totalSpaces,
            Instant targetTime) {
        this.lotId = lotId;
        this.predictedOccupancyRate = predictedOccupancyRate;
        this.availabilityProbability = availabilityProbability;
        this.estimatedAvailableSpaces = estimatedAvailableSpaces;
        this.totalSpaces = totalSpaces;
        this.targetTime = targetTime;
    }

    public String getLotId() {
        return lotId;
    }

    public double getPredictedOccupancyRate() {
        return predictedOccupancyRate;
    }

    public double getAvailabilityProbability() {
        return availabilityProbability;
    }

    public int getEstimatedAvailableSpaces() {
        return estimatedAvailableSpaces;
    }

    public int getTotalSpaces() {
        return totalSpaces;
    }

    public Instant getTargetTime() {
        return targetTime;
    }
}
