package com.travelmemory.trip.repository;

import com.travelmemory.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.travelmemory.trip.entity.TripStatus;
import com.travelmemory.trip.entity.TripVisibility;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    @Query("""
            select trip
            from Trip trip
            where trip.status <> com.travelmemory.trip.entity.TripStatus.ARCHIVED
              and (
                trip.owner.id = :userId
                or exists (
                    select member.id
                    from TripMember member
                    where member.trip = trip and member.user.id = :userId
                )
              )
            order by trip.startDate desc
            """)
    List<Trip> findAccessibleTrips(@Param("userId") UUID userId);

    Optional<Trip> findByPublicSlugAndVisibilityAndStatusNot(
            String publicSlug,
            TripVisibility visibility,
            TripStatus excludedStatus);
}
