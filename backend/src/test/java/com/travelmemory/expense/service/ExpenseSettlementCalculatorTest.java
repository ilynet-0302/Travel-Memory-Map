package com.travelmemory.expense.service;

import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseSettlementCalculatorTest {

    private final ExpenseSettlementCalculator calculator = new ExpenseSettlementCalculator();

    @Test
    void calculatesMinimalTransfersFromExactParticipantShares() {
        UserProfile owner = profile("Owner");
        UserProfile maria = profile("Maria");
        UserProfile ivan = profile("Ivan");
        Trip trip = trip(owner);
        Expense dinner = expense(trip, owner, new BigDecimal("120.00"), List.of(owner, maria, ivan));
        Expense taxi = expense(trip, maria, new BigDecimal("30.00"), List.of(owner, maria));

        List<ExpenseSettlementCalculator.Transfer> transfers = calculator.calculate(List.of(dinner, taxi));

        assertThat(transfers).hasSize(2);
        assertThat(transfers).anySatisfy(transfer -> {
            assertThat(transfer.fromUserId()).isEqualTo(ivan.getId());
            assertThat(transfer.toUserId()).isEqualTo(owner.getId());
            assertThat(transfer.amount()).isEqualByComparingTo("40.00");
        });
        assertThat(transfers).anySatisfy(transfer -> {
            assertThat(transfer.fromUserId()).isEqualTo(maria.getId());
            assertThat(transfer.toUserId()).isEqualTo(owner.getId());
            assertThat(transfer.amount()).isEqualByComparingTo("25.00");
        });
    }

    @Test
    void allocatesRemainderCentsWithoutLosingMoney() {
        UserProfile owner = profile("Owner");
        UserProfile maria = profile("Maria");
        UserProfile ivan = profile("Ivan");
        Expense expense = expense(trip(owner), owner, new BigDecimal("100.00"), List.of(owner, maria, ivan));

        BigDecimal participantTotal = expense.getParticipants().stream()
                .map(participant -> participant.getShareAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payerShare = expense.getParticipants().stream()
                .filter(participant -> participant.getUser().getId().equals(owner.getId()))
                .findFirst()
                .orElseThrow()
                .getShareAmount();
        BigDecimal transferTotal = calculator.calculate(List.of(expense)).stream()
                .map(ExpenseSettlementCalculator.Transfer::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(participantTotal).isEqualByComparingTo("100.00");
        assertThat(transferTotal).isEqualByComparingTo(new BigDecimal("100.00").subtract(payerShare));
    }

    private Expense expense(
            Trip trip, UserProfile paidBy, BigDecimal amount, List<UserProfile> participants) {
        return new Expense(
                trip,
                "Shared cost",
                amount,
                "EUR",
                ExpenseCategory.FOOD,
                trip.getStartDate(),
                paidBy,
                paidBy,
                participants);
    }

    private Trip trip(UserProfile owner) {
        return new Trip(
                owner,
                "Rome",
                null,
                "Italy",
                "IT",
                "Rome",
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 16),
                TripVisibility.PRIVATE);
    }

    private UserProfile profile(String name) {
        return new UserProfile(UUID.randomUUID(), name.toLowerCase() + "@example.com", name);
    }
}
