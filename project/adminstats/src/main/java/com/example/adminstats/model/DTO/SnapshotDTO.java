package com.example.adminstats.model.DTO;

import java.time.LocalDate;

public class SnapshotDTO {

    private int lotId;
    private LocalDate date;
    private int[] occupancy = new int[24];
    private int spotsTotal;

    public SnapshotDTO() {}

    public SnapshotDTO(int lotId, LocalDate date, int[] occupancy, int spotsTotal) {
        this.lotId = lotId;
        this.date = date;
        this.occupancy = occupancy;
        this.spotsTotal = spotsTotal;
    }

    public void setLotId(int _lotId){this.lotId=_lotId;}
    public void setDate(LocalDate _date){this.date=_date;}
    public void setOccupancy(int[] _occupancy){this.occupancy=_occupancy;}
    public void setSpotsTotal(int _spotsTotal){this.spotsTotal = _spotsTotal;}

    public int getLotId(){return this.lotId;}
    public LocalDate getDate(){return this.date;}
    public int[] getOccupancy(){return this.occupancy;}
    public int getSpotsTotal(){return this.spotsTotal;}
}
