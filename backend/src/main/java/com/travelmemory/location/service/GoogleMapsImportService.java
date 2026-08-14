package com.travelmemory.location.service;

import com.travelmemory.location.dto.GoogleMapsPlaceResponse;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class GoogleMapsImportService {

    private final GoogleMapsRedirectResolver redirectResolver;

    public GoogleMapsImportService(GoogleMapsRedirectResolver redirectResolver) {
        this.redirectResolver = redirectResolver;
    }

    public GoogleMapsPlaceResponse importPlace(String url) {
        URI parsed = GoogleMapsLinkSupport.parseSupportedUri(url);
        URI resolved = GoogleMapsLinkSupport.isShortened(parsed) ? redirectResolver.resolve(parsed) : parsed;
        GoogleMapsLinkSupport.requireSupported(resolved);
        GoogleMapsLinkSupport.Coordinates coordinates = GoogleMapsLinkSupport.coordinates(resolved);
        return new GoogleMapsPlaceResponse(
                GoogleMapsLinkSupport.placeName(resolved),
                coordinates.latitude(),
                coordinates.longitude(),
                resolved.toASCIIString());
    }
}
