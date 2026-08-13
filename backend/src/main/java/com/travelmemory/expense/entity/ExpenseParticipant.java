package com.travelmemory.expense.entity;

import com.travelmemory.user.entity.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "expense_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_expense_participants_expense_user",
                columnNames = {"expense_id", "user_id"}))
public class ExpenseParticipant {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(name = "share_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal shareAmount;

    protected ExpenseParticipant() {
    }

    ExpenseParticipant(Expense expense, UserProfile user, BigDecimal shareAmount) {
        this.id = UUID.randomUUID();
        this.expense = expense;
        this.user = user;
        this.shareAmount = shareAmount;
    }

    void updateShare(BigDecimal shareAmount) {
        this.shareAmount = shareAmount;
    }

    public UUID getId() { return id; }
    public Expense getExpense() { return expense; }
    public UserProfile getUser() { return user; }
    public BigDecimal getShareAmount() { return shareAmount; }
}
