package com.travelmemory.expense.dto;

import com.travelmemory.expense.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID tripId,
        String title,
        BigDecimal amount,
        String currency,
        ExpenseCategory category,
        LocalDate date,
        UUID paidByUserId,
        String paidByDisplayName,
        UUID createdByUserId,
        List<ExpenseParticipantResponse> participants,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

