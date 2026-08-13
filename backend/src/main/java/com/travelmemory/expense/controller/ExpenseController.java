package com.travelmemory.expense.controller;

import com.travelmemory.expense.dto.ExpenseOverviewResponse;
import com.travelmemory.expense.dto.ExpenseResponse;
import com.travelmemory.expense.dto.UpsertExpenseRequest;
import com.travelmemory.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ExpenseOverviewResponse overview(@PathVariable UUID tripId) {
        return expenseService.overview(tripId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpsertExpenseRequest request) {
        return expenseService.create(tripId, request);
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponse update(
            @PathVariable UUID tripId,
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpsertExpenseRequest request) {
        return expenseService.update(tripId, expenseId, request);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID tripId, @PathVariable UUID expenseId) {
        expenseService.delete(tripId, expenseId);
    }
}

