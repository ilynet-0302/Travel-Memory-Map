package com.travelmemory.memory.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.memory.dto.OnThisDayMemoryResponse;
import com.travelmemory.memory.dto.OnThisDayResponse;
import com.travelmemory.memory.service.OnThisDayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OnThisDayController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class OnThisDayControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private OnThisDayService onThisDayService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void anonymousUserCannotReadMemories() throws Exception {
        mockMvc.perform(get("/api/v1/profile/on-this-day"))
                .andExpect(status().isUnauthorized());

        verify(onThisDayService, never()).getCurrentUsersMemories();
    }

    @Test
    void authenticatedUserCanReadMemories() throws Exception {
        UUID tripId = UUID.randomUUID();
        when(onThisDayService.getCurrentUsersMemories()).thenReturn(new OnThisDayResponse(
                LocalDate.of(2026, 8, 13),
                List.of(new OnThisDayMemoryResponse(
                        tripId, "Budapest After Dark", "Hungary", "HU", "Budapest",
                        LocalDate.of(2023, 8, 13), 3, 6, 18,
                        List.of(), null, null, List.of("Rudas Baths")))));

        mockMvc.perform(get("/api/v1/profile/on-this-day").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-13"))
                .andExpect(jsonPath("$.memories[0].tripId").value(tripId.toString()))
                .andExpect(jsonPath("$.memories[0].city").value("Budapest"))
                .andExpect(jsonPath("$.memories[0].yearsAgo").value(3))
                .andExpect(jsonPath("$.memories[0].placeCount").value(6))
                .andExpect(jsonPath("$.memories[0].photoCount").value(18));
    }
}
