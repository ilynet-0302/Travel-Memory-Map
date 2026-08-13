package com.travelmemory.photo.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.InvalidPhotoException;
import com.travelmemory.exception.PhotoNotFoundException;
import com.travelmemory.exception.TripAccessDeniedException;
import com.travelmemory.exception.TripStopNotFoundException;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.photo.dto.PhotoResponse;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PhotoService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final TripService tripService;
    private final TripStopRepository tripStopRepository;
    private final PhotoRepository photoRepository;
    private final TripPermissionService tripPermissionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final UserProfileService userProfileService;
    private final PhotoMetadataExtractor metadataExtractor;
    private final PhotoStorageService storageService;

    public PhotoService(
            TripService tripService,
            TripStopRepository tripStopRepository,
            PhotoRepository photoRepository,
            TripPermissionService tripPermissionService,
            AuthenticatedUserProvider authenticatedUserProvider,
            UserProfileService userProfileService,
            PhotoMetadataExtractor metadataExtractor,
            PhotoStorageService storageService) {
        this.tripService = tripService;
        this.tripStopRepository = tripStopRepository;
        this.photoRepository = photoRepository;
        this.tripPermissionService = tripPermissionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userProfileService = userProfileService;
        this.metadataExtractor = metadataExtractor;
        this.storageService = storageService;
    }

    @Transactional
    public PhotoResponse upload(UUID tripId, UUID tripStopId, String caption, MultipartFile file) {
        AuthenticatedUser identity = authenticatedUserProvider.getCurrentUser();
        String accessToken = authenticatedUserProvider.getCurrentAccessToken();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireEditorOrOwner(trip, identity.id());
        validate(file, caption);

        TripStop tripStop = tripStopId == null ? null : tripStopRepository.findByIdAndTripId(tripStopId, tripId)
                .orElseThrow(() -> new TripStopNotFoundException(tripStopId));
        UserProfile uploadedBy = userProfileService.synchronizeProfile(identity);
        UUID photoId = UUID.randomUUID();
        String extension = EXTENSIONS.get(file.getContentType());
        String storagePath = "%s/%s.%s".formatted(tripId, photoId, extension);
        ExtractedPhotoMetadata metadata = metadataExtractor.extract(file);

        storageService.upload(storagePath, file, accessToken);
        try {
            Photo photo = photoRepository.save(new Photo(
                    photoId,
                    trip,
                    tripStop,
                    uploadedBy,
                    storagePath,
                    normalizedFilename(file.getOriginalFilename(), extension),
                    file.getContentType(),
                    file.getSize(),
                    metadata.takenAt(),
                    metadata.latitude(),
                    metadata.longitude(),
                    caption));
            return toResponse(photo, storageService.createSignedUrl(storagePath, accessToken));
        } catch (RuntimeException exception) {
            try {
                storageService.delete(storagePath, accessToken);
            } catch (RuntimeException ignored) {
                // Keep the original database error; an orphan can be cleaned up separately.
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> list(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        String accessToken = authenticatedUserProvider.getCurrentAccessToken();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireViewAccess(trip, userId);
        return photoRepository.findByTripIdOrderByTakenAtAscCreatedAtAsc(tripId).stream()
                .map(photo -> toResponse(photo, storageService.createSignedUrl(photo.getStoragePath(), accessToken)))
                .toList();
    }

    @Transactional
    public void delete(UUID tripId, UUID photoId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        String accessToken = authenticatedUserProvider.getCurrentAccessToken();
        Trip trip = tripService.getTripEntity(tripId);
        Photo photo = photoRepository.findByIdAndTripId(photoId, tripId)
                .orElseThrow(() -> new PhotoNotFoundException(photoId));

        boolean owner = trip.getOwner().getId().equals(userId);
        boolean ownEditablePhoto = photo.getUploadedBy().getId().equals(userId)
                && tripPermissionService.canEditTripContent(trip, userId);
        if (!owner && !ownEditablePhoto) {
            throw new TripAccessDeniedException();
        }

        storageService.delete(photo.getStoragePath(), accessToken);
        photoRepository.delete(photo);
    }

    private void validate(MultipartFile file, String caption) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPhotoException("Select a non-empty image to upload.");
        }
        if (!EXTENSIONS.containsKey(file.getContentType())) {
            throw new InvalidPhotoException("Only JPEG, PNG and WebP images are supported.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidPhotoException("Photos cannot be larger than 10 MB.");
        }
        if (caption != null && caption.trim().length() > 1000) {
            throw new InvalidPhotoException("Photo captions cannot exceed 1000 characters.");
        }
        if (!hasExpectedSignature(file)) {
            throw new InvalidPhotoException("The selected file does not contain a valid image header.");
        }
    }

    private boolean hasExpectedSignature(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            return switch (file.getContentType()) {
                case "image/jpeg" -> header.length >= 3
                        && unsigned(header[0]) == 0xff
                        && unsigned(header[1]) == 0xd8
                        && unsigned(header[2]) == 0xff;
                case "image/png" -> header.length >= 8
                        && unsigned(header[0]) == 0x89
                        && header[1] == 'P'
                        && header[2] == 'N'
                        && header[3] == 'G'
                        && unsigned(header[4]) == 0x0d
                        && unsigned(header[5]) == 0x0a
                        && unsigned(header[6]) == 0x1a
                        && unsigned(header[7]) == 0x0a;
                case "image/webp" -> header.length >= 12
                        && header[0] == 'R'
                        && header[1] == 'I'
                        && header[2] == 'F'
                        && header[3] == 'F'
                        && header[8] == 'W'
                        && header[9] == 'E'
                        && header[10] == 'B'
                        && header[11] == 'P';
                default -> false;
            };
        } catch (IOException exception) {
            throw new InvalidPhotoException("The selected image could not be read.");
        }
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }

    private String normalizedFilename(String originalFilename, String extension) {
        if (originalFilename == null || originalFilename.isBlank()) return "photo." + extension;
        String filename = originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).trim();
        if (filename.length() > 255) filename = filename.substring(filename.length() - 255);
        return filename.isBlank() ? "photo." + extension : filename;
    }

    private PhotoResponse toResponse(Photo photo, String signedUrl) {
        return new PhotoResponse(
                photo.getId(),
                photo.getTrip().getId(),
                photo.getTripStop() == null ? null : photo.getTripStop().getId(),
                photo.getUploadedBy().getId(),
                photo.getUploadedBy().getDisplayName(),
                photo.getStoragePath(),
                signedUrl,
                photo.getOriginalFileName(),
                photo.getContentType(),
                photo.getFileSize(),
                photo.getTakenAt(),
                photo.getLatitude(),
                photo.getLongitude(),
                photo.getCaption(),
                photo.getCreatedAt());
    }
}
