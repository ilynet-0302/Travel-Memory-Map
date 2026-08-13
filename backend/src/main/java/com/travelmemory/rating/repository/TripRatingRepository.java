package com.travelmemory.rating.repository;

import com.travelmemory.rating.entity.TripRating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TripRatingRepository extends JpaRepository<TripRating, UUID> {

    Optional<TripRating> findByTripIdAndUserId(UUID tripId, UUID userId);

    List<TripRating> findByTripId(UUID tripId);

    List<TripRating> findByTripIdIn(Collection<UUID> tripIds);
}
