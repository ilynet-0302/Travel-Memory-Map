package com.travelmemory.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long MINUTE_MILLIS = 60_000L;
    private static final long HOUR_MILLIS = 3_600_000L;
    private static final int CLEANUP_INTERVAL = 256;
    private static final int MAX_BUCKETS = 20_000;
    private static final Pattern INVITE_PREVIEW = Pattern.compile("^/api/v1/invites/[^/]+$");
    private static final Pattern PHOTO_UPLOAD = Pattern.compile("^/api/v1/trips/[^/]+/photos$");
    private static final Pattern INVITE_CREATE = Pattern.compile("^/api/v1/trips/[^/]+/invites$");
    private static final Pattern TRIP_REPLAY = Pattern.compile("^/api/v1/trips/[^/]+/replay$");

    private final boolean enabled;
    private final List<Policy> policies;
    private final Clock clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    @Autowired
    public RateLimitInterceptor(
            @Value("${app.rate-limit.enabled}") boolean enabled,
            @Value("${app.rate-limit.public-requests-per-minute}") int publicRequestsPerMinute,
            @Value("${app.rate-limit.invite-previews-per-minute}") int invitePreviewsPerMinute,
            @Value("${app.rate-limit.uploads-per-hour}") int uploadsPerHour,
            @Value("${app.rate-limit.invites-per-hour}") int invitesPerHour,
            @Value("${app.rate-limit.replays-per-minute}") int replaysPerMinute) {
        this(enabled, publicRequestsPerMinute, invitePreviewsPerMinute, uploadsPerHour,
                invitesPerHour, replaysPerMinute, Clock.systemUTC());
    }

    RateLimitInterceptor(
            boolean enabled,
            int publicRequestsPerMinute,
            int invitePreviewsPerMinute,
            int uploadsPerHour,
            int invitesPerHour,
            int replaysPerMinute,
            Clock clock) {
        this.enabled = enabled;
        this.clock = clock;
        this.policies = List.of(
                new Policy("public", "GET", Pattern.compile("^/api/v1/public/.*$"),
                        positive(publicRequestsPerMinute), MINUTE_MILLIS, false),
                new Policy("invite-preview", "GET", INVITE_PREVIEW,
                        positive(invitePreviewsPerMinute), MINUTE_MILLIS, false),
                new Policy("photo-upload", "POST", PHOTO_UPLOAD,
                        positive(uploadsPerHour), HOUR_MILLIS, true),
                new Policy("invite-create", "POST", INVITE_CREATE,
                        positive(invitesPerHour), HOUR_MILLIS, true),
                new Policy("replay", "GET", TRIP_REPLAY,
                        positive(replaysPerMinute), MINUTE_MILLIS, true));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!enabled) return true;
        Policy policy = policyFor(request.getMethod(), request.getRequestURI());
        if (policy == null) return true;

        long now = clock.millis();
        cleanup(now);
        String client = policy.authenticatedClient() ? authenticatedClient(request) : remoteClient(request);
        Decision decision = consume(policy, client, now);
        if (decision.allowed()) return true;

        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"code":"RATE_LIMITED","message":"Too many requests. Please try again later.","fieldErrors":{}}
                """);
        return false;
    }

    private Policy policyFor(String method, String path) {
        return policies.stream()
                .filter(policy -> policy.method().equals(method) && policy.path().matcher(path).matches())
                .findFirst()
                .orElse(null);
    }

    private Decision consume(Policy policy, String client, long now) {
        String key = policy.name() + ':' + client;
        AtomicBoolean allowed = new AtomicBoolean();
        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAtMillis() <= now) {
                allowed.set(true);
                return new Window(1, now + policy.windowMillis());
            }
            if (existing.count() >= policy.limit()) {
                allowed.set(false);
                return existing;
            }
            allowed.set(true);
            return new Window(existing.count() + 1, existing.expiresAtMillis());
        });
        long retryAfterSeconds = Math.max(1, (window.expiresAtMillis() - now + 999) / 1000);
        return new Decision(allowed.get(), retryAfterSeconds);
    }

    private String authenticatedClient(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }
        return remoteClient(request);
    }

    private String remoteClient(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        return "ip:" + (address == null || address.isBlank() ? "unknown" : address);
    }

    private void cleanup(long now) {
        long count = requestCount.incrementAndGet();
        if (count % CLEANUP_INTERVAL != 0 && windows.size() < MAX_BUCKETS) return;
        windows.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
        if (windows.size() > MAX_BUCKETS) windows.clear();
    }

    private static int positive(int value) {
        if (value < 1) throw new IllegalArgumentException("Rate limits must be positive.");
        return value;
    }

    private record Policy(
            String name, String method, Pattern path, int limit, long windowMillis, boolean authenticatedClient) {
    }

    private record Window(int count, long expiresAtMillis) {
    }

    private record Decision(boolean allowed, long retryAfterSeconds) {
    }
}
