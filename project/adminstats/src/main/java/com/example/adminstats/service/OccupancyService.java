package com.example.adminstats.service;

import org.springframework.stereotype.Service;

import com.example.adminstats.model.DTO.*;

@Service
public class OccupancyService {
    public OccupancySnapshotDTO OccupancySnapshotAssembler(SnapshotDTO snapshot){
        int[] occupancy_history = snapshot.getOccupancy();
        int hour = 0;
        int occupancy = 0;

        for(int i = 0; i < 24; i++){
            if(occupancy_history[i] > 0){
                hour = i+1;
                occupancy = occupancy_history[i];
            }
        }

        OccupancySnapshotDTO os = new OccupancySnapshotDTO(
            snapshot.getLotId(), snapshot.getDate(), hour, occupancy, snapshot.getSpotsTotal()
        );

        return os;
    }
}
