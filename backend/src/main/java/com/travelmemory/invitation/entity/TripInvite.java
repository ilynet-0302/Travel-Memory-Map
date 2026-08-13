package com.travelmemory.invitation.entity;

import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.user.entity.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "trip_invites")
public class TripInvite {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserProfile createdBy;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripRole role;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "use_count", nullable = false)
    private int useCount;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TripInvite() {
    }

    public TripInvite(
            Trip trip,
            UserProfile createdBy,
            String tokenHash,
            TripRole role,
            OffsetDateTime expiresAt,
            int maxUses) {
        if (role == TripRole.OWNER) {
            throw new IllegalArgumentException("An invitation cannot grant the OWNER role.");
        }
        if (maxUses < 1) {
            throw new IllegalArgumentException("Invitation max uses must be positive.");
        }
        this.id = UUID.randomUUID();
        this.trip = trip;
        this.createdBy = createdBy;
        this.tokenHash = tokenHash;
        this.role = role;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.useCount = 0;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void incrementUsage() {
        if (useCount >= maxUses) {
            throw new IllegalStateException("Invitation usage limit has already been reached.");
        }
        useCount++;
    }

    public void revoke(OffsetDateTime revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean hasReachedUsageLimit() {
        return useCount >= maxUses;
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public UserProfile getCreatedBy() { return createdBy; }
    public String getTokenHash() { return tokenHash; }
    public TripRole getRole() { return role; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public int getMaxUses() { return maxUses; }
    public int getUseCount() { return useCount; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
