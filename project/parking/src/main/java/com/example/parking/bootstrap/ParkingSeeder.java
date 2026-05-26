package com.example.parking.bootstrap;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.parking.model.Parking;
import com.example.parking.repository.ParkingRepository;

@Component
public class ParkingSeeder implements CommandLineRunner {

    private final ParkingRepository parkingRepository;
    private static final int NUMBER_OF_ENTRIES = 100;
    private static final List<LotSpec> LOTS = List.of(
            new LotSpec("P1", 25, 3),
            new LotSpec("P2", 15, 1),
            new LotSpec("P3", 20, 1),
            new LotSpec("P4", 15, 1),
            new LotSpec("P5", 15, 1),
            new LotSpec("P8", 10, 1));

    public ParkingSeeder(ParkingRepository parkingRepository) {
        this.parkingRepository = parkingRepository;
    }

    @Override
    public void run(String... args) {
        if (parkingRepository.count() > 0) {
            return;
        }

        for (int i = 0; i < NUMBER_OF_ENTRIES; i++) {

            Parking parking = new Parking();
            SpaceRef space = spaceFor(i);

            parking.setAccountId((long) ((i % 12) + 1));
            parking.setParkingLot(space.lotName());
            parking.setParkingSpace(space.sensorId());
            parking.setVehicle("UOW" + String.format("%03d", i + 1));

            LocalDateTime startTime = i < 90
                    ? LocalDateTime.now().minusDays(7 - (i % 7)).withHour(8 + (i % 9)).withMinute((i * 7) % 60).withSecond(0).withNano(0)
                    : LocalDateTime.now().plusHours(2 + (i - 90) * 3L).withSecond(0).withNano(0);

            parking.setStartTime(startTime);
            parking.setEndTime(startTime.plusHours(1 + (i % 4)));
            parking.setCost(Math.round((2.50 + (i % 4) * 2.50) * 100.0) / 100.0);
            parking.setStatus(i < 90 ? "COMPLETED" : "RESERVED");
            parking.setCreatedAt(LocalDateTime.now());

            parkingRepository.save(parking);
        }
    }

    private SpaceRef spaceFor(int index) {
        int offset = index % NUMBER_OF_ENTRIES;
        for (LotSpec lot : LOTS) {
            if (offset < lot.totalSpaces()) {
                String bay = String.format("%03d", offset + 1);
                String type = offset < lot.accessibleSpaces() ? "ACC" : "GEN";
                return new SpaceRef(lot.lotName(), "UOW-" + lot.lotName() + "-" + type + "-" + bay);
            }
            offset -= lot.totalSpaces();
        }
        LotSpec fallback = LOTS.get(0);
        return new SpaceRef(fallback.lotName(), "UOW-P1-ACC-001");
    }

    private record LotSpec(String lotName, int totalSpaces, int accessibleSpaces) {
    }

    private record SpaceRef(String lotName, String sensorId) {
    }
}
