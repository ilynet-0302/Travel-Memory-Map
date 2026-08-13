package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InviteNotFoundException extends DomainException {
    public InviteNotFoundException() {
        super("INVITE_NOT_FOUND", "This invitation does not exist.", HttpStatus.NOT_FOUND);
    }
}
