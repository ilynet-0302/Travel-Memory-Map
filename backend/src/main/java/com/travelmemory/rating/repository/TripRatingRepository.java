package com.travelmemory.rating.repository;

import com.travelmemory.rating.entity.TripRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TripRatingRepository extends JpaRepository<TripRating, UUID> {

    Optional<TripRating> findByTripIdAndUserId(UUID tripId, UUID userId);

    long countByTripId(UUID tripId);

    @Query("select avg(r.score) from TripRating r where r.trip.id = :tripId")
    Double averageScoreByTripId(@Param("tripId") UUID tripId);
}
