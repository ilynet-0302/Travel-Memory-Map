package com.travelmemory.dna.controller;

import com.travelmemory.dna.dto.TripDnaResponse;
import com.travelmemory.dna.service.TravelDnaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/dna")
public class TripDnaController {

    private final TravelDnaService travelDnaService;

    public TripDnaController(TravelDnaService travelDnaService) {
        this.travelDnaService = travelDnaService;
    }

    @GetMapping
    public TripDnaResponse get(@PathVariable UUID tripId) {
        return travelDnaService.forTrip(tripId);
    }
}
