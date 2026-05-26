package com.example.parking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.parking.dto.request.SpotterDetectionRequest;
import com.example.parking.dto.response.SpotterSpaceResponse;

@Service
public class SpotterClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String spotterServiceUrl;

    public SpotterClient(@Value("${spotter.service-url:http://localhost:8085/api/spotter}") String spotterServiceUrl) {
        this.spotterServiceUrl = spotterServiceUrl;
    }

    public SpotterSpaceResponse getSpace(String sensorId) {
        try {
            return restTemplate.getForObject(
                    spotterServiceUrl + "/sensors/" + sensorId,
                    SpotterSpaceResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalArgumentException("Parking space is not available in Spotter");
        }
    }

    public void updateOccupancy(String sensorId, boolean occupied, String source) {
        try {
            restTemplate.postForObject(
                    spotterServiceUrl + "/sensors/" + sensorId + "/detect",
                    new SpotterDetectionRequest(occupied, 0.99, source),
                    Object.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Could not update Spotter occupancy");
        }
    }
}
