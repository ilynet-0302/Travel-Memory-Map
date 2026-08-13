package com.travelmemory.expense.repository;

import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.repository.UserProfileRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ExpenseRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private TripMemberRepository memberRepository;
    @Autowired private UserProfileRepository profileRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void migrationPersistsAndUpdatesExactParticipantShares() {
        UserProfile owner = profileRepository.save(profile("Owner"));
        UserProfile editor = profileRepository.save(profile("Editor"));
        UserProfile viewer = profileRepository.save(profile("Viewer"));
        Trip trip = tripRepository.save(new Trip(
                owner, "Rome", null, "Italy", "IT", "Rome",
                LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 16), TripVisibility.PRIVATE));
        memberRepository.save(new TripMember(trip, owner, TripRole.OWNER));
        memberRepository.save(new TripMember(trip, editor, TripRole.EDITOR));
        memberRepository.save(new TripMember(trip, viewer, TripRole.VIEWER));
        Expense expense = expenseRepository.save(new Expense(
                trip, "Dinner", new BigDecimal("100.00"), "EUR", ExpenseCategory.FOOD,
                trip.getStartDate(), owner, owner, List.of(owner, editor, viewer)));
        entityManager.flush();

        expense.updateDetails(
                "Dinner and drinks", new BigDecimal("90.00"), "EUR", ExpenseCategory.FOOD,
                trip.getStartDate(), editor, List.of(owner, editor));
        entityManager.flush();
        entityManager.clear();

        Expense reloaded = expenseRepository.findByIdAndTripId(expense.getId(), trip.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Dinner and drinks");
        assertThat(reloaded.getParticipants()).hasSize(2);
        assertThat(reloaded.getParticipants().stream()
                .map(participant -> participant.getShareAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("90.00");
    }

    private UserProfile profile(String name) {
        return new UserProfile(UUID.randomUUID(), name.toLowerCase() + "@integration.test", name);
    }
}

