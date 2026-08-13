package com.travelmemory.worldmap.dto;

import java.util.List;

public record WorldMapResponse(
        int countriesVisited,
        int countriesPlanned,
        int countriesTotal,
        List<WorldMapCountryResponse> countries) {
}
