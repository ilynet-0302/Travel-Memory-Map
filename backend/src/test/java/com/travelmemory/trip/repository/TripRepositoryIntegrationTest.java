package com.travelmemory.trip.repository;

import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class TripRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripMemberRepository tripMemberRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void memberCanListSharedPrivateTrip() {
        UserProfile owner = userProfileRepository.save(new UserProfile(
                UUID.randomUUID(), "owner@integration.test", "Owner"));
        UserProfile editor = userProfileRepository.save(new UserProfile(
                UUID.randomUUID(), "editor@integration.test", "Editor"));
        Trip trip = tripRepository.save(new Trip(
                owner,
                "Italian road trip",
                "A shared private trip",
                "Italy",
                "IT",
                "Rome",
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                TripVisibility.PRIVATE));
        tripMemberRepository.save(new TripMember(trip, owner, TripRole.OWNER));
        tripMemberRepository.save(new TripMember(trip, editor, TripRole.EDITOR));

        List<Trip> accessibleTrips = tripRepository.findAccessibleTrips(editor.getId());

        assertThat(accessibleTrips).extracting(Trip::getId).containsExactly(trip.getId());
    }
}
