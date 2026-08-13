package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class PhotoStorageException extends DomainException {
    public PhotoStorageException(String message) {
        super("PHOTO_STORAGE_ERROR", message, HttpStatus.BAD_GATEWAY);
    }
}
