package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class UnauthenticatedException extends DomainException {
    public UnauthenticatedException() {
        this("A valid access token is required.");
    }

    public UnauthenticatedException(String message) {
        super("UNAUTHENTICATED", message, HttpStatus.UNAUTHORIZED);
    }
}
