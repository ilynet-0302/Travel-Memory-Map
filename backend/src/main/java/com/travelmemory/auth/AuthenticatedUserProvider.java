package com.travelmemory.auth;

import com.travelmemory.exception.UnauthenticatedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class AuthenticatedUserProvider {

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication) || !authentication.isAuthenticated()) {
            throw new UnauthenticatedException();
        }

        String subject = jwtAuthentication.getToken().getSubject();
        try {
            UUID userId = UUID.fromString(subject);
            String email = jwtAuthentication.getToken().getClaimAsString("email");
            Map<String, Object> metadata = jwtAuthentication.getToken().getClaimAsMap("user_metadata");
            String displayName = metadata == null ? null : asString(metadata.get("full_name"));
            return new AuthenticatedUser(userId, email, displayName);
        } catch (IllegalArgumentException exception) {
            throw new UnauthenticatedException("The access token subject is not a valid user identifier.");
        }
    }

    private String asString(Object value) {
        return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : null;
    }
}
