package com.travelmemory.user.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.dna.dto.TravelDnaScoresResponse;
import com.travelmemory.dna.dto.TravelPersonalityResponse;
import com.travelmemory.dna.service.TravelDnaService;
import com.travelmemory.statistics.dto.TravelStatisticsResponse;
import com.travelmemory.statistics.service.TravelStatisticsService;
import com.travelmemory.user.dto.UpdateUserProfileRequest;
import com.travelmemory.user.dto.UserProfileResponse;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Mock
    private TravelStatisticsService travelStatisticsService;

    @Mock
    private TravelDnaService travelDnaService;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void userEditedDisplayNameIsNotOverwrittenByOlderTokenMetadata() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "ilia@example.com", "Old token name");
        UserProfile existingProfile = new UserProfile(userId, "ilia@example.com", "Initial name");
        TravelStatisticsResponse statistics = new TravelStatisticsResponse(
                1, 2, 3, 2, 8, 14, 12, List.of(), null, null, null, null, null);
        TravelPersonalityResponse personality = new TravelPersonalityResponse(
                "Still taking shape", "Complete more journeys.", false, 2, 3,
                new TravelDnaScoresResponse(60, 40, 30, 20, 25, 35, 45));

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(authenticatedUser);
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(existingProfile));
        when(travelStatisticsService.calculateFor(userId)).thenReturn(statistics);
        when(travelDnaService.personalityFor(userId)).thenReturn(personality);

        UserProfileResponse response = userProfileService.updateCurrentProfile(
                new UpdateUserProfileRequest("Iliya Petrov"));

        assertThat(response.displayName()).isEqualTo("Iliya Petrov");
        assertThat(existingProfile.getDisplayName()).isEqualTo("Iliya Petrov");
        assertThat(response.statistics()).isEqualTo(statistics);
        assertThat(response.personality()).isEqualTo(personality);
    }
}
