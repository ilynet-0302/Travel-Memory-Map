package com.travelmemory.photo.repository;

import com.travelmemory.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    List<Photo> findByTripIdOrderByTakenAtAscCreatedAtAsc(UUID tripId);
    List<Photo> findByTripIdIn(Collection<UUID> tripIds);
    Optional<Photo> findByIdAndTripId(UUID photoId, UUID tripId);
    long countByTripId(UUID tripId);
    long countByTripIdIn(Collection<UUID> tripIds);
    long countByTripIdAndPublicVisibleTrue(UUID tripId);
    List<Photo> findByTripIdAndPublicVisibleTrueOrderByTakenAtAscCreatedAtAsc(UUID tripId);
}
