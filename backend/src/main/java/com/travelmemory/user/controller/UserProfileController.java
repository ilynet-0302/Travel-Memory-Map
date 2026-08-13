package com.travelmemory.user.controller;

import com.travelmemory.user.dto.UpdateUserProfileRequest;
import com.travelmemory.user.dto.UserProfileResponse;
import com.travelmemory.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserProfileResponse getCurrentProfile() {
        return userProfileService.getCurrentProfile();
    }

    @PutMapping
    public UserProfileResponse updateCurrentProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userProfileService.updateCurrentProfile(request);
    }
}
