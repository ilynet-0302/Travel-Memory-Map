package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InvalidPhotoException extends DomainException {
    public InvalidPhotoException(String message) {
        super("INVALID_PHOTO", message, HttpStatus.BAD_REQUEST);
    }
}
