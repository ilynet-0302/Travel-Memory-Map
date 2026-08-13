package com.travelmemory.invitation.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.AlreadyTripMemberException;
import com.travelmemory.exception.InviteExpiredException;
import com.travelmemory.exception.InviteRevokedException;
import com.travelmemory.exception.InviteUsageLimitReachedException;
import com.travelmemory.exception.TripAccessDeniedException;
import com.travelmemory.invitation.dto.AcceptInviteResponse;
import com.travelmemory.invitation.dto.CreateInviteRequest;
import com.travelmemory.invitation.dto.CreateInviteResponse;
import com.travelmemory.invitation.entity.TripInvite;
import com.travelmemory.invitation.repository.TripInviteRepository;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripInvitationServiceTest {

    private static final String RAW_TOKEN = "test-token-that-is-only-shown-once";
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-11T10:00:00Z"), ZoneOffset.UTC);

    private TripInviteRepository inviteRepository;
    private TripMemberRepository memberRepository;
    private TripService tripService;
    private TripPermissionService permissionService;
    private AuthenticatedUserProvider userProvider;
    private UserProfileService profileService;
    private InvitationTokenGenerator tokenGenerator;
    private TripInvitationService service;
    private AuthenticatedUser currentUser;
    private UserProfile owner;
    private Trip trip;

    @BeforeEach
    void setUp() {
        inviteRepository = mock(TripInviteRepository.class);
        memberRepository = mock(TripMemberRepository.class);
        tripService = mock(TripService.class);
        permissionService = mock(TripPermissionService.class);
        userProvider = mock(AuthenticatedUserProvider.class);
        profileService = mock(UserProfileService.class);
        tokenGenerator = mock(InvitationTokenGenerator.class);
        service = new TripInvitationService(
                inviteRepository,
                memberRepository,
                tripService,
                permissionService,
                userProvider,
                profileService,
                tokenGenerator,
                CLOCK);

        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        currentUser = new AuthenticatedUser(owner.getId(), owner.getEmail(), owner.getDisplayName());
        trip = new Trip(
                owner,
                "Rome",
                "Five days in Rome",
                "Italy",
                "IT",
                "Rome",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 5),
                TripVisibility.PRIVATE);
        when(userProvider.getCurrentUser()).thenReturn(currentUser);
        when(tripService.getTripEntity(trip.getId())).thenReturn(trip);
        when(profileService.synchronizeProfile(currentUser)).thenReturn(owner);
        when(tokenGenerator.generateRawToken()).thenReturn(RAW_TOKEN);
        when(tokenGenerator.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(inviteRepository.save(any(TripInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.save(any(TripMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rawTokenIsReturnedOnceButOnlyItsHashIsPersisted() {
        CreateInviteResponse response = service.createInvite(
                trip.getId(), new CreateInviteRequest(TripRole.EDITOR, 7, 3));

        ArgumentCaptor<TripInvite> captor = ArgumentCaptor.forClass(TripInvite.class);
        verify(inviteRepository).save(captor.capture());
        assertThat(response.token()).isEqualTo(RAW_TOKEN);
        assertThat(captor.getValue().getTokenHash()).isEqualTo(TOKEN_HASH).doesNotContain(RAW_TOKEN);
    }

    @Test
    void viewerCannotCreateInvitation() {
        doThrow(new TripAccessDeniedException())
                .when(permissionService).requireOwner(trip, currentUser.id());

        assertThatThrownBy(() -> service.createInvite(
                trip.getId(), new CreateInviteRequest(TripRole.VIEWER, 7, 1)))
                .isInstanceOf(TripAccessDeniedException.class);
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void validInvitationCreatesMembershipAndIncrementsUsage() {
        TripInvite invite = activeInvite(2);
        when(inviteRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(invite));
        when(memberRepository.existsByTripIdAndUserId(trip.getId(), currentUser.id())).thenReturn(false);

        AcceptInviteResponse response = service.acceptInvite(RAW_TOKEN);

        assertThat(response.tripId()).isEqualTo(trip.getId());
        assertThat(response.role()).isEqualTo(TripRole.EDITOR);
        assertThat(invite.getUseCount()).isEqualTo(1);
        verify(memberRepository).save(any(TripMember.class));
    }

    @Test
    void expiredInvitationIsRejected() {
        TripInvite invite = new TripInvite(
                trip, owner, TOKEN_HASH, TripRole.EDITOR,
                OffsetDateTime.now(CLOCK).minusSeconds(1), 1);
        assertRejected(invite, InviteExpiredException.class);
    }

    @Test
    void revokedInvitationIsRejected() {
        TripInvite invite = activeInvite(1);
        invite.revoke(OffsetDateTime.now(CLOCK));
        assertRejected(invite, InviteRevokedException.class);
    }

    @Test
    void exhaustedInvitationIsRejected() {
        TripInvite invite = activeInvite(1);
        invite.incrementUsage();
        assertRejected(invite, InviteUsageLimitReachedException.class);
    }

    @Test
    void existingMemberCannotAcceptInvitationAgain() {
        TripInvite invite = activeInvite(2);
        when(inviteRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(invite));
        when(memberRepository.existsByTripIdAndUserId(trip.getId(), currentUser.id())).thenReturn(true);

        assertThatThrownBy(() -> service.acceptInvite(RAW_TOKEN))
                .isInstanceOf(AlreadyTripMemberException.class);
        assertThat(invite.getUseCount()).isZero();
        verify(memberRepository, never()).save(any());
    }

    @Test
    void ownerCanRevokeInvitation() {
        TripInvite invite = activeInvite(1);
        when(inviteRepository.findByIdAndTripId(invite.getId(), trip.getId())).thenReturn(Optional.of(invite));

        service.revokeInvite(trip.getId(), invite.getId());

        assertThat(invite.isRevoked()).isTrue();
    }

    private TripInvite activeInvite(int maxUses) {
        return new TripInvite(
                trip, owner, TOKEN_HASH, TripRole.EDITOR,
                OffsetDateTime.now(CLOCK).plusDays(1), maxUses);
    }

    private void assertRejected(TripInvite invite, Class<? extends RuntimeException> exceptionType) {
        when(inviteRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(invite));
        assertThatThrownBy(() -> service.acceptInvite(RAW_TOKEN)).isInstanceOf(exceptionType);
        verify(memberRepository, never()).save(any());
    }
}
