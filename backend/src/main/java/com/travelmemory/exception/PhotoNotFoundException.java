package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PhotoNotFoundException extends DomainException {
    public PhotoNotFoundException(UUID photoId) {
        super("PHOTO_NOT_FOUND", "Photo %s was not found.".formatted(photoId), HttpStatus.NOT_FOUND);
    }
}
