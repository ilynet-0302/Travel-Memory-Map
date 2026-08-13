package com.travelmemory.user.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile synchronizeProfile(AuthenticatedUser authenticatedUser) {
        return userProfileRepository.findById(authenticatedUser.id())
                .map(profile -> {
                    profile.synchronizeIdentity(authenticatedUser.email(), displayName(authenticatedUser));
                    return profile;
                })
                .orElseGet(() -> userProfileRepository.save(new UserProfile(
                        authenticatedUser.id(),
                        authenticatedUser.email() == null ? authenticatedUser.id() + "@unknown.local" : authenticatedUser.email(),
                        displayName(authenticatedUser))));
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
}
