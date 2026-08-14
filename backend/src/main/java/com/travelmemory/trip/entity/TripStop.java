package com.travelmemory.trip.entity;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "trip_stops")
public class TripStop {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserProfile createdBy;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "arrival_time", nullable = false)
    private OffsetDateTime arrivalTime;

    @Column(name = "arrival_local_datetime")
    private LocalDateTime arrivalLocalDateTime;

    @Column(name = "departure_time")
    private OffsetDateTime departureTime;

    @Column(name = "departure_local_datetime")
    private LocalDateTime departureLocalDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StopCategory category;

    @Column
    private Integer rating;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TripStop() {
    }

    public TripStop(
            Trip trip,
            UserProfile createdBy,
            String name,
            String description,
            BigDecimal latitude,
            BigDecimal longitude,
            OffsetDateTime arrivalTime,
            OffsetDateTime departureTime,
            StopCategory category,
            Integer rating,
            int position) {
        this.id = UUID.randomUUID();
        this.trip = trip;
        this.createdBy = createdBy;
        updateDetails(name, description, latitude, longitude, arrivalTime, departureTime, category, rating, position);
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void updateDetails(
            String name,
            String description,
            BigDecimal latitude,
            BigDecimal longitude,
            OffsetDateTime arrivalTime,
            OffsetDateTime departureTime,
            StopCategory category,
            Integer rating,
            int position) {
        this.name = name.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.latitude = latitude;
        this.longitude = longitude;
        this.arrivalTime = arrivalTime;
        this.arrivalLocalDateTime = arrivalTime.toLocalDateTime();
        this.departureTime = departureTime;
        this.departureLocalDateTime = departureTime == null ? null : departureTime.toLocalDateTime();
        this.category = category;
        this.rating = rating;
        this.position = position;
    }

    public void moveToPosition(int position) {
        this.position = position;
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public UserProfile getCreatedBy() { return createdBy; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public OffsetDateTime getArrivalTime() { return arrivalTime; }
    public LocalDateTime getArrivalLocalDateTime() { return arrivalLocalDateTime; }
    public OffsetDateTime getDepartureTime() { return departureTime; }
    public LocalDateTime getDepartureLocalDateTime() { return departureLocalDateTime; }
    public StopCategory getCategory() { return category; }
    public Integer getRating() { return rating; }
    public int getPosition() { return position; }
}
