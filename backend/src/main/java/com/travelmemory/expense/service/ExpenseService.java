package com.travelmemory.expense.service;

import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.ExpenseNotFoundException;
import com.travelmemory.exception.InvalidExpenseException;
import com.travelmemory.exception.TripAccessDeniedException;
import com.travelmemory.expense.dto.ExpenseCurrencySummaryResponse;
import com.travelmemory.expense.dto.ExpenseDayTotalResponse;
import com.travelmemory.expense.dto.ExpenseOverviewResponse;
import com.travelmemory.expense.dto.ExpenseParticipantResponse;
import com.travelmemory.expense.dto.ExpenseResponse;
import com.travelmemory.expense.dto.ExpenseSettlementResponse;
import com.travelmemory.expense.dto.UpsertExpenseRequest;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final TripService tripService;
    private final ExpenseRepository expenseRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripPermissionService tripPermissionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ExpenseSettlementCalculator settlementCalculator;

    public ExpenseService(
            TripService tripService,
            ExpenseRepository expenseRepository,
            TripMemberRepository tripMemberRepository,
            TripPermissionService tripPermissionService,
            AuthenticatedUserProvider authenticatedUserProvider,
            ExpenseSettlementCalculator settlementCalculator) {
        this.tripService = tripService;
        this.expenseRepository = expenseRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.tripPermissionService = tripPermissionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.settlementCalculator = settlementCalculator;
    }

    @Transactional(readOnly = true)
    public ExpenseOverviewResponse overview(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireMember(trip, userId);
        List<Expense> expenses = expenseRepository.findByTripIdOrderByDateDescCreatedAtDesc(tripId);
        List<ExpenseResponse> responses = expenses.stream().map(this::toResponse).toList();
        Map<String, List<Expense>> byCurrency = expenses.stream().collect(Collectors.groupingBy(
                Expense::getCurrency,
                LinkedHashMap::new,
                Collectors.toList()));
        long memberCount = Math.max(1, tripMemberRepository.countByTripId(tripId));
        List<ExpenseCurrencySummaryResponse> summaries = byCurrency.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> summarize(trip, entry.getKey(), entry.getValue(), memberCount))
                .toList();
        return new ExpenseOverviewResponse(responses, summaries);
    }

    @Transactional
    public ExpenseResponse create(UUID tripId, UpsertExpenseRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireEditorOrOwner(trip, userId);
        Members members = validateAndResolve(trip, request);
        UserProfile createdBy = members.byId().get(userId);
        if (createdBy == null) throw new TripAccessDeniedException();
        Expense expense = expenseRepository.save(new Expense(
                trip,
                request.title(),
                request.amount(),
                request.currency(),
                request.category(),
                request.date(),
                members.byId().get(request.paidByUserId()),
                createdBy,
                members.participants()));
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse update(UUID tripId, UUID expenseId, UpsertExpenseRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        Expense expense = findExpense(tripId, expenseId);
        requireCanModify(trip, expense, userId);
        Members members = validateAndResolve(trip, request);
        expense.updateDetails(
                request.title(),
                request.amount(),
                request.currency(),
                request.category(),
                request.date(),
                members.byId().get(request.paidByUserId()),
                members.participants());
        return toResponse(expense);
    }

    @Transactional
    public void delete(UUID tripId, UUID expenseId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        Expense expense = findExpense(tripId, expenseId);
        requireCanModify(trip, expense, userId);
        expenseRepository.delete(expense);
    }

    private Members validateAndResolve(Trip trip, UpsertExpenseRequest request) {
        if (request.date().isBefore(trip.getStartDate()) || request.date().isAfter(trip.getEndDate())) {
            throw new InvalidExpenseException("Expense date must be within the trip dates.");
        }
        LinkedHashSet<UUID> uniqueParticipantIds = new LinkedHashSet<>(request.participantUserIds());
        if (uniqueParticipantIds.size() != request.participantUserIds().size()) {
            throw new InvalidExpenseException("Each expense participant can be selected only once.");
        }
        Map<UUID, UserProfile> members = tripMemberRepository.findByTripIdOrderByJoinedAtAsc(trip.getId()).stream()
                .map(TripMember::getUser)
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        if (!members.containsKey(request.paidByUserId())) {
            throw new InvalidExpenseException("The payer must be a member of this trip.");
        }
        List<UserProfile> participants = new ArrayList<>();
        for (UUID participantId : uniqueParticipantIds) {
            UserProfile participant = members.get(participantId);
            if (participant == null) {
                throw new InvalidExpenseException("Every expense participant must be a member of this trip.");
            }
            participants.add(participant);
        }
        return new Members(members, participants);
    }

    private void requireCanModify(Trip trip, Expense expense, UUID userId) {
        boolean owner = trip.getOwner().getId().equals(userId);
        boolean ownExpense = expense.getCreatedBy().getId().equals(userId)
                && tripPermissionService.canEditTripContent(trip, userId);
        if (!owner && !ownExpense) throw new TripAccessDeniedException();
    }

    private Expense findExpense(UUID tripId, UUID expenseId) {
        return expenseRepository.findByIdAndTripId(expenseId, tripId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));
    }

    private ExpenseCurrencySummaryResponse summarize(
            Trip trip, String currency, List<Expense> expenses, long memberCount) {
        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
        long tripDays = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
        Map<ExpenseCategory, BigDecimal> categoryTotals = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : ExpenseCategory.values()) categoryTotals.put(category, BigDecimal.ZERO.setScale(2));
        expenses.forEach(expense -> categoryTotals.merge(expense.getCategory(), expense.getAmount(), BigDecimal::add));

        Map<LocalDate, BigDecimal> dayTotals = expenses.stream().collect(Collectors.groupingBy(
                Expense::getDate,
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
        Map.Entry<LocalDate, BigDecimal> mostExpensiveDay = dayTotals.entrySet().stream()
                .max(Map.Entry.<LocalDate, BigDecimal>comparingByValue()
                        .thenComparing(Map.Entry::getKey))
                .orElseThrow();
        Expense largestExpense = expenses.stream()
                .max(Comparator.comparing(Expense::getAmount)
                        .thenComparing(Expense::getDate)
                        .thenComparing(expense -> expense.getId().toString()))
                .orElseThrow();
        List<ExpenseSettlementResponse> settlements = settlementCalculator.calculate(expenses).stream()
                .map(transfer -> new ExpenseSettlementResponse(
                        transfer.fromUserId(), transfer.fromDisplayName(),
                        transfer.toUserId(), transfer.toDisplayName(), transfer.amount(), currency))
                .toList();
        return new ExpenseCurrencySummaryResponse(
                currency,
                total,
                total.divide(BigDecimal.valueOf(tripDays), 2, RoundingMode.HALF_UP),
                total.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP),
                new ExpenseDayTotalResponse(mostExpensiveDay.getKey(), mostExpensiveDay.getValue().setScale(2)),
                toResponse(largestExpense),
                categoryTotals,
                settlements);
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTrip().getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getCategory(),
                expense.getDate(),
                expense.getPaidBy().getId(),
                expense.getPaidBy().getDisplayName(),
                expense.getCreatedBy().getId(),
                expense.getParticipants().stream()
                        .map(participant -> new ExpenseParticipantResponse(
                                participant.getUser().getId(),
                                participant.getUser().getDisplayName(),
                                participant.getShareAmount()))
                        .toList(),
                expense.getCreatedAt(),
                expense.getUpdatedAt());
    }

    private record Members(Map<UUID, UserProfile> byId, List<UserProfile> participants) {
    }
}

