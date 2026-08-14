package com.travelmemory.memory.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.memory.service.MemoryGalleryService;
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

@WebMvcTest(MemoryGalleryController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class MemoryGalleryControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MemoryGalleryService memoryGalleryService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void anonymousUserCannotReadPrivateMemories() throws Exception {
        mockMvc.perform(get("/api/v1/profile/memories"))
                .andExpect(status().isUnauthorized());

        verify(memoryGalleryService, never()).getCurrentUsersMemories();
    }

    @Test
    void authenticatedUserCanReadTheirMemoryGallery() throws Exception {
        when(memoryGalleryService.getCurrentUsersMemories()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/profile/memories").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
