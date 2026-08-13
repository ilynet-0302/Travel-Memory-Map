package com.travelmemory.rating.entity;

import com.travelmemory.trip.entity.Trip;
import com.travelmemory.user.entity.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "trip_ratings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trip_ratings_trip_user",
                columnNames = {"trip_id", "user_id"}))
public class TripRating {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(nullable = false)
    private short score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected TripRating() {
    }

    public TripRating(Trip trip, UserProfile user, int score) {
        this.id = UUID.randomUUID();
        this.trip = trip;
        this.user = user;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        updateScore(score);
        this.createdAt = this.updatedAt;
    }

    public void updateScore(int score) {
        if (score < 1 || score > 10) {
            throw new IllegalArgumentException("A trip rating must be between 1 and 10.");
        }
        this.score = (short) score;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public UserProfile getUser() { return user; }
    public int getScore() { return score; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
