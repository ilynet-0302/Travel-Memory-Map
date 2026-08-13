package com.travelmemory.comparison.controller;

import com.travelmemory.comparison.dto.TripComparisonResponse;
import com.travelmemory.comparison.service.TripComparisonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/comparison")
public class TripComparisonController {

    private final TripComparisonService tripComparisonService;

    public TripComparisonController(TripComparisonService tripComparisonService) {
        this.tripComparisonService = tripComparisonService;
    }

    @GetMapping
    public TripComparisonResponse compare(
            @RequestParam UUID leftTripId,
            @RequestParam UUID rightTripId) {
        return tripComparisonService.compare(leftTripId, rightTripId);
    }
}
