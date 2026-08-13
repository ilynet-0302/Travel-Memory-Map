package com.travelmemory.rating.entity;

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

    @Column(name = "overall_score", nullable = false)
    private short overallScore;

    @Column(nullable = false)
    private short food;

    @Column(nullable = false)
    private short nightlife;

    @Column(nullable = false)
    private short culture;

    @Column(nullable = false)
    private short nature;

    @Column(nullable = false)
    private short walkability;

    @Column(name = "value_for_money", nullable = false)
    private short valueForMoney;

    @Column(nullable = false)
    private short crowds;

    @Column(nullable = false)
    private short relaxation;

    @Enumerated(EnumType.STRING)
    @Column(name = "would_return", nullable = false, length = 10)
    private WouldReturn wouldReturn;

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
        updateDetails(score, score, score, score, score, score, score, score, WouldReturn.MAYBE);
        this.createdAt = this.updatedAt;
    }

    public TripRating(
            Trip trip,
            UserProfile user,
            int food,
            int nightlife,
            int culture,
            int nature,
            int walkability,
            int valueForMoney,
            int crowds,
            int relaxation,
            WouldReturn wouldReturn) {
        this.id = UUID.randomUUID();
        this.trip = trip;
        this.user = user;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        updateDetails(food, nightlife, culture, nature, walkability, valueForMoney, crowds, relaxation, wouldReturn);
        this.createdAt = this.updatedAt;
    }

    public void updateDetails(
            int food,
            int nightlife,
            int culture,
            int nature,
            int walkability,
            int valueForMoney,
            int crowds,
            int relaxation,
            WouldReturn wouldReturn) {
        this.food = checkedScore(food);
        this.nightlife = checkedScore(nightlife);
        this.culture = checkedScore(culture);
        this.nature = checkedScore(nature);
        this.walkability = checkedScore(walkability);
        this.valueForMoney = checkedScore(valueForMoney);
        this.crowds = checkedScore(crowds);
        this.relaxation = checkedScore(relaxation);
        this.wouldReturn = java.util.Objects.requireNonNull(wouldReturn, "Would return is required.");
        this.overallScore = (short) Math.round((food + nightlife + culture + nature
                + walkability + valueForMoney + crowds + relaxation) / 8.0);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private short checkedScore(int score) {
        if (score < 1 || score > 10) {
            throw new IllegalArgumentException("Every trip rating must be between 1 and 10.");
        }
        return (short) score;
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public UserProfile getUser() { return user; }
    public int getScore() { return overallScore; }
    public int getOverallScore() { return overallScore; }
    public int getFood() { return food; }
    public int getNightlife() { return nightlife; }
    public int getCulture() { return culture; }
    public int getNature() { return nature; }
    public int getWalkability() { return walkability; }
    public int getValueForMoney() { return valueForMoney; }
    public int getCrowds() { return crowds; }
    public int getRelaxation() { return relaxation; }
    public WouldReturn getWouldReturn() { return wouldReturn; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
