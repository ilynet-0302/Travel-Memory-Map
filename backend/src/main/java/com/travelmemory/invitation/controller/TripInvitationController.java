package com.travelmemory.invitation.controller;

import com.travelmemory.invitation.dto.CreateInviteRequest;
import com.travelmemory.invitation.dto.CreateInviteResponse;
import com.travelmemory.invitation.dto.InviteResponse;
import com.travelmemory.invitation.service.TripInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/invites")
public class TripInvitationController {

    private final TripInvitationService tripInvitationService;

    public TripInvitationController(TripInvitationService tripInvitationService) {
        this.tripInvitationService = tripInvitationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInviteResponse createInvite(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateInviteRequest request) {
        return tripInvitationService.createInvite(tripId, request);
    }

    @GetMapping
    public List<InviteResponse> listInvites(@PathVariable UUID tripId) {
        return tripInvitationService.listInvites(tripId);
    }

    @DeleteMapping("/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvite(@PathVariable UUID tripId, @PathVariable UUID inviteId) {
        tripInvitationService.revokeInvite(tripId, inviteId);
    }
}
