package com.travelmemory.trip.controller;

import com.travelmemory.membership.dto.TripMemberResponse;
import com.travelmemory.membership.dto.UpdateMemberRoleRequest;
import com.travelmemory.membership.service.TripMemberService;
import com.travelmemory.trip.dto.CreateTripRequest;
import com.travelmemory.trip.dto.CreateTripStopRequest;
import com.travelmemory.trip.dto.ReorderTripStopsRequest;
import com.travelmemory.trip.dto.TripDetailsResponse;
import com.travelmemory.trip.dto.TripStopResponse;
import com.travelmemory.trip.dto.TripSummaryResponse;
import com.travelmemory.trip.dto.TripSearchCriteria;
import com.travelmemory.trip.dto.TripSearchResultResponse;
import com.travelmemory.trip.dto.UpdateTripRequest;
import com.travelmemory.trip.dto.UpdateTripStopRequest;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.trip.service.TripSearchService;
import com.travelmemory.trip.service.TripStopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService tripService;
    private final TripSearchService tripSearchService;
    private final TripStopService tripStopService;
    private final TripMemberService tripMemberService;

    public TripController(
            TripService tripService,
            TripSearchService tripSearchService,
            TripStopService tripStopService,
            TripMemberService tripMemberService) {
        this.tripService = tripService;
        this.tripSearchService = tripSearchService;
        this.tripStopService = tripStopService;
        this.tripMemberService = tripMemberService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripDetailsResponse createTrip(@Valid @RequestBody CreateTripRequest request) {
        return tripService.createTrip(request);
    }

    @GetMapping
    public List<TripSummaryResponse> listTrips() {
        return tripService.listTrips();
    }

    @GetMapping("/search")
    public List<TripSearchResultResponse> searchTrips(@Valid @ModelAttribute TripSearchCriteria criteria) {
        return tripSearchService.search(criteria);
    }

    @GetMapping("/{tripId}")
    public TripDetailsResponse getTrip(@PathVariable UUID tripId) {
        return tripService.getTrip(tripId);
    }

    @PutMapping("/{tripId}")
    public TripDetailsResponse updateTrip(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpdateTripRequest request) {
        return tripService.updateTrip(tripId, request);
    }

    @DeleteMapping("/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable UUID tripId) {
        tripService.deleteTrip(tripId);
    }

    @PatchMapping("/{tripId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveTrip(@PathVariable UUID tripId) {
        tripService.archiveTrip(tripId);
    }

    @PostMapping("/{tripId}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    public TripStopResponse addStop(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateTripStopRequest request) {
        return tripStopService.addStop(tripId, request);
    }

    @GetMapping("/{tripId}/stops")
    public List<TripStopResponse> listStops(@PathVariable UUID tripId) {
        return tripStopService.listStops(tripId);
    }

    @PutMapping("/{tripId}/stops/order")
    public List<TripStopResponse> reorderStops(
            @PathVariable UUID tripId,
            @Valid @RequestBody ReorderTripStopsRequest request) {
        return tripStopService.reorderStops(tripId, request);
    }

    @PutMapping("/{tripId}/stops/{stopId}")
    public TripStopResponse updateStop(
            @PathVariable UUID tripId,
            @PathVariable UUID stopId,
            @Valid @RequestBody UpdateTripStopRequest request) {
        return tripStopService.updateStop(tripId, stopId, request);
    }

    @DeleteMapping("/{tripId}/stops/{stopId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStop(@PathVariable UUID tripId, @PathVariable UUID stopId) {
        tripStopService.deleteStop(tripId, stopId);
    }

    @GetMapping("/{tripId}/members")
    public List<TripMemberResponse> listMembers(@PathVariable UUID tripId) {
        return tripMemberService.listMembers(tripId);
    }

    @PatchMapping("/{tripId}/members/{memberId}")
    public TripMemberResponse updateMemberRole(
            @PathVariable UUID tripId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return tripMemberService.updateMemberRole(tripId, memberId, request);
    }

    @DeleteMapping("/{tripId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID tripId, @PathVariable UUID memberId) {
        tripMemberService.removeMember(tripId, memberId);
    }

    @DeleteMapping("/{tripId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveTrip(@PathVariable UUID tripId) {
        tripMemberService.leaveTrip(tripId);
    }
}
