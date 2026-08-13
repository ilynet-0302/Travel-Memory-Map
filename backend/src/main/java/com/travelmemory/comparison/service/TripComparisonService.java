package com.travelmemory.comparison.service;

import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.comparison.dto.TripComparisonResponse;
import com.travelmemory.comparison.dto.TripComparisonSpendingResponse;
import com.travelmemory.comparison.dto.TripComparisonTripResponse;
import com.travelmemory.exception.InvalidTripComparisonException;
import com.travelmemory.exception.TripNotFoundException;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.rating.dto.TripRatingBreakdownResponse;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.entity.WouldReturn;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
public class TripComparisonService {

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final PhotoRepository photoRepository;
    private final ExpenseRepository expenseRepository;
    private final TripRatingRepository tripRatingRepository;
    private final TripPermissionService tripPermissionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public TripComparisonService(
            TripRepository tripRepository,
            TripStopRepository tripStopRepository,
            PhotoRepository photoRepository,
            ExpenseRepository expenseRepository,
            TripRatingRepository tripRatingRepository,
            TripPermissionService tripPermissionService,
            AuthenticatedUserProvider authenticatedUserProvider) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.photoRepository = photoRepository;
        this.expenseRepository = expenseRepository;
        this.tripRatingRepository = tripRatingRepository;
        this.tripPermissionService = tripPermissionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional(readOnly = true)
    public TripComparisonResponse compare(UUID leftTripId, UUID rightTripId) {
        if (leftTripId.equals(rightTripId)) {
            throw new InvalidTripComparisonException("Choose two different trips to compare.");
        }

        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip left = findTrip(leftTripId);
        Trip right = findTrip(rightTripId);
        tripPermissionService.requireViewAccess(left, userId);
        tripPermissionService.requireViewAccess(right, userId);

        return new TripComparisonResponse(toResponse(left), toResponse(right));
    }

    private Trip findTrip(UUID tripId) {
        return tripRepository.findById(tripId).orElseThrow(() -> new TripNotFoundException(tripId));
    }

    private TripComparisonTripResponse toResponse(Trip trip) {
        List<TripRating> ratings = tripRatingRepository.findByTripId(trip.getId());
        long travelDays = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
        long wouldReturnYes = ratings.stream()
                .filter(rating -> rating.getWouldReturn() == WouldReturn.YES)
                .count();
        BigDecimal wouldReturnYesPercent = ratings.isEmpty()
                ? null
                : BigDecimal.valueOf(wouldReturnYes * 100.0 / ratings.size())
                        .setScale(0, RoundingMode.HALF_UP);

        return new TripComparisonTripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getCountry(),
                trip.getCountryCode(),
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                trip.getCoverImageUrl(),
                travelDays,
                tripStopRepository.countByTripId(trip.getId()),
                photoRepository.countByTripId(trip.getId()),
                ratings.size(),
                average(ratings, TripRating::getOverallScore),
                wouldReturnYesPercent,
                new TripRatingBreakdownResponse(
                        average(ratings, TripRating::getFood),
                        average(ratings, TripRating::getNightlife),
                        average(ratings, TripRating::getCulture),
                        average(ratings, TripRating::getNature),
                        average(ratings, TripRating::getWalkability),
                        average(ratings, TripRating::getValueForMoney),
                        average(ratings, TripRating::getCrowds),
                        average(ratings, TripRating::getRelaxation)),
                spending(trip, travelDays));
    }

    private List<TripComparisonSpendingResponse> spending(Trip trip, long travelDays) {
        Map<String, BigDecimal> totals = expenseRepository.findByTripId(trip.getId()).stream()
                .collect(Collectors.groupingBy(
                        Expense::getCurrency,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
        return totals.entrySet().stream()
                .map(entry -> new TripComparisonSpendingResponse(
                        entry.getKey(),
                        entry.getValue().setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().divide(BigDecimal.valueOf(travelDays), 2, RoundingMode.HALF_UP)))
                .toList();
    }

    private BigDecimal average(List<TripRating> ratings, ToIntFunction<TripRating> value) {
        if (ratings.isEmpty()) {
            return null;
        }
        return BigDecimal.valueOf(ratings.stream().mapToInt(value).average().orElseThrow())
                .setScale(1, RoundingMode.HALF_UP);
    }
}
