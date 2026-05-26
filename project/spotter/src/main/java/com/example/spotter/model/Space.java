package com.example.spotter.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "spaces", uniqueConstraints = {
		@UniqueConstraint(name = "uk_space_sensor_id", columnNames = "sensor_id")
})
public class Space {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String lotName;

	@Column(nullable = false)
	private String zone;

	@Column(nullable = false)
	private String bayNumber;

	@Column(name = "sensor_id", nullable = false)
	private String sensorId;

	private int maxParkingMinutes;
	private boolean disabilityPermitRequired;
	private boolean occupied;
	private double confidence;
	private String statusSource;
	private Instant lastUpdated;
	private Double latitude;
	private Double longitude;

	public Space() {
	}

	public Space(
			String lotName,
			String zone,
			String bayNumber,
			String sensorId,
			int maxParkingMinutes,
			boolean disabilityPermitRequired,
			boolean occupied,
			double confidence,
			String statusSource,
			Instant lastUpdated,
			Double latitude,
			Double longitude) {
		this.lotName = lotName;
		this.zone = zone;
		this.bayNumber = bayNumber;
		this.sensorId = sensorId;
		this.maxParkingMinutes = maxParkingMinutes;
		this.disabilityPermitRequired = disabilityPermitRequired;
		this.occupied = occupied;
		this.confidence = confidence;
		this.statusSource = statusSource;
		this.lastUpdated = lastUpdated;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Long getId() {
		return id;
	}

	public String getLotName() {
		return lotName;
	}

	public void setLotName(String lotName) {
		this.lotName = lotName;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public String getBayNumber() {
		return bayNumber;
	}

	public void setBayNumber(String bayNumber) {
		this.bayNumber = bayNumber;
	}

	public String getSensorId() {
		return sensorId;
	}

	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}

	public int getMaxParkingMinutes() {
		return maxParkingMinutes;
	}

	public void setMaxParkingMinutes(int to) {
		maxParkingMinutes = to;
	}

	public boolean isDisabilityPermitRequired() {
		return disabilityPermitRequired;
	}

	public void setDisabilityPermitRequired(boolean disabilityPermitRequired) {
		this.disabilityPermitRequired = disabilityPermitRequired;
	}

	public boolean isOccupied() {
		return occupied;
	}

	public void setOccupied(boolean to) {
		occupied = to;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public String getStatusSource() {
		return statusSource;
	}

	public void setStatusSource(String statusSource) {
		this.statusSource = statusSource;
	}

	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getDisplayName() {
		if (disabilityPermitRequired) {
			return lotName + " Accessible " + bayNumber;
		}
		return lotName + " Bay " + bayNumber;
	}

	public void applyDetection(boolean occupied, double confidence, String source, Instant detectedAt) {
		this.occupied = occupied;
		this.confidence = confidence;
		this.statusSource = source;
		this.lastUpdated = detectedAt;
	}
}
