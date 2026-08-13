package com.travelmemory.expense.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseParticipantResponse(
        UUID userId,
        String displayName,
        BigDecimal shareAmount) {
}

