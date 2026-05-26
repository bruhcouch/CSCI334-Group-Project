package com.example.occupancy.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.occupancy.dto.OccupancyPredictionResponse;
import com.example.occupancy.model.OccupancyRecord;
import com.example.occupancy.repository.OccupancyRepository;

@Service
public class OccupancyService {
    private final OccupancyRepository occupancyRepository;
    private final PredictionService predictionService;
    
    public OccupancyService(OccupancyRepository occupancyRepository, PredictionService predictionService) {
    	this.occupancyRepository = occupancyRepository;
    	this.predictionService = predictionService;
    }
    
    public int calculateCurrentOccupancy(String lotId) {
    	return occupancyRepository.findCurrentByLotId(lotId).occupiedSpaces();
    }
    
    public List<OccupancyRecord> analyseHistory(String lotId) {
    	return occupancyRepository.findHistoryByLotId(lotId);
    }
    
    public double predictFutureOccupancy(String lotId, Instant time) {
    	return this.predictionService.predictOccupancy(this.occupancyRepository.findHistoryByLotId(lotId), time);
    }

    public OccupancyPredictionResponse predictLot(String lotId, Instant time) {
        OccupancyRecord current = occupancyRepository.findCurrentByLotId(lotId);
        double predictedRate = predictFutureOccupancy(lotId, time);
        int available = Math.max(0, current.totalSpaces() - (int) Math.round(predictedRate * current.totalSpaces()));
        return new OccupancyPredictionResponse(
                current.lotId(),
                predictedRate,
                Math.round((1.0 - predictedRate) * 10000.0) / 10000.0,
                available,
                current.totalSpaces(),
                time);
    }

    public List<OccupancyPredictionResponse> predictAll(Instant time) {
        Instant targetTime = time == null ? Instant.now().plus(Duration.ofHours(1)) : time;
        return occupancyRepository.findLotIds().stream()
                .map(lotId -> predictLot(lotId, targetTime))
                .sorted(Comparator.comparing(OccupancyPredictionResponse::getAvailabilityProbability).reversed())
                .toList();
    }
}
