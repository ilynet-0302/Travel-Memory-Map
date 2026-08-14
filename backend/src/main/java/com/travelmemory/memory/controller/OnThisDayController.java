package com.travelmemory.memory.controller;

import com.travelmemory.memory.dto.OnThisDayResponse;
import com.travelmemory.memory.service.OnThisDayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile/on-this-day")
public class OnThisDayController {

    private final OnThisDayService onThisDayService;

    public OnThisDayController(OnThisDayService onThisDayService) {
        this.onThisDayService = onThisDayService;
    }

    @GetMapping
    public OnThisDayResponse getCurrentUsersMemories() {
        return onThisDayService.getCurrentUsersMemories();
    }
}
