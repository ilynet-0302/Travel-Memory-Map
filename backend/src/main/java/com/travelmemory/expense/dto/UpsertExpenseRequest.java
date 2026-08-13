package com.travelmemory.expense.dto;

import com.travelmemory.expense.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpsertExpenseRequest(
        @NotBlank @Size(max = 160) String title,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "(?i)^[A-Z]{3}$", message = "Currency must be a three-letter ISO code.") String currency,
        @NotNull ExpenseCategory category,
        @NotNull LocalDate date,
        @NotNull UUID paidByUserId,
        @NotEmpty @Size(max = 100) List<@NotNull UUID> participantUserIds) {
}

