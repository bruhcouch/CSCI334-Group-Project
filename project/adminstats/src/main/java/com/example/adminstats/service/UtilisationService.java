package com.example.adminstats.service;

import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.example.adminstats.model.DTO.SnapshotDTO;
import com.example.adminstats.model.DTO.UtilisationSnapshotDTO;

@Service
public class UtilisationService {
    public UtilisationSnapshotDTO UtilisationSnapshotAssembler(SnapshotDTO snapshot){
        double utilisationRate = 0;
        int[] occupancy_history = snapshot.getOccupancy();
        int end = 24;

        for(int i = 0; i <= end-1; i++){
            if(occupancy_history[i] == -1){
                end = i-1;
                break;
            }
            utilisationRate += ((double) occupancy_history[i]/snapshot.getSpotsTotal())*100;
        }

        utilisationRate = utilisationRate / end;

        UtilisationSnapshotDTO us = new UtilisationSnapshotDTO(
            snapshot.getLotId(), snapshot.getDate(), utilisationRate
        );

        return us;
    }
}
