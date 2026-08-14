package com.travelmemory.memory.controller;

import com.travelmemory.memory.dto.MemoryPhotoResponse;
import com.travelmemory.memory.service.MemoryGalleryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile/memories")
public class MemoryGalleryController {

    private final MemoryGalleryService memoryGalleryService;

    public MemoryGalleryController(MemoryGalleryService memoryGalleryService) {
        this.memoryGalleryService = memoryGalleryService;
    }

    @GetMapping
    public List<MemoryPhotoResponse> getCurrentUsersMemories() {
        return memoryGalleryService.getCurrentUsersMemories();
    }
}
