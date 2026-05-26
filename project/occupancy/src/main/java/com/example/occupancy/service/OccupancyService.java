package com.example.occupancy.service;

import java.time.Instant;

import com.example.occupancy.repository.OccupancyRepository;

public class OccupancyService {
    private OccupancyRepository occupancyRepository;
    private PredictionService predictionService;
    
    public OccupancyService(OccupancyRepository occupancyRepository, PredictionService predictionService) {
    	this.occupancyRepository = occupancyRepository;
    	this.predictionService = predictionService;
    }
    
    public int calculateCurrentOccupancy(String lotId) {
    	return this.occupancyRepository.findCurrentByLotId(lotId).occupiedSpaces();
    }
    
    public auto analyseHistory(String lotId) {
    	return this.occupancyRepository.findHistoryByLotId(lotId);
    }
    
    public double predictFutureOccupancy(String lotId, Instant time) {
    	return this.predictionService.predictOccupancy(this.occupancyRepository.findHistoryByLotId(lotId), time);
    }
}
