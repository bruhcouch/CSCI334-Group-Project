package com.example.occupancy.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.occupancy.dto.OccupancyPredictionResponse;
import com.example.occupancy.model.OccupancyRecord;
import com.example.occupancy.service.OccupancyService;

@RestController
@RequestMapping({"/occupancy", "/api/occupancy"})
public class OccupancyController {
    private final OccupancyService occupancyService;

	public OccupancyController(OccupancyService occupancyService) {
		this.occupancyService = occupancyService;
    }
	
	@GetMapping("/{lotId}")
	public int getCurrentOccupancy(@PathVariable String lotId) {
		return this.occupancyService.calculateCurrentOccupancy(lotId);
	}
	
	@GetMapping("/{lotId}/history")
	public List<OccupancyRecord> getOccupancyHistory(@PathVariable String lotId) {
		return this.occupancyService.analyseHistory(lotId);
	}
	
	@GetMapping("/{lotId}/predict")
	public OccupancyPredictionResponse getPredictedOccupancy(
			@PathVariable String lotId,
			@RequestParam(required = false) Instant time) {
		Instant targetTime = time == null ? Instant.now().plus(Duration.ofHours(1)) : time;
		return this.occupancyService.predictLot(lotId, targetTime);
	}

	@GetMapping("/predictions")
	public List<OccupancyPredictionResponse> getPredictions(@RequestParam(required = false) Instant time) {
		return this.occupancyService.predictAll(time);
	}
}
