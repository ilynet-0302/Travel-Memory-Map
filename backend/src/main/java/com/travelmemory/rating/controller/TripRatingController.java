package com.travelmemory.rating.controller;

import com.travelmemory.rating.dto.TripRatingSummaryResponse;
import com.travelmemory.rating.dto.UpsertTripRatingRequest;
import com.travelmemory.rating.service.TripRatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/rating")
public class TripRatingController {

    private final TripRatingService tripRatingService;

    public TripRatingController(TripRatingService tripRatingService) {
        this.tripRatingService = tripRatingService;
    }

    @GetMapping
    public TripRatingSummaryResponse summary(@PathVariable UUID tripId) {
        return tripRatingService.summary(tripId);
    }

    @PutMapping
    public TripRatingSummaryResponse rate(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpsertTripRatingRequest request) {
        return tripRatingService.rate(tripId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID tripId) {
        tripRatingService.remove(tripId);
    }
}
