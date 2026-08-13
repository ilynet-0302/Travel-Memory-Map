package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InviteUsageLimitReachedException extends DomainException {
    public InviteUsageLimitReachedException() {
        super("INVITE_USAGE_LIMIT_REACHED", "This invitation has reached its maximum number of uses.", HttpStatus.GONE);
    }
}
