package com.travelmemory.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseDayTotalResponse(LocalDate date, BigDecimal amount) {
}

