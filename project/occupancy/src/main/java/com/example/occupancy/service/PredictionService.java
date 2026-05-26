package com.example.occupancy.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.occupancy.model.OccupancyRecord;

@Service
public class PredictionService {
	private static final ZoneId CAMPUS_ZONE = ZoneId.of("Australia/Sydney");

	public double predictOccupancy(List<OccupancyRecord> records, Instant time) {
		if (records == null || records.isEmpty()) {
			return 0;
		}

		ZonedDateTime predictionLocal = time.atZone(CAMPUS_ZONE);
		double weightedTotal = 0;
		double totalWeight = 0;

		for (OccupancyRecord record : records) {
			ZonedDateTime recordLocal = record.timestamp().atZone(CAMPUS_ZONE);
			int hourDistance = Math.abs(predictionLocal.getHour() - recordLocal.getHour());
			int circularHourDistance = Math.min(hourDistance, 24 - hourDistance);
			long dayDistance = Math.abs(Duration.between(record.timestamp(), time).toDays());
			boolean sameDayType = (predictionLocal.getDayOfWeek().getValue() >= 6) == (recordLocal.getDayOfWeek().getValue() >= 6);
			double timeWeight = Math.exp(-Math.pow(circularHourDistance, 2) / 18.0);
			double recencyWeight = 1.0 / (1.0 + dayDistance / 7.0);
			double dayWeight = sameDayType ? 1.2 : 0.75;
			double weight = timeWeight * recencyWeight * dayWeight;
			weightedTotal += record.getOccupancyRate() * weight;
			totalWeight += weight;
		}

		if (totalWeight == 0) {
			return 0;
		}

		return Math.round((weightedTotal / totalWeight) * 10000.0) / 10000.0;
	}
}
