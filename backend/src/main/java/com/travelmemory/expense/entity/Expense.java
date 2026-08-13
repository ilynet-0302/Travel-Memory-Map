package com.travelmemory.expense.entity;

import com.travelmemory.trip.entity.Trip;
import com.travelmemory.user.entity.UserProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(name = "expense_date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_user_id", nullable = false)
    private UserProfile paidBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private UserProfile createdBy;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseParticipant> participants = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Expense() {
    }

    public Expense(
            Trip trip,
            String title,
            BigDecimal amount,
            String currency,
            ExpenseCategory category,
            LocalDate date,
            UserProfile paidBy,
            UserProfile createdBy,
            List<UserProfile> participantUsers) {
        this.id = UUID.randomUUID();
        this.trip = trip;
        this.createdBy = createdBy;
        updateDetails(title, amount, currency, category, date, paidBy, participantUsers);
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = this.createdAt;
    }

    public void updateDetails(
            String title,
            BigDecimal amount,
            String currency,
            ExpenseCategory category,
            LocalDate date,
            UserProfile paidBy,
            List<UserProfile> participantUsers) {
        this.title = title.trim();
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.trim().toUpperCase();
        this.category = category;
        this.date = date;
        this.paidBy = paidBy;
        replaceParticipants(participantUsers);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void replaceParticipants(List<UserProfile> participantUsers) {
        List<UserProfile> orderedUsers = participantUsers.stream()
                .sorted(Comparator.comparing(user -> user.getId().toString()))
                .toList();
        Map<UUID, ExpenseParticipant> existingByUserId = new HashMap<>();
        participants.forEach(participant -> existingByUserId.put(participant.getUser().getId(), participant));
        participants.removeIf(participant -> orderedUsers.stream()
                .noneMatch(user -> user.getId().equals(participant.getUser().getId())));
        BigDecimal baseShare = amount.divide(BigDecimal.valueOf(orderedUsers.size()), 2, RoundingMode.DOWN);
        int remainderCents = amount
                .subtract(baseShare.multiply(BigDecimal.valueOf(orderedUsers.size())))
                .movePointRight(2)
                .intValueExact();
        for (int index = 0; index < orderedUsers.size(); index++) {
            BigDecimal share = index < remainderCents ? baseShare.add(new BigDecimal("0.01")) : baseShare;
            ExpenseParticipant existing = existingByUserId.get(orderedUsers.get(index).getId());
            if (existing == null) participants.add(new ExpenseParticipant(this, orderedUsers.get(index), share));
            else existing.updateShare(share);
        }
    }

    public UUID getId() { return id; }
    public Trip getTrip() { return trip; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public ExpenseCategory getCategory() { return category; }
    public LocalDate getDate() { return date; }
    public UserProfile getPaidBy() { return paidBy; }
    public UserProfile getCreatedBy() { return createdBy; }
    public List<ExpenseParticipant> getParticipants() { return List.copyOf(participants); }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
