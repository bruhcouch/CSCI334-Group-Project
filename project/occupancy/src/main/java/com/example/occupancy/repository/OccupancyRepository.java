package com.example.occupancy.repository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.example.occupancy.model.OccupancyRecord;

import jakarta.annotation.PostConstruct;

@Repository
public class OccupancyRepository {
    private final Map<String, List<OccupancyRecord>> historyByLot = new ConcurrentHashMap<>();
    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Australia/Sydney");

    @PostConstruct
    public void seed() {
        if (!historyByLot.isEmpty()) {
            return;
        }

        List<LotSeed> lots = List.of(
                new LotSeed("P1", 25, 0.78),
                new LotSeed("P2", 15, 0.62),
                new LotSeed("P3", 20, 0.74),
                new LotSeed("P4", 15, 0.56),
                new LotSeed("P5", 15, 0.68),
                new LotSeed("P8", 10, 0.71));

        Instant now = Instant.now();
        for (LotSeed lot : lots) {
            for (int index = 96; index >= 0; index--) {
                Instant timestamp = now.minusSeconds(index * 30L * 60L);
                ZonedDateTime local = timestamp.atZone(CAMPUS_ZONE);
                double hour = local.getHour() + local.getMinute() / 60.0;
                double morning = Math.exp(-Math.pow(hour - 10.0, 2) / 8.0);
                double afternoon = Math.exp(-Math.pow(hour - 14.0, 2) / 12.0);
                double weekendFactor = local.getDayOfWeek().getValue() >= 6 ? 0.45 : 1.0;
                double pulse = Math.sin((index + lot.totalSpaces()) * 0.63) * 0.06;
                double rate = clamp((0.18 + lot.baseDemand() * Math.max(morning, afternoon)) * weekendFactor + pulse, 0.04, 0.98);
                int occupied = (int) Math.round(rate * lot.totalSpaces());
                save(new OccupancyRecord(lot.lotId() + "-" + timestamp.toEpochMilli(), lot.lotId(), occupied, lot.totalSpaces(), timestamp));
            }
        }
    }

    public OccupancyRecord findCurrentByLotId(String lotId) {
        return findHistoryByLotId(lotId).stream()
                .max(Comparator.comparing(OccupancyRecord::timestamp))
                .orElseThrow(() -> new IllegalArgumentException("Unknown lot " + lotId));
    }

    public List<OccupancyRecord> findHistoryByLotId(String lotId) {
        return historyByLot.getOrDefault(key(lotId), List.of()).stream()
                .sorted(Comparator.comparing(OccupancyRecord::timestamp))
                .toList();
    }

    public List<OccupancyRecord> findAllCurrent() {
        return historyByLot.values().stream()
                .map(records -> records.stream()
                        .max(Comparator.comparing(OccupancyRecord::timestamp))
                        .orElse(null))
                .filter(record -> record != null)
                .sorted(Comparator.comparing(OccupancyRecord::lotId))
                .toList();
    }

    public List<String> findLotIds() {
        return findAllCurrent().stream()
                .map(OccupancyRecord::lotId)
                .toList();
    }

    public OccupancyRecord save(OccupancyRecord record) {
        historyByLot.computeIfAbsent(key(record.lotId()), ignored -> new ArrayList<>()).add(record);
        return record;
    }

    private String key(String lotId) {
        return lotId == null ? "" : lotId.toLowerCase();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record LotSeed(String lotId, int totalSpaces, double baseDemand) {
    }
}
