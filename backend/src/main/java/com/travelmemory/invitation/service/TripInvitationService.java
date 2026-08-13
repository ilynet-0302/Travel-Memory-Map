package com.travelmemory.invitation.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.AlreadyTripMemberException;
import com.travelmemory.exception.InvalidTripRoleException;
import com.travelmemory.exception.InviteExpiredException;
import com.travelmemory.exception.InviteNotFoundException;
import com.travelmemory.exception.InviteRevokedException;
import com.travelmemory.exception.InviteUsageLimitReachedException;
import com.travelmemory.invitation.dto.AcceptInviteResponse;
import com.travelmemory.invitation.dto.CreateInviteRequest;
import com.travelmemory.invitation.dto.CreateInviteResponse;
import com.travelmemory.invitation.dto.InvitePreviewResponse;
import com.travelmemory.invitation.dto.InviteResponse;
import com.travelmemory.invitation.entity.InviteStatus;
import com.travelmemory.invitation.entity.TripInvite;
import com.travelmemory.invitation.repository.TripInviteRepository;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TripInvitationService {

    private final TripInviteRepository tripInviteRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripService tripService;
    private final TripPermissionService tripPermissionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final UserProfileService userProfileService;
    private final InvitationTokenGenerator tokenGenerator;
    private final Clock clock;

    public TripInvitationService(
            TripInviteRepository tripInviteRepository,
            TripMemberRepository tripMemberRepository,
            TripService tripService,
            TripPermissionService tripPermissionService,
            AuthenticatedUserProvider authenticatedUserProvider,
            UserProfileService userProfileService,
            InvitationTokenGenerator tokenGenerator,
            Clock clock) {
        this.tripInviteRepository = tripInviteRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.tripService = tripService;
        this.tripPermissionService = tripPermissionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userProfileService = userProfileService;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
    }

    @Transactional
    public CreateInviteResponse createInvite(UUID tripId, CreateInviteRequest request) {
        validateAssignableRole(request.role());
        AuthenticatedUser authenticatedUser = authenticatedUserProvider.getCurrentUser();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireOwner(trip, authenticatedUser.id());
        UserProfile creator = userProfileService.synchronizeProfile(authenticatedUser);
        String rawToken = tokenGenerator.generateRawToken();
        OffsetDateTime expiresAt = OffsetDateTime.now(clock).plusDays(request.expiresInDays());
        TripInvite invite = tripInviteRepository.save(new TripInvite(
                trip,
                creator,
                tokenGenerator.hashToken(rawToken),
                request.role(),
                expiresAt,
                request.maxUses()));
        return new CreateInviteResponse(
                invite.getId(),
                rawToken,
                invite.getRole(),
                invite.getExpiresAt(),
                invite.getMaxUses(),
                invite.getUseCount(),
                InviteStatus.ACTIVE,
                invite.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> listInvites(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireOwner(trip, userId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        return tripInviteRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(invite -> toResponse(invite, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public InvitePreviewResponse previewInvite(String rawToken) {
        TripInvite invite = tripInviteRepository.findByTokenHash(tokenGenerator.hashToken(rawToken))
                .orElseThrow(InviteNotFoundException::new);
        validateInvite(invite, OffsetDateTime.now(clock));
        Trip trip = invite.getTrip();
        return new InvitePreviewResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getCountry(),
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                invite.getCreatedBy().getDisplayName(),
                invite.getRole(),
                invite.getExpiresAt());
    }

    @Transactional
    public AcceptInviteResponse acceptInvite(String rawToken) {
        AuthenticatedUser authenticatedUser = authenticatedUserProvider.getCurrentUser();
        TripInvite invite = tripInviteRepository
                .findByTokenHashForUpdate(tokenGenerator.hashToken(rawToken))
                .orElseThrow(InviteNotFoundException::new);
        validateInvite(invite, OffsetDateTime.now(clock));

        UUID tripId = invite.getTrip().getId();
        if (tripMemberRepository.existsByTripIdAndUserId(tripId, authenticatedUser.id())) {
            throw new AlreadyTripMemberException();
        }

        UserProfile user = userProfileService.synchronizeProfile(authenticatedUser);
        TripMember member = tripMemberRepository.save(new TripMember(invite.getTrip(), user, invite.getRole()));
        invite.incrementUsage();
        return new AcceptInviteResponse(tripId, member.getId(), member.getRole());
    }

    @Transactional
    public void revokeInvite(UUID tripId, UUID inviteId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireOwner(trip, userId);
        TripInvite invite = tripInviteRepository.findByIdAndTripId(inviteId, tripId)
                .orElseThrow(InviteNotFoundException::new);
        invite.revoke(OffsetDateTime.now(clock));
    }

    void validateInvite(TripInvite invite, OffsetDateTime now) {
        if (invite.isRevoked()) {
            throw new InviteRevokedException();
        }
        if (invite.isExpired(now)) {
            throw new InviteExpiredException();
        }
        if (invite.hasReachedUsageLimit()) {
            throw new InviteUsageLimitReachedException();
        }
    }

    private void validateAssignableRole(TripRole role) {
        if (role == TripRole.OWNER) {
            throw new InvalidTripRoleException("An invitation can grant only EDITOR or VIEWER access.");
        }
    }

    private InviteResponse toResponse(TripInvite invite, OffsetDateTime now) {
        return new InviteResponse(
                invite.getId(),
                invite.getRole(),
                invite.getExpiresAt(),
                invite.getMaxUses(),
                invite.getUseCount(),
                statusOf(invite, now),
                invite.getCreatedAt());
    }

    private InviteStatus statusOf(TripInvite invite, OffsetDateTime now) {
        if (invite.isRevoked()) return InviteStatus.REVOKED;
        if (invite.isExpired(now)) return InviteStatus.EXPIRED;
        if (invite.hasReachedUsageLimit()) return InviteStatus.USAGE_LIMIT_REACHED;
        return InviteStatus.ACTIVE;
    }
}
