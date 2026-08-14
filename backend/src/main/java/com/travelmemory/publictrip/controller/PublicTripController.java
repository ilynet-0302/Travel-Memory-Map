package com.travelmemory.publictrip.controller;

import com.travelmemory.publictrip.dto.PublicTripResponse;
import com.travelmemory.publictrip.service.PublicTripService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/trips")
public class PublicTripController {

    private final PublicTripService publicTripService;

    public PublicTripController(PublicTripService publicTripService) {
        this.publicTripService = publicTripService;
    }

    @GetMapping("/{publicSlug}")
    public PublicTripResponse getBySlug(@PathVariable String publicSlug) {
        return publicTripService.getBySlug(publicSlug);
    }
}
