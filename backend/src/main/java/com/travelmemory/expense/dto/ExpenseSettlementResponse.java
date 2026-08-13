package com.travelmemory.expense.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseSettlementResponse(
        UUID fromUserId,
        String fromDisplayName,
        UUID toUserId,
        String toDisplayName,
        BigDecimal amount,
        String currency) {
}

