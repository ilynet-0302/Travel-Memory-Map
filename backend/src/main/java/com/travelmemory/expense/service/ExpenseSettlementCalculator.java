package com.travelmemory.expense.service;

import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseParticipant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ExpenseSettlementCalculator {

    public List<Transfer> calculate(List<Expense> expenses) {
        Map<UUID, Balance> balances = new HashMap<>();
        for (Expense expense : expenses) {
            balances.computeIfAbsent(
                    expense.getPaidBy().getId(),
                    id -> new Balance(id, expense.getPaidBy().getDisplayName()))
                    .add(expense.getAmount());
            for (ExpenseParticipant participant : expense.getParticipants()) {
                balances.computeIfAbsent(
                        participant.getUser().getId(),
                        id -> new Balance(id, participant.getUser().getDisplayName()))
                        .subtract(participant.getShareAmount());
            }
        }

        Comparator<Balance> byAmountThenId = Comparator
                .comparing(Balance::amount)
                .thenComparing(balance -> balance.userId().toString());
        List<Balance> debtors = balances.values().stream()
                .filter(balance -> balance.amount().signum() < 0)
                .sorted(byAmountThenId)
                .map(Balance::copy)
                .toList();
        List<Balance> creditors = balances.values().stream()
                .filter(balance -> balance.amount().signum() > 0)
                .sorted(byAmountThenId.reversed())
                .map(Balance::copy)
                .toList();

        List<Transfer> transfers = new ArrayList<>();
        int debtorIndex = 0;
        int creditorIndex = 0;
        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            Balance debtor = debtors.get(debtorIndex);
            Balance creditor = creditors.get(creditorIndex);
            BigDecimal amount = debtor.amount().negate().min(creditor.amount()).setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() > 0) {
                transfers.add(new Transfer(
                        debtor.userId(), debtor.displayName(), creditor.userId(), creditor.displayName(), amount));
            }
            debtor.add(amount);
            creditor.subtract(amount);
            if (debtor.amount().signum() == 0) debtorIndex++;
            if (creditor.amount().signum() == 0) creditorIndex++;
        }
        return List.copyOf(transfers);
    }

    public record Transfer(
            UUID fromUserId,
            String fromDisplayName,
            UUID toUserId,
            String toDisplayName,
            BigDecimal amount) {
    }

    private static final class Balance {
        private final UUID userId;
        private final String displayName;
        private BigDecimal amount = BigDecimal.ZERO.setScale(2);

        private Balance(UUID userId, String displayName) {
            this.userId = userId;
            this.displayName = displayName;
        }

        private Balance copy() {
            Balance copy = new Balance(userId, displayName);
            copy.amount = amount;
            return copy;
        }

        private void add(BigDecimal value) { amount = amount.add(value); }
        private void subtract(BigDecimal value) { amount = amount.subtract(value); }
        private UUID userId() { return userId; }
        private String displayName() { return displayName; }
        private BigDecimal amount() { return amount; }
    }
}

