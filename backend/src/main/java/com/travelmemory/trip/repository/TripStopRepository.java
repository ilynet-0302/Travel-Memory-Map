package com.travelmemory.trip.repository;

import com.travelmemory.trip.entity.TripStop;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TripStopRepository extends JpaRepository<TripStop, UUID> {
    List<TripStop> findByTripIdOrderByPositionAsc(UUID tripId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stop from TripStop stop where stop.trip.id = :tripId order by stop.position asc")
    List<TripStop> findByTripIdForUpdate(@Param("tripId") UUID tripId);
    List<TripStop> findByTripIdIn(Collection<UUID> tripIds);
    Optional<TripStop> findByIdAndTripId(UUID stopId, UUID tripId);
    long countByTripId(UUID tripId);
    long countByTripIdIn(Collection<UUID> tripIds);
}
