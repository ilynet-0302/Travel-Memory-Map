package com.travelmemory.expense.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.InvalidExpenseException;
import com.travelmemory.exception.TripAccessDeniedException;
import com.travelmemory.expense.dto.ExpenseResponse;
import com.travelmemory.expense.dto.UpsertExpenseRequest;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseServiceTest {

    private TripService tripService;
    private ExpenseRepository expenseRepository;
    private TripMemberRepository memberRepository;
    private TripPermissionService permissionService;
    private AuthenticatedUserProvider userProvider;
    private ExpenseService service;
    private UserProfile owner;
    private UserProfile editor;
    private UserProfile outsider;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripService = mock(TripService.class);
        expenseRepository = mock(ExpenseRepository.class);
        memberRepository = mock(TripMemberRepository.class);
        permissionService = mock(TripPermissionService.class);
        userProvider = mock(AuthenticatedUserProvider.class);
        service = new ExpenseService(
                tripService,
                expenseRepository,
                memberRepository,
                permissionService,
                userProvider,
                new ExpenseSettlementCalculator());
        owner = profile("Owner");
        editor = profile("Editor");
        outsider = profile("Outsider");
        trip = new Trip(
                owner,
                "Rome",
                null,
                "Italy",
                "IT",
                "Rome",
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 16),
                TripVisibility.PRIVATE);
        when(tripService.getTripEntity(trip.getId())).thenReturn(trip);
        when(memberRepository.findByTripIdOrderByJoinedAtAsc(trip.getId())).thenReturn(List.of(
                new TripMember(trip, owner, TripRole.OWNER),
                new TripMember(trip, editor, TripRole.EDITOR)));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void editorCanCreateAnEquallySplitExpenseForTripMembers() {
        authenticate(editor);

        ExpenseResponse response = service.create(trip.getId(), request(owner.getId(), List.of(owner.getId(), editor.getId())));

        assertThat(response.amount()).isEqualByComparingTo("90.00");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.participants()).hasSize(2).allSatisfy(participant ->
                assertThat(participant.shareAmount()).isEqualByComparingTo("45.00"));
        verify(permissionService).requireEditorOrOwner(trip, editor.getId());
    }

    @Test
    void createRejectsPayersOrParticipantsOutsideTheTrip() {
        authenticate(editor);

        assertThatThrownBy(() -> service.create(
                trip.getId(), request(outsider.getId(), List.of(owner.getId(), outsider.getId()))))
                .isInstanceOf(InvalidExpenseException.class)
                .hasMessageContaining("payer");
    }

    @Test
    void editorCannotModifyAnExpenseCreatedByTheOwner() {
        authenticate(editor);
        Expense ownerExpense = new Expense(
                trip,
                "Hotel",
                new BigDecimal("300.00"),
                "EUR",
                ExpenseCategory.ACCOMMODATION,
                trip.getStartDate(),
                owner,
                owner,
                List.of(owner, editor));
        when(expenseRepository.findByIdAndTripId(ownerExpense.getId(), trip.getId()))
                .thenReturn(Optional.of(ownerExpense));
        when(permissionService.canEditTripContent(trip, editor.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.update(
                trip.getId(), ownerExpense.getId(), request(owner.getId(), List.of(owner.getId(), editor.getId()))))
                .isInstanceOf(TripAccessDeniedException.class);
    }

    private UpsertExpenseRequest request(UUID paidBy, List<UUID> participants) {
        return new UpsertExpenseRequest(
                "Dinner",
                new BigDecimal("90.00"),
                "eur",
                ExpenseCategory.FOOD,
                LocalDate.of(2026, 9, 13),
                paidBy,
                participants);
    }

    private void authenticate(UserProfile profile) {
        when(userProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(
                profile.getId(), profile.getEmail(), profile.getDisplayName()));
    }

    private UserProfile profile(String name) {
        return new UserProfile(UUID.randomUUID(), name.toLowerCase() + "@example.com", name);
    }
}

