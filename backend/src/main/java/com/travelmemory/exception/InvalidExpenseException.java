package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InvalidExpenseException extends DomainException {
    public InvalidExpenseException(String message) {
        super("INVALID_EXPENSE", message, HttpStatus.BAD_REQUEST);
    }
}

