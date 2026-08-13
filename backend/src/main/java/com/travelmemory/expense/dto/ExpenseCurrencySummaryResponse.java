package com.travelmemory.expense.dto;

import com.travelmemory.expense.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ExpenseCurrencySummaryResponse(
        String currency,
        BigDecimal total,
        BigDecimal costPerDay,
        BigDecimal costPerPerson,
        ExpenseDayTotalResponse mostExpensiveDay,
        ExpenseResponse largestExpense,
        Map<ExpenseCategory, BigDecimal> categoryTotals,
        List<ExpenseSettlementResponse> settlements) {
}

