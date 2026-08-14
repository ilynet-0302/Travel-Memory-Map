package com.travelmemory.memory.service;

import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.memory.dto.MemoryPhotoResponse;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.photo.service.PhotoStorageService;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class MemoryGalleryService {

    private final TripRepository tripRepository;
    private final PhotoRepository photoRepository;
    private final PhotoStorageService photoStorageService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public MemoryGalleryService(
            TripRepository tripRepository,
            PhotoRepository photoRepository,
            PhotoStorageService photoStorageService,
            AuthenticatedUserProvider authenticatedUserProvider) {
        this.tripRepository = tripRepository;
        this.photoRepository = photoRepository;
        this.photoStorageService = photoStorageService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional(readOnly = true)
    public List<MemoryPhotoResponse> getCurrentUsersMemories() {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        String accessToken = authenticatedUserProvider.getCurrentAccessToken();
        List<UUID> accessibleTripIds = tripRepository.findAccessibleTrips(userId).stream()
                .map(Trip::getId)
                .toList();
        if (accessibleTripIds.isEmpty()) return List.of();

        return photoRepository.findByTripIdIn(accessibleTripIds).stream()
                .sorted(Comparator.comparing(this::memoryDate).reversed().thenComparing(Photo::getId))
                .map(photo -> response(photo, accessToken))
                .toList();
    }

    private OffsetDateTime memoryDate(Photo photo) {
        return photo.getTakenAt() == null ? photo.getCreatedAt() : photo.getTakenAt();
    }

    private MemoryPhotoResponse response(Photo photo, String accessToken) {
        Trip trip = photo.getTrip();
        return new MemoryPhotoResponse(
                photo.getId(),
                trip.getId(),
                trip.getTitle(),
                trip.getCountry(),
                trip.getCountryCode(),
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                photo.getTripStop() == null ? null : photo.getTripStop().getId(),
                photo.getTripStop() == null ? null : photo.getTripStop().getName(),
                photo.getUploadedBy().getId(),
                photo.getUploadedBy().getDisplayName(),
                photoStorageService.createSignedUrl(photo.getStoragePath(), accessToken),
                photo.getOriginalFileName(),
                photo.getContentType(),
                photo.getFileSize(),
                photo.getTakenAt(),
                photo.getLatitude(),
                photo.getLongitude(),
                photo.getCaption(),
                photo.isPublicVisible(),
                photo.getCreatedAt());
    }
}
