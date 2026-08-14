package com.travelmemory.location.controller;

import com.travelmemory.location.dto.GoogleMapsImportRequest;
import com.travelmemory.location.dto.GoogleMapsPlaceResponse;
import com.travelmemory.location.service.GoogleMapsImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationImportController {

    private final GoogleMapsImportService googleMapsImportService;

    public LocationImportController(GoogleMapsImportService googleMapsImportService) {
        this.googleMapsImportService = googleMapsImportService;
    }

    @PostMapping("/google-maps/import")
    public GoogleMapsPlaceResponse importGoogleMapsPlace(
            @Valid @RequestBody GoogleMapsImportRequest request) {
        return googleMapsImportService.importPlace(request.url());
    }
}
