package com.travelmemory.publictrip.service;

import com.travelmemory.exception.PhotoStorageException;
import com.travelmemory.exception.PublicTripNotFoundException;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.photo.service.PhotoStorageService;
import com.travelmemory.publictrip.dto.PublicTripPhotoResponse;
import com.travelmemory.publictrip.dto.PublicTripRatingResponse;
import com.travelmemory.publictrip.dto.PublicTripResponse;
import com.travelmemory.publictrip.dto.PublicTripStatisticsResponse;
import com.travelmemory.publictrip.dto.PublicTripStopResponse;
import com.travelmemory.rating.dto.ReturnIntentSummaryResponse;
import com.travelmemory.rating.dto.TripRatingBreakdownResponse;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.entity.WouldReturn;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.replay.service.RoadRouteResult;
import com.travelmemory.replay.service.RoadRouteService;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStatus;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.ToIntFunction;

@Service
public class PublicTripService {

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final PhotoRepository photoRepository;
    private final TripRatingRepository tripRatingRepository;
    private final PhotoStorageService photoStorageService;
    private final RoadRouteService roadRouteService;

    public PublicTripService(
            TripRepository tripRepository,
            TripStopRepository tripStopRepository,
            PhotoRepository photoRepository,
            TripRatingRepository tripRatingRepository,
            PhotoStorageService photoStorageService,
            RoadRouteService roadRouteService) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.photoRepository = photoRepository;
        this.tripRatingRepository = tripRatingRepository;
        this.photoStorageService = photoStorageService;
        this.roadRouteService = roadRouteService;
    }

    @Transactional(readOnly = true)
    public PublicTripResponse getBySlug(String publicSlug) {
        if (publicSlug == null || publicSlug.isBlank() || publicSlug.length() > 140) {
            throw new PublicTripNotFoundException();
        }
        Trip trip = tripRepository.findByPublicSlugAndVisibilityAndStatusNot(
                        publicSlug, TripVisibility.PUBLIC, TripStatus.ARCHIVED)
                .orElseThrow(PublicTripNotFoundException::new);
        List<TripStop> stops = tripStopRepository.findByTripIdOrderByPositionAsc(trip.getId());
        List<Photo> photos = photoRepository.findByTripIdAndPublicVisibleTrueOrderByTakenAtAscCreatedAtAsc(trip.getId());
        RoadRouteResult route = roadRouteService.calculate(stops);
        return new PublicTripResponse(
                trip.getPublicSlug(),
                trip.getTitle(),
                trip.getDescription(),
                trip.getCountry(),
                trip.getCountryCode(),
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                trip.getCoverImageUrl(),
                new PublicTripStatisticsResponse(
                        ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1,
                        stops.size(),
                        photos.size(),
                        route.totalDistanceKm()),
                route.source(),
                route.segments(),
                stops.stream().map(this::stop).toList(),
                photos.stream().map(this::photo).toList(),
                rating(trip));
    }

    private PublicTripStopResponse stop(TripStop stop) {
        return new PublicTripStopResponse(
                stop.getId(), stop.getName(), stop.getDescription(), stop.getLatitude(), stop.getLongitude(),
                stop.getArrivalTime(), stop.getArrivalLocalDateTime(),
                stop.getDepartureTime(), stop.getDepartureLocalDateTime(),
                stop.getCategory(), stop.getRating(), stop.getPosition());
    }

    private PublicTripPhotoResponse photo(Photo photo) {
        String signedUrl;
        try {
            signedUrl = photoStorageService.createPublicSignedUrl(photo.getStoragePath());
        } catch (PhotoStorageException exception) {
            signedUrl = null;
        }
        return new PublicTripPhotoResponse(
                photo.getId(),
                photo.getTripStop() == null ? null : photo.getTripStop().getId(),
                signedUrl,
                photo.getCaption(),
                photo.getTakenAt());
    }

    private PublicTripRatingResponse rating(Trip trip) {
        List<TripRating> ratings = tripRatingRepository.findByTripId(trip.getId());
        long yes = ratings.stream().filter(rating -> rating.getWouldReturn() == WouldReturn.YES).count();
        long maybe = ratings.stream().filter(rating -> rating.getWouldReturn() == WouldReturn.MAYBE).count();
        long no = ratings.stream().filter(rating -> rating.getWouldReturn() == WouldReturn.NO).count();
        return new PublicTripRatingResponse(
                average(ratings, TripRating::getOverallScore),
                ratings.size(),
                new TripRatingBreakdownResponse(
                        average(ratings, TripRating::getFood),
                        average(ratings, TripRating::getNightlife),
                        average(ratings, TripRating::getCulture),
                        average(ratings, TripRating::getNature),
                        average(ratings, TripRating::getWalkability),
                        average(ratings, TripRating::getValueForMoney),
                        average(ratings, TripRating::getCrowds),
                        average(ratings, TripRating::getRelaxation)),
                new ReturnIntentSummaryResponse(yes, maybe, no));
    }

    private BigDecimal average(List<TripRating> ratings, ToIntFunction<TripRating> value) {
        if (ratings.isEmpty()) return null;
        return BigDecimal.valueOf(ratings.stream().mapToInt(value).average().orElseThrow())
                .setScale(1, RoundingMode.HALF_UP);
    }
}
