package com.travelmemory.replay.controller;

import com.travelmemory.replay.dto.TripReplayResponse;
import com.travelmemory.replay.service.TripReplayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/replay")
public class TripReplayController {

    private final TripReplayService tripReplayService;

    public TripReplayController(TripReplayService tripReplayService) {
        this.tripReplayService = tripReplayService;
    }

    @GetMapping
    public TripReplayResponse replay(@PathVariable UUID tripId) {
        return tripReplayService.buildReplay(tripId);
    }
}

