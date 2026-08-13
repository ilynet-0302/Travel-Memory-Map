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
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserProfile owner;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripVisibility visibility;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "public_slug", unique = true, length = 140)
    private String publicSlug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Trip() {
    }

    public Trip(
            UserProfile owner,
            String title,
            String description,
            String country,
            String countryCode,
            String city,
            LocalDate startDate,
            LocalDate endDate,
            TripVisibility visibility) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        updateDetails(title, description, country, countryCode, city, startDate, endDate, visibility);
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = this.createdAt;
    }

    public void updateDetails(
            String title,
            String description,
            String country,
            String countryCode,
            String city,
            LocalDate startDate,
            LocalDate endDate,
            TripVisibility visibility) {
        this.title = title.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.country = country.trim();
        this.countryCode = countryCode.trim().toUpperCase();
        this.city = city.trim();
        this.startDate = startDate;
        this.endDate = endDate;
        this.visibility = visibility;
        this.status = calculateStatus(startDate, endDate);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void archive() {
        this.status = TripStatus.ARCHIVED;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private TripStatus calculateStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (startDate.isAfter(today)) {
            return TripStatus.UPCOMING;
        }
        if (endDate.isBefore(today)) {
            return TripStatus.COMPLETED;
        }
        return TripStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public UserProfile getOwner() { return owner; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCountry() { return country; }
    public String getCountryCode() { return countryCode; }
    public String getCity() { return city; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public TripStatus getStatus() { return status; }
    public TripVisibility getVisibility() { return visibility; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public String getPublicSlug() { return publicSlug; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
