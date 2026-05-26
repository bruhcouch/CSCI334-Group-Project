package com.example.adminstats.service;

import org.springframework.stereotype.Service;

import com.example.adminstats.model.DTO.*;

@Service
public class PeakHourService {
    public PeakHourSnapshotDTO PeakHourSnapshotAssembler(SnapshotDTO snapshot){
        int hour = -1;
        double occupancyRate = -1;
        int[] occupancy_history = snapshot.getOccupancy();
        int maxOccupancy = -1;

        for(int i = 0; i < 24; i++){
            if(occupancy_history[i] > maxOccupancy){
                maxOccupancy = occupancy_history[i];
                hour = i+1;
            }
        }

        if(snapshot.getSpotsTotal() > 0){
            occupancyRate = ((double) maxOccupancy / snapshot.getSpotsTotal()) * 100;
        }

        PeakHourSnapshotDTO phs = new PeakHourSnapshotDTO(
            snapshot.getLotId(), snapshot.getDate(), hour, occupancyRate
        );

        return phs;
    }
}
