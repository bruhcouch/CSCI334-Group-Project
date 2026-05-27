package com.example.parking.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.parking.dto.response.SpotterSpaceResponse;
import com.example.parking.model.Parking;
import com.example.parking.repository.ParkingRepository;

@Service
public class ParkingService {
    public static final String RESERVED = "RESERVED";
    public static final String ACTIVE = "ACTIVE";
    public static final String EXPIRED = "EXPIRED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";

    private final ParkingRepository parkingRepository;
    private final SpotterClient spotterClient;
    private final AccountClient accountClient;

    public ParkingService(ParkingRepository parkingRepository, SpotterClient spotterClient, AccountClient accountClient) {
        this.parkingRepository = parkingRepository;
        this.spotterClient = spotterClient;
        this.accountClient = accountClient;
    }

    @Transactional
    public Parking createParking(Parking parking) {
        maintainBookingStatuses();
        validateParkingWindow(parking);
        SpotterSpaceResponse space = spotterClient.getSpace(parking.getParkingSpace());
        parking.setParkingSpace(space.getSensorId());
        validateAccessiblePermit(parking, space);
        if (parking.getParkingLot() == null || parking.getParkingLot().isBlank()) {
            parking.setParkingLot(space.getLotName());
        }
        parking.setStatus(statusFor(parking));
        parking.setCreatedAt(LocalDateTime.now());
        preventOverlap(parking, null);

        if (ACTIVE.equals(parking.getStatus())) {
            if (space.isOccupied()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Space is currently occupied");
            }
            spotterClient.updateOccupancy(parking.getParkingSpace(), true, "booking-active");
        }

        return parkingRepository.save(parking);
    }

    public List<Parking> getAllParking() {
        maintainBookingStatuses();
        return parkingRepository.findAll();
    }

    public List<Parking> getParkingForAccount(Long accountId) {
        maintainBookingStatuses();
        return parkingRepository.findByAccountIdOrderByStartTimeDesc(accountId);
    }

    public Parking getParkingById(Long id) {
        maintainBookingStatuses();
        return parkingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking record not found"));
    }

    @Transactional
    public Parking updateParking(Long id, Parking updatedParking) {
        maintainBookingStatuses();
        Parking parking = getParkingById(id);
        boolean wasActive = ACTIVE.equals(parking.getStatus());
        String previousSpace = parking.getParkingSpace();

        if (updatedParking.getAccountId() != null) {
            parking.setAccountId(updatedParking.getAccountId());
        }
        if (updatedParking.getParkingLot() != null) {
            parking.setParkingLot(updatedParking.getParkingLot());
        }
        if (updatedParking.getParkingSpace() != null) {
            parking.setParkingSpace(updatedParking.getParkingSpace());
        }
        if (updatedParking.getVehicle() != null) {
            parking.setVehicle(updatedParking.getVehicle());
        }
        if (updatedParking.getMobilityParkingPermitNumber() != null) {
            parking.setMobilityParkingPermitNumber(updatedParking.getMobilityParkingPermitNumber());
        }
        if (updatedParking.getStartTime() != null) {
            parking.setStartTime(updatedParking.getStartTime());
        }
        if (updatedParking.getEndTime() != null) {
            parking.setEndTime(updatedParking.getEndTime());
        }
        if (updatedParking.getCost() != null) {
            parking.setCost(updatedParking.getCost());
        }
        if (updatedParking.getStatus() != null) {
            parking.setStatus(updatedParking.getStatus());
        }

        validateParkingWindow(parking);
        SpotterSpaceResponse space = spotterClient.getSpace(parking.getParkingSpace());
        parking.setParkingSpace(space.getSensorId());
        validateAccessiblePermit(parking, space);
        if (!CANCELLED.equals(parking.getStatus()) && !COMPLETED.equals(parking.getStatus())) {
            parking.setStatus(statusFor(parking));
            preventOverlap(parking, parking.getId());
        }

        Parking saved = parkingRepository.save(parking);
        syncSpotterAfterStatusChange(saved, wasActive, previousSpace);
        return saved;
    }

    @Transactional
    public Parking cancelParking(Long id) {
        maintainBookingStatuses();
        Parking parking = getParkingById(id);
        boolean wasActive = ACTIVE.equals(parking.getStatus());
        parking.setStatus(CANCELLED);
        Parking saved = parkingRepository.save(parking);
        if (wasActive) {
            releaseSpace(saved);
        }
        return saved;
    }

    @Transactional
    public void deleteParking(Long id) {
        Parking parking = getParkingById(id);
        if (ACTIVE.equals(parking.getStatus())) {
            releaseSpace(parking);
        }
        parkingRepository.delete(parking);
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void maintainBookingStatuses() {
        LocalDateTime now = LocalDateTime.now();

        for (Parking parking : parkingRepository.findByStatusAndStartTimeLessThanEqual(RESERVED, now)) {
            if (parking.getEndTime().isAfter(now)) {
                parking.setStatus(ACTIVE);
                parkingRepository.save(parking);
                spotterClient.updateOccupancy(parking.getParkingSpace(), true, "booking-active");
            }
        }

        for (Parking parking : parkingRepository.findByStatusInAndEndTimeLessThanEqual(Set.of(RESERVED, ACTIVE), now)) {
            boolean wasActive = ACTIVE.equals(parking.getStatus());
            parking.setStatus(EXPIRED);
            parkingRepository.save(parking);
            if (wasActive) {
                releaseSpace(parking);
            }
        }
    }

    private void validateParkingWindow(Parking parking) {
        if (parking.getAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is required");
        }
        if (parking.getParkingSpace() == null || parking.getParkingSpace().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parking space is required");
        }
        if (parking.getVehicle() == null || parking.getVehicle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle is required");
        }
        if (parking.getStartTime() == null || parking.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and end time are required");
        }
        if (!parking.getStartTime().isBefore(parking.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        if (parking.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking cannot end in the past");
        }
        if (!isPremium(parking.getAccountId()) && parking.getStartTime().isBefore(LocalDateTime.now().plusHours(4))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Free accounts can only book spaces at least four hours in advance");
        }
        if (parking.getCost() == null) {
            long minutes = Duration.between(parking.getStartTime(), parking.getEndTime()).toMinutes();
            parking.setCost(Math.max(2.50, Math.round((minutes / 60.0) * 2.50 * 100.0) / 100.0));
        }
    }

    private boolean isPremium(Long accountId) {
        return "PREMIUM".equalsIgnoreCase(accountClient.getSubscription(accountId));
    }

    private void validateAccessiblePermit(Parking parking, SpotterSpaceResponse space) {
        boolean accessibleSpace = "Accessible".equalsIgnoreCase(space.getZone()) || space.getSensorId().contains("-ACC-");
        if (!accessibleSpace) {
            return;
        }

        if (parking.getMobilityParkingPermitNumber() == null || parking.getMobilityParkingPermitNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobility Parking Scheme permit number is required for accessible bays");
        }

        parking.setMobilityParkingPermitNumber(parking.getMobilityParkingPermitNumber().trim());
    }

    private void preventOverlap(Parking booking, Long currentId) {
        List<Parking> candidates = parkingRepository.findByParkingSpaceIgnoreCaseAndStatusIn(
                booking.getParkingSpace(),
                Set.of(RESERVED, ACTIVE));

        boolean overlaps = candidates.stream()
                .filter(candidate -> currentId == null || !candidate.getId().equals(currentId))
                .anyMatch(candidate -> windowsOverlap(
                        booking.getStartTime(),
                        booking.getEndTime(),
                        candidate.getStartTime(),
                        candidate.getEndTime()));

        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Space already has a booking in that time window");
        }
    }

    private boolean windowsOverlap(LocalDateTime firstStart, LocalDateTime firstEnd, LocalDateTime secondStart, LocalDateTime secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }

    private String statusFor(Parking parking) {
        LocalDateTime now = LocalDateTime.now();
        if (!parking.getStartTime().isAfter(now) && parking.getEndTime().isAfter(now)) {
            return ACTIVE;
        }
        if (parking.getStartTime().isAfter(now)) {
            return RESERVED;
        }
        return EXPIRED;
    }

    private void syncSpotterAfterStatusChange(Parking parking, boolean wasActive, String previousSpace) {
        boolean isActive = ACTIVE.equals(parking.getStatus());
        boolean changedSpace = previousSpace != null && !previousSpace.equalsIgnoreCase(parking.getParkingSpace());
        if (wasActive && changedSpace) {
            releaseSpace(previousSpace, parking.getId());
        }
        if (isActive && !wasActive) {
            spotterClient.updateOccupancy(parking.getParkingSpace(), true, "booking-active");
        }
        if (isActive && wasActive && changedSpace) {
            spotterClient.updateOccupancy(parking.getParkingSpace(), true, "booking-active");
        }
        if (!isActive && wasActive) {
            releaseSpace(parking);
        }
    }

    private void releaseSpace(Parking parking) {
        releaseSpace(parking.getParkingSpace(), parking.getId());
    }

    private void releaseSpace(String parkingSpace, Long ignoredBookingId) {
        boolean anotherActiveBooking = parkingRepository.findByParkingSpaceIgnoreCaseAndStatusIn(
                parkingSpace,
                Set.of(ACTIVE)).stream()
                .anyMatch(candidate -> !candidate.getId().equals(ignoredBookingId));

        if (!anotherActiveBooking) {
            spotterClient.updateOccupancy(parkingSpace, false, "booking-ended");
        }
    }
}
