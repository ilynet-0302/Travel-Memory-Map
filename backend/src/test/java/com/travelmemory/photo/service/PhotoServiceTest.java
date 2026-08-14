package com.travelmemory.photo.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.InvalidPhotoException;
import com.travelmemory.exception.TripAccessDeniedException;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.photo.dto.PhotoResponse;
import com.travelmemory.photo.dto.UpdatePhotoPublicVisibilityRequest;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhotoServiceTest {

    private TripService tripService;
    private PhotoRepository photoRepository;
    private TripPermissionService permissionService;
    private AuthenticatedUserProvider userProvider;
    private UserProfileService userProfileService;
    private PhotoMetadataExtractor metadataExtractor;
    private PhotoStorageService storageService;
    private PhotoService service;
    private UserProfile owner;
    private UserProfile editor;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripService = mock(TripService.class);
        photoRepository = mock(PhotoRepository.class);
        permissionService = mock(TripPermissionService.class);
        userProvider = mock(AuthenticatedUserProvider.class);
        userProfileService = mock(UserProfileService.class);
        metadataExtractor = mock(PhotoMetadataExtractor.class);
        storageService = mock(PhotoStorageService.class);
        service = new PhotoService(
                tripService,
                mock(TripStopRepository.class),
                photoRepository,
                permissionService,
                userProvider,
                userProfileService,
                metadataExtractor,
                storageService);

        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        editor = new UserProfile(UUID.randomUUID(), "editor@example.com", "Editor");
        trip = new Trip(
                owner,
                "Rome",
                null,
                "Italy",
                "IT",
                "Rome",
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 16),
                TripVisibility.PRIVATE);
        when(tripService.getTripEntity(trip.getId())).thenReturn(trip);
        when(userProvider.getCurrentAccessToken()).thenReturn("user-access-token");
    }

    @Test
    void editorUploadUsesPrivateTripPathAndExtractedMetadata() {
        AuthenticatedUser identity = new AuthenticatedUser(editor.getId(), editor.getEmail(), editor.getDisplayName());
        MockMultipartFile file = new MockMultipartFile(
                "file", "rome.jpg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9});
        ExtractedPhotoMetadata metadata = ExtractedPhotoMetadata.empty();
        when(userProvider.getCurrentUser()).thenReturn(identity);
        when(userProfileService.synchronizeProfile(identity)).thenReturn(editor);
        when(metadataExtractor.extract(file)).thenReturn(metadata);
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.createSignedUrl(any(), any())).thenReturn("https://signed.example/photo");

        PhotoResponse response = service.upload(trip.getId(), null, "Golden hour", file);

        assertThat(response.tripId()).isEqualTo(trip.getId());
        assertThat(response.uploadedByUserId()).isEqualTo(editor.getId());
        assertThat(response.storagePath()).startsWith(trip.getId() + "/").endsWith(".jpg");
        assertThat(response.caption()).isEqualTo("Golden hour");
        verify(permissionService).requireEditorOrOwner(trip, editor.getId());
        verify(storageService).upload(response.storagePath(), file, "user-access-token");
    }

    @Test
    void uploadRejectsUnsupportedFilesBeforeCallingStorage() {
        AuthenticatedUser identity = new AuthenticatedUser(editor.getId(), editor.getEmail(), editor.getDisplayName());
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", new byte[]{1});
        when(userProvider.getCurrentUser()).thenReturn(identity);

        assertThatThrownBy(() -> service.upload(trip.getId(), null, null, file))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("JPEG");
        verify(storageService, never()).upload(any(), any(), any());
    }

    @Test
    void uploadRejectsAFileWithSpoofedImageContentType() {
        AuthenticatedUser identity = new AuthenticatedUser(editor.getId(), editor.getEmail(), editor.getDisplayName());
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", "not really an image".getBytes());
        when(userProvider.getCurrentUser()).thenReturn(identity);

        assertThatThrownBy(() -> service.upload(trip.getId(), null, null, file))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("valid image header");
        verify(storageService, never()).upload(any(), any(), any());
    }

    @Test
    void editorCannotDeletePhotoUploadedBySomeoneElse() {
        AuthenticatedUser identity = new AuthenticatedUser(editor.getId(), editor.getEmail(), editor.getDisplayName());
        Photo photo = new Photo(
                UUID.randomUUID(),
                trip,
                null,
                owner,
                trip.getId() + "/owner.jpg",
                "owner.jpg",
                "image/jpeg",
                10,
                null,
                null,
                null,
                null);
        when(userProvider.getCurrentUser()).thenReturn(identity);
        when(photoRepository.findByIdAndTripId(photo.getId(), trip.getId())).thenReturn(Optional.of(photo));
        when(permissionService.canEditTripContent(trip, editor.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(trip.getId(), photo.getId()))
                .isInstanceOf(TripAccessDeniedException.class);
        verify(storageService, never()).delete(any(), any());
    }

    @Test
    void editorCanSelectAPhotoForAPublicTripPage() {
        Trip publicTrip = new Trip(
                owner, "Rome", null, "Italy", "IT", "Rome",
                LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 16), TripVisibility.PUBLIC);
        Photo photo = new Photo(
                UUID.randomUUID(), publicTrip, null, editor, publicTrip.getId() + "/rome.jpg",
                "rome.jpg", "image/jpeg", 10, null, null, null, "Golden hour");
        when(userProvider.getCurrentUser()).thenReturn(
                new AuthenticatedUser(editor.getId(), editor.getEmail(), editor.getDisplayName()));
        when(tripService.getTripEntity(publicTrip.getId())).thenReturn(publicTrip);
        when(photoRepository.findByIdAndTripId(photo.getId(), publicTrip.getId())).thenReturn(Optional.of(photo));
        when(photoRepository.countByTripIdAndPublicVisibleTrue(publicTrip.getId())).thenReturn(0L);
        when(storageService.createSignedUrl(photo.getStoragePath(), "user-access-token"))
                .thenReturn("https://signed.example/photo");

        PhotoResponse response = service.updatePublicVisibility(
                publicTrip.getId(), photo.getId(), new UpdatePhotoPublicVisibilityRequest(true));

        assertThat(response.publicVisible()).isTrue();
        verify(permissionService).requireEditorOrOwner(publicTrip, editor.getId());
    }

    @Test
    void cannotPublishAPhotoWhileTripIsPrivate() {
        Photo photo = new Photo(
                UUID.randomUUID(), trip, null, owner, trip.getId() + "/rome.jpg",
                "rome.jpg", "image/jpeg", 10, null, null, null, null);
        when(userProvider.getCurrentUser()).thenReturn(
                new AuthenticatedUser(owner.getId(), owner.getEmail(), owner.getDisplayName()));
        when(photoRepository.findByIdAndTripId(photo.getId(), trip.getId())).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> service.updatePublicVisibility(
                trip.getId(), photo.getId(), new UpdatePhotoPublicVisibilityRequest(true)))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("public trip");
    }
}
