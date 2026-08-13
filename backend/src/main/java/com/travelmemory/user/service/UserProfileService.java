package com.travelmemory.user.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.dna.service.TravelDnaService;
import com.travelmemory.statistics.service.TravelStatisticsService;
import com.travelmemory.user.dto.UpdateUserProfileRequest;
import com.travelmemory.user.dto.UserProfileResponse;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final TravelStatisticsService travelStatisticsService;
    private final TravelDnaService travelDnaService;

    public UserProfileService(
            UserProfileRepository userProfileRepository,
            AuthenticatedUserProvider authenticatedUserProvider,
            TravelStatisticsService travelStatisticsService,
            TravelDnaService travelDnaService) {
        this.userProfileRepository = userProfileRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.travelStatisticsService = travelStatisticsService;
        this.travelDnaService = travelDnaService;
    }

    @Transactional
    public UserProfile synchronizeProfile(AuthenticatedUser authenticatedUser) {
        return userProfileRepository.findById(authenticatedUser.id())
                .map(profile -> {
                    profile.synchronizeIdentity(authenticatedUser.email(), null);
                    return profile;
                })
                .orElseGet(() -> userProfileRepository.save(new UserProfile(
                        authenticatedUser.id(),
                        authenticatedUser.email() == null ? authenticatedUser.id() + "@unknown.local" : authenticatedUser.email(),
                        displayName(authenticatedUser))));
    }

    @Transactional
    public UserProfileResponse getCurrentProfile() {
        AuthenticatedUser authenticatedUser = authenticatedUserProvider.getCurrentUser();
        return toResponse(synchronizeProfile(authenticatedUser));
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(UpdateUserProfileRequest request) {
        AuthenticatedUser authenticatedUser = authenticatedUserProvider.getCurrentUser();
        UserProfile profile = synchronizeProfile(authenticatedUser);
        profile.updateDisplayName(request.displayName());
        return toResponse(profile);
    }

    private String displayName(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser.displayName() != null) {
            return authenticatedUser.displayName();
        }
        if (authenticatedUser.email() != null && authenticatedUser.email().contains("@")) {
            return authenticatedUser.email().substring(0, authenticatedUser.email().indexOf('@'));
        }
        return "Traveller";
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                travelStatisticsService.calculateFor(profile.getId()),
                travelDnaService.personalityFor(profile.getId()));
    }
}
