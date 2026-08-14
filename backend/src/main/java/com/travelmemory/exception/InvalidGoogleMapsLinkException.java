package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InvalidGoogleMapsLinkException extends DomainException {

    public InvalidGoogleMapsLinkException(String message) {
        super("INVALID_GOOGLE_MAPS_LINK", message, HttpStatus.BAD_REQUEST);
    }
}
