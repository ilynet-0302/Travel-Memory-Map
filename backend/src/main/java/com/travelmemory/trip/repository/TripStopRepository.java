package com.travelmemory.trip.repository;

import com.travelmemory.trip.entity.TripStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripStopRepository extends JpaRepository<TripStop, UUID> {
    List<TripStop> findByTripIdOrderByPositionAsc(UUID tripId);
    Optional<TripStop> findByIdAndTripId(UUID stopId, UUID tripId);
    long countByTripId(UUID tripId);
}
