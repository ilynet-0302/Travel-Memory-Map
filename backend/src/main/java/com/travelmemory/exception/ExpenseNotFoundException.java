package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ExpenseNotFoundException extends DomainException {
    public ExpenseNotFoundException(UUID expenseId) {
        super("EXPENSE_NOT_FOUND", "Expense %s was not found.".formatted(expenseId), HttpStatus.NOT_FOUND);
    }
}

