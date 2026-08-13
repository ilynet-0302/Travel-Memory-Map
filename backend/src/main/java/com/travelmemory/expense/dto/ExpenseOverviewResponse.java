package com.travelmemory.expense.dto;

import java.util.List;

public record ExpenseOverviewResponse(
        List<ExpenseResponse> expenses,
        List<ExpenseCurrencySummaryResponse> summaries) {
}

