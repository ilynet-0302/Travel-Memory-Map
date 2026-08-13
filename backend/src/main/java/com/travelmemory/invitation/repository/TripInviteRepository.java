package com.travelmemory.invitation.repository;

import com.travelmemory.invitation.entity.TripInvite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripInviteRepository extends JpaRepository<TripInvite, UUID> {
    Optional<TripInvite> findByTokenHash(String tokenHash);
    Optional<TripInvite> findByIdAndTripId(UUID inviteId, UUID tripId);
    List<TripInvite> findByTripIdOrderByCreatedAtDesc(UUID tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invite from TripInvite invite where invite.tokenHash = :tokenHash")
    Optional<TripInvite> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
