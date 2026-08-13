package com.travelmemory.expense.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.expense.dto.ExpenseOverviewResponse;
import com.travelmemory.expense.dto.ExpenseParticipantResponse;
import com.travelmemory.expense.dto.ExpenseResponse;
import com.travelmemory.expense.dto.UpsertExpenseRequest;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class ExpenseControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ExpenseService expenseService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void anonymousUserCannotReadPrivateExpenseData() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/trips/{tripId}/expenses", tripId))
                .andExpect(status().isUnauthorized());

        verify(expenseService, never()).overview(tripId);
    }

    @Test
    void authenticatedMemberCanCreateAValidatedExpense() throws Exception {
        UUID tripId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        when(expenseService.create(any(UUID.class), any(UpsertExpenseRequest.class))).thenReturn(new ExpenseResponse(
                expenseId, tripId, "Dinner", new BigDecimal("120.00"), "EUR", ExpenseCategory.FOOD,
                LocalDate.of(2026, 9, 13), userId, "Iliya", userId,
                List.of(new ExpenseParticipantResponse(userId, "Iliya", new BigDecimal("120.00"))), now, now));

        mockMvc.perform(post("/api/v1/trips/{tripId}/expenses", tripId)
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Dinner",
                                  "amount": 120.00,
                                  "currency": "EUR",
                                  "category": "FOOD",
                                  "date": "2026-09-13",
                                  "paidByUserId": "%s",
                                  "participantUserIds": ["%s"]
                                }
                                """.formatted(userId, userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.amount").value(120.0));
    }

    @Test
    void invalidExpensePayloadIsRejectedBeforeTheService() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/trips/{tripId}/expenses", tripId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "amount": -1,
                                  "currency": "EURO",
                                  "category": "FOOD",
                                  "date": "2026-09-13",
                                  "paidByUserId": "00000000-0000-0000-0000-000000000001",
                                  "participantUserIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(expenseService, never()).create(any(), any());
    }
}

