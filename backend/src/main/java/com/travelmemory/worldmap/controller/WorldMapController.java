package com.travelmemory.worldmap.controller;

import com.travelmemory.worldmap.dto.WorldMapResponse;
import com.travelmemory.worldmap.service.WorldMapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile/world-map")
public class WorldMapController {

    private final WorldMapService worldMapService;

    public WorldMapController(WorldMapService worldMapService) {
        this.worldMapService = worldMapService;
    }

    @GetMapping
    public WorldMapResponse getCurrentUsersMap() {
        return worldMapService.getCurrentUsersMap();
    }
}
