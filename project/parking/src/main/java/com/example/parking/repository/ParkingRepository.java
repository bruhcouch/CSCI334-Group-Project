package com.example.parking.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.parking.model.Parking;

@Repository
public interface ParkingRepository extends JpaRepository<Parking, Long> {
    List<Parking> findByAccountIdOrderByStartTimeDesc(Long accountId);

    List<Parking> findByStatusIn(Collection<String> statuses);

    List<Parking> findByParkingSpaceIgnoreCaseAndStatusIn(String parkingSpace, Collection<String> statuses);

    List<Parking> findByStatusAndStartTimeLessThanEqual(String status, LocalDateTime startTime);

    List<Parking> findByStatusInAndEndTimeLessThanEqual(Collection<String> statuses, LocalDateTime endTime);
}
