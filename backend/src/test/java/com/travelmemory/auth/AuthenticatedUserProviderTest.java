package com.travelmemory.auth;

import com.travelmemory.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserProviderTest {

    private final AuthenticatedUserProvider provider = new AuthenticatedUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsSupabaseJwtClaimsToAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("email", "traveller@example.com")
                .claim("user_metadata", Map.of("full_name", "Test Traveller"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(authenticated(jwt));

        AuthenticatedUser user = provider.getCurrentUser();

        assertThat(user.id()).isEqualTo(userId);
        assertThat(user.email()).isEqualTo("traveller@example.com");
        assertThat(user.displayName()).isEqualTo("Test Traveller");
    }

    @Test
    void rejectsJwtWithNonUuidSubject() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("not-a-uuid")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(authenticated(jwt));

        assertThatThrownBy(provider::getCurrentUser)
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessageContaining("subject");
    }

    private JwtAuthenticationToken authenticated(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, java.util.List.of(new SimpleGrantedAuthority("ROLE_AUTHENTICATED")));
    }
}
