package com.example.adminstats.model;

import java.time.LocalDate;

import com.example.adminstats.model.DTO.OccupancySnapshotDTO;
import com.example.adminstats.model.DTO.PeakHourSnapshotDTO;
import com.example.adminstats.model.DTO.UtilisationSnapshotDTO;

public class Summary {
    private LocalDate date;
    private OccupancySnapshotDTO occupancy_snap;
    private PeakHourSnapshotDTO peakHour_snap;
    private UtilisationSnapshotDTO utilisation_snap;

    public Summary(LocalDate date, OccupancySnapshotDTO _occupancy_snap, PeakHourSnapshotDTO _peakHour_snap, UtilisationSnapshotDTO _utilisation_snap){
        this.date = date;
        this.occupancy_snap = _occupancy_snap;
        this.peakHour_snap = _peakHour_snap;
        this.utilisation_snap = _utilisation_snap;
    }

    public void setDate(LocalDate date){this.date = date;}
    public void setOccupancySnapshot(OccupancySnapshotDTO _occupancy_snap){this.occupancy_snap = _occupancy_snap;}
    public void setPeakHourSnapshot(PeakHourSnapshotDTO _peakHour_snap){this.peakHour_snap = _peakHour_snap;}
    public void setUtilisationSnapshot(UtilisationSnapshotDTO _utilisation_snap){this.utilisation_snap = _utilisation_snap;}

    public LocalDate getDate(){return this.date;}
    public OccupancySnapshotDTO getOccupancySnapshot(){return this.occupancy_snap;}
    public PeakHourSnapshotDTO getPeakHourSnapshot(){return this.peakHour_snap;}
    public UtilisationSnapshotDTO getUtilisationSnapshot(){return this.utilisation_snap;}

    @Override
    public String toString(){
        return "Parking Lot Summary for " + date +":\n" +
        occupancy_snap + "\n" +
        peakHour_snap + "\n" +
        utilisation_snap + "\n";
    }
}
