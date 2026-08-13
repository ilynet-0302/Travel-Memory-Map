package com.travelmemory.photo.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.photo.dto.PhotoResponse;
import com.travelmemory.photo.service.PhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PhotoController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class PhotoControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhotoService photoService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousRequestCannotListPrivateTripPhotos() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/trips/{tripId}/photos", tripId))
                .andExpect(status().isUnauthorized());

        verify(photoService, never()).list(tripId);
    }

    @Test
    void authenticatedUserCanListPhotos() throws Exception {
        UUID tripId = UUID.randomUUID();
        when(photoService.list(tripId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/trips/{tripId}/photos", tripId).with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(photoService).list(tripId);
    }

    @Test
    void authenticatedUploadMapsMultipartPhoto() throws Exception {
        UUID tripId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 8, 13, 9, 30, 0, 0, ZoneOffset.UTC);
        MockMultipartFile file = new MockMultipartFile(
                "file", "rome.jpg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
        when(photoService.upload(eq(tripId), isNull(), eq("Golden hour"), any()))
                .thenReturn(new PhotoResponse(
                        photoId,
                        tripId,
                        null,
                        userId,
                        "Trip Owner",
                        tripId + "/" + photoId + ".jpg",
                        "https://signed.example/photo",
                        "rome.jpg",
                        "image/jpeg",
                        3,
                        null,
                        null,
                        null,
                        "Golden hour",
                        createdAt));

        mockMvc.perform(multipart("/api/v1/trips/{tripId}/photos", tripId)
                        .file(file)
                        .param("caption", "Golden hour")
                        .with(authenticatedJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(photoId.toString()))
                .andExpect(jsonPath("$.caption").value("Golden hour"));

        verify(photoService).upload(eq(tripId), isNull(), eq("Golden hour"), any());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedJwt() {
        return jwt().jwt(token -> token
                .subject(UUID.randomUUID().toString())
                .claim("email", "owner@example.com")
                .claim("user_metadata", Map.of("full_name", "Trip Owner")));
    }
}
