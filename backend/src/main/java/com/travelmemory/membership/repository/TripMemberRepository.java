package com.travelmemory.membership.repository;

import com.travelmemory.membership.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripMemberRepository extends JpaRepository<TripMember, UUID> {
    Optional<TripMember> findByIdAndTripId(UUID id, UUID tripId);
    Optional<TripMember> findByTripIdAndUserId(UUID tripId, UUID userId);
    List<TripMember> findByTripIdOrderByJoinedAtAsc(UUID tripId);
    long countByTripId(UUID tripId);
    boolean existsByTripIdAndUserId(UUID tripId, UUID userId);
}
