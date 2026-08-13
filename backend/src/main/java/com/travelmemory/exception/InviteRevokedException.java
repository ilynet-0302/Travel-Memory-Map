package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InviteRevokedException extends DomainException {
    public InviteRevokedException() {
        super("INVITE_REVOKED", "This invitation is no longer valid.", HttpStatus.GONE);
    }
}
