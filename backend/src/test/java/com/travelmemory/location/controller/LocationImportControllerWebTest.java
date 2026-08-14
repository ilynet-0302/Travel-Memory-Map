package com.travelmemory.location.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.location.dto.GoogleMapsPlaceResponse;
import com.travelmemory.location.service.GoogleMapsImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationImportController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class LocationImportControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleMapsImportService googleMapsImportService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousImportIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/locations/google-maps/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://www.google.com/maps/place/Colosseum/@41.8902,12.4922,17z"}
                                """))
                .andExpect(status().isUnauthorized());

        verify(googleMapsImportService, never()).importPlace(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void authenticatedImportReturnsParsedPlace() throws Exception {
        String url = "https://www.google.com/maps/place/Colosseum/@41.8902,12.4922,17z";
        when(googleMapsImportService.importPlace(url)).thenReturn(new GoogleMapsPlaceResponse(
                "Colosseum", new BigDecimal("41.8902"), new BigDecimal("12.4922"), url));

        mockMvc.perform(post("/api/v1/locations/google-maps/import")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://www.google.com/maps/place/Colosseum/@41.8902,12.4922,17z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Colosseum"))
                .andExpect(jsonPath("$.latitude").value(41.8902))
                .andExpect(jsonPath("$.longitude").value(12.4922));
    }
}
