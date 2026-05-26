package com.example.adminstats.service;

import com.example.adminstats.repository.*;

import java.time.LocalDate;
import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.example.adminstats.model.*;
import com.example.adminstats.model.DTO.OccupancySnapshotDTO;
import com.example.adminstats.model.DTO.PeakHourSnapshotDTO;
import com.example.adminstats.model.DTO.SnapshotDTO;
import com.example.adminstats.model.DTO.UtilisationSnapshotDTO;

@Service
public class AnalyticsService {
    private OccupancyService occupancyService;
    private PeakHourService peakHourService;
    private UtilisationService utilisationService;
    private AdminRepository adminRepo;

    public AnalyticsService(OccupancyService occupancyService, PeakHourService peakHourService, UtilisationService utilisationService, AdminRepository adminRepo){
        this.occupancyService = occupancyService;
        this.peakHourService = peakHourService;
        this.utilisationService = utilisationService;
        this.adminRepo = adminRepo;
    }

    public int[] getOccupancyAsArray(String occupancyPerHour) {
        return Arrays.stream(occupancyPerHour.split(","))
                    .mapToInt(Integer::parseInt)
                    .toArray();
    }

    public SnapshotDTO snapshotAssembler(Snapshot snapshot){
        SnapshotDTO snapshotDTO = new SnapshotDTO();
        snapshotDTO.setLotId(snapshot.getLotId());
        snapshotDTO.setSpotsTotal(snapshot.getSpotsTotal());
        snapshotDTO.setDate(snapshot.getDate());
        snapshotDTO.setOccupancy(getOccupancyAsArray(snapshot.getOccupancy()));
        return snapshotDTO;
    }

    public Summary getSummary(LocalDate date){

        Snapshot snapshot = adminRepo.findSnapshotByDate(date);

        if (snapshot == null) {
            throw new RuntimeException("No record found for date: " + date);
        }

        SnapshotDTO snapshotDTO = snapshotAssembler(snapshot);

        OccupancySnapshotDTO os = occupancyService.OccupancySnapshotAssembler(snapshotDTO);
        PeakHourSnapshotDTO phs = peakHourService.PeakHourSnapshotAssembler(snapshotDTO);
        UtilisationSnapshotDTO us = utilisationService.UtilisationSnapshotAssembler(snapshotDTO);

        Summary summary = new Summary(snapshot.getDate(), os, phs, us);

        return summary;
    }
    public Summary getLatest(){

        Snapshot snapshot = adminRepo.findTopByOrderByDateDesc();
        SnapshotDTO snapshotDTO = snapshotAssembler(snapshot);

        System.out.println(Arrays.toString(snapshotDTO.getOccupancy()));

        OccupancySnapshotDTO os = occupancyService.OccupancySnapshotAssembler(snapshotDTO);
        PeakHourSnapshotDTO phs = peakHourService.PeakHourSnapshotAssembler(snapshotDTO);
        UtilisationSnapshotDTO us = utilisationService.UtilisationSnapshotAssembler(snapshotDTO);

        Summary summary = new Summary(snapshot.getDate(), os, phs, us);

        return summary;
    }
}
