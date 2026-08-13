package com.travelmemory.invitation.controller;

import com.travelmemory.invitation.dto.AcceptInviteResponse;
import com.travelmemory.invitation.dto.InvitePreviewResponse;
import com.travelmemory.invitation.service.TripInvitationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invites")
public class InvitationController {

    private final TripInvitationService tripInvitationService;

    public InvitationController(TripInvitationService tripInvitationService) {
        this.tripInvitationService = tripInvitationService;
    }

    @GetMapping("/{token}")
    public InvitePreviewResponse previewInvite(@PathVariable String token) {
        return tripInvitationService.previewInvite(token);
    }

    @PostMapping("/{token}/accept")
    public AcceptInviteResponse acceptInvite(@PathVariable String token) {
        return tripInvitationService.acceptInvite(token);
    }
}
