package com.travelmemory.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-14T08:00:00Z"), ZoneOffset.UTC);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsPublicRequestsAfterConfiguredLimit() throws Exception {
        RateLimitInterceptor interceptor = interceptor(2);

        assertThat(invoke(interceptor, "GET", "/api/v1/public/trips/rome", "203.0.113.8").getStatus())
                .isEqualTo(200);
        assertThat(invoke(interceptor, "GET", "/api/v1/public/trips/rome", "203.0.113.8").getStatus())
                .isEqualTo(200);
        MockHttpServletResponse rejected = invoke(
                interceptor, "GET", "/api/v1/public/trips/rome", "203.0.113.8");

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        assertThat(rejected.getContentAsString()).contains("RATE_LIMITED");
    }

    @Test
    void authenticatedLimitsAreIsolatedPerUser() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(true, 10, 10, 1, 10, 10, CLOCK);
        authenticate("first-user");
        assertThat(invoke(interceptor, "POST", "/api/v1/trips/1/photos", "127.0.0.1").getStatus())
                .isEqualTo(200);
        assertThat(invoke(interceptor, "POST", "/api/v1/trips/1/photos", "127.0.0.1").getStatus())
                .isEqualTo(429);

        authenticate("second-user");
        assertThat(invoke(interceptor, "POST", "/api/v1/trips/1/photos", "127.0.0.1").getStatus())
                .isEqualTo(200);
    }

    @Test
    void unrelatedEndpointsAreNotLimited() throws Exception {
        RateLimitInterceptor interceptor = interceptor(1);

        assertThat(invoke(interceptor, "GET", "/api/v1/profile", "203.0.113.8").getStatus()).isEqualTo(200);
        assertThat(invoke(interceptor, "GET", "/api/v1/profile", "203.0.113.8").getStatus()).isEqualTo(200);
    }

    private RateLimitInterceptor interceptor(int publicLimit) {
        return new RateLimitInterceptor(true, publicLimit, 10, 10, 10, 10, CLOCK);
    }

    private MockHttpServletResponse invoke(
            RateLimitInterceptor interceptor, String method, String path, String remoteAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        if (interceptor.preHandle(request, response, new Object()) && response.getStatus() == 200) {
            response.setStatus(200);
        }
        return response;
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(username, "n/a", java.util.List.of()));
    }
}
