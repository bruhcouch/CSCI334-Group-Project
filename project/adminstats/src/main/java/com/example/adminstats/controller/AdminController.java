package com.example.adminstats.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.adminstats.service.AnalyticsService;
import com.example.adminstats.model.Summary;

@RestController
@RequestMapping("adminstats")
public class AdminController {
    
    private final AnalyticsService analyticsService;

    public AdminController(AnalyticsService analyticsService){
        this.analyticsService = analyticsService;
    }
    @GetMapping("/{date}")
    public ResponseEntity<Summary> getSummary(@PathVariable LocalDate date){
        try {
            Summary summary = analyticsService.getSummary(date);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/latest")
    public Summary getLatest(){
        return analyticsService.getLatest();
    }
    
}
