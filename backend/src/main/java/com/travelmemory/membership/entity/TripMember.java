package com.travelmemory.membership.entity;

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
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(
        name = "trip_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_trip_members_trip_user", columnNames = {"trip_id", "user_id"}))
public class TripMember {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    protected TripMember() {
    }

    public TripMember(Trip trip, UserProfile user, TripRole role) {
        this.id = UUID.randomUUID();
        this.trip = trip;
        this.user = user;
        this.role = role;
        this.joinedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void changeRole(TripRole role) {
        if (this.role == TripRole.OWNER || role == TripRole.OWNER) {
            throw new IllegalStateException("The owner role cannot be changed through member management.");
        }
        this.role = role;
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public UserProfile getUser() { return user; }
    public TripRole getRole() { return role; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
}
