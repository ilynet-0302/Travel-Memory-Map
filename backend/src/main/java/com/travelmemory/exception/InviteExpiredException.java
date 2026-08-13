package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InviteExpiredException extends DomainException {
    public InviteExpiredException() {
        super("INVITE_EXPIRED", "This invitation has expired.", HttpStatus.GONE);
    }
}
