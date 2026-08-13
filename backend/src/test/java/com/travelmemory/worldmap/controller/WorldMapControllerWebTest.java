package com.travelmemory.worldmap.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.worldmap.dto.WorldMapResponse;
import com.travelmemory.worldmap.service.WorldMapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorldMapController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class WorldMapControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private WorldMapService worldMapService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void anonymousUserCannotReadWorldMap() throws Exception {
        mockMvc.perform(get("/api/v1/profile/world-map"))
                .andExpect(status().isUnauthorized());

        verify(worldMapService, never()).getCurrentUsersMap();
    }

    @Test
    void authenticatedUserCanReadWorldMap() throws Exception {
        when(worldMapService.getCurrentUsersMap())
                .thenReturn(new WorldMapResponse(3, 1, 195, List.of()));

        mockMvc.perform(get("/api/v1/profile/world-map").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countriesVisited").value(3))
                .andExpect(jsonPath("$.countriesPlanned").value(1))
                .andExpect(jsonPath("$.countriesTotal").value(195));
    }
}
