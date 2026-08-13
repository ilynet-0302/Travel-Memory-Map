package com.travelmemory.expense.repository;

import com.travelmemory.expense.entity.Expense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @EntityGraph(attributePaths = {"paidBy", "createdBy", "participants", "participants.user"})
    List<Expense> findByTripIdOrderByDateDescCreatedAtDesc(UUID tripId);

    @EntityGraph(attributePaths = {"paidBy", "createdBy", "participants", "participants.user"})
    Optional<Expense> findByIdAndTripId(UUID id, UUID tripId);

    List<Expense> findByTripId(UUID tripId);

    List<Expense> findByTripIdIn(Collection<UUID> tripIds);
}
