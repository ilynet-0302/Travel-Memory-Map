package com.travelmemory.photo.entity;

import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.user.entity.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "photos")
public class Photo {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_stop_id")
    private TripStop tripStop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private UserProfile uploadedBy;

    @Column(name = "storage_path", nullable = false, unique = true, length = 600)
    private String storagePath;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "taken_at")
    private OffsetDateTime takenAt;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(length = 1000)
    private String caption;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Photo() {
    }

    public Photo(
            UUID id,
            Trip trip,
            TripStop tripStop,
            UserProfile uploadedBy,
            String storagePath,
            String originalFileName,
            String contentType,
            long fileSize,
            OffsetDateTime takenAt,
            BigDecimal latitude,
            BigDecimal longitude,
            String caption) {
        this.id = id;
        this.trip = trip;
        this.tripStop = tripStop;
        this.uploadedBy = uploadedBy;
        this.storagePath = storagePath;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.takenAt = takenAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.caption = normalizeCaption(caption);
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String normalizeCaption(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public TripStop getTripStop() { return tripStop; }
    public UserProfile getUploadedBy() { return uploadedBy; }
    public String getStoragePath() { return storagePath; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public OffsetDateTime getTakenAt() { return takenAt; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getCaption() { return caption; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
