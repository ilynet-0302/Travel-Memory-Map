package com.travelmemory.memory.service;

import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.PhotoStorageException;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.memory.dto.OnThisDayMemoryResponse;
import com.travelmemory.memory.dto.OnThisDayResponse;
import com.travelmemory.memory.dto.OnThisDaySpendingResponse;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.photo.service.PhotoStorageService;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OnThisDayService {

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final PhotoRepository photoRepository;
    private final ExpenseRepository expenseRepository;
    private final PhotoStorageService photoStorageService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final Clock clock;

    public OnThisDayService(
            TripRepository tripRepository,
            TripStopRepository tripStopRepository,
            PhotoRepository photoRepository,
            ExpenseRepository expenseRepository,
            PhotoStorageService photoStorageService,
            AuthenticatedUserProvider authenticatedUserProvider,
            Clock clock) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.photoRepository = photoRepository;
        this.expenseRepository = expenseRepository;
        this.photoStorageService = photoStorageService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OnThisDayResponse getCurrentUsersMemories() {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        LocalDate today = LocalDate.now(clock);
        List<OnThisDayMemoryResponse> memories = tripRepository.findAccessibleTrips(userId).stream()
                .map(trip -> memoryDate(trip, today).map(date -> memory(trip, date, today)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(OnThisDayMemoryResponse::yearsAgo)
                        .thenComparing(OnThisDayMemoryResponse::tripTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new OnThisDayResponse(today, memories);
    }

    private Optional<LocalDate> memoryDate(Trip trip, LocalDate today) {
        MonthDay todayInCalendar = MonthDay.from(today);
        return trip.getStartDate()
                .datesUntil(trip.getEndDate().plusDays(1))
                .filter(date -> date.isBefore(today))
                .filter(date -> MonthDay.from(date).equals(todayInCalendar))
                .reduce((first, second) -> second);
    }

    private OnThisDayMemoryResponse memory(Trip trip, LocalDate memoryDate, LocalDate today) {
        List<TripStop> stops = tripStopRepository.findByTripIdOrderByPositionAsc(trip.getId()).stream()
                .filter(stop -> happenedOn(stop, memoryDate))
                .toList();
        List<Photo> photos = photoRepository.findByTripIdOrderByTakenAtAscCreatedAtAsc(trip.getId()).stream()
                .filter(photo -> happenedOn(photo, memoryDate))
                .toList();
        List<Expense> expenses = expenseRepository.findByTripId(trip.getId()).stream()
                .filter(expense -> expense.getDate().equals(memoryDate))
                .toList();
        Photo heroPhoto = photos.isEmpty() ? null : photos.getFirst();
        return new OnThisDayMemoryResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getCountry(),
                trip.getCountryCode(),
                trip.getCity(),
                memoryDate,
                today.getYear() - memoryDate.getYear(),
                stops.size(),
                photos.size(),
                spending(expenses),
                heroPhoto == null ? null : signedUrl(heroPhoto),
                heroPhoto == null ? null : heroPhoto.getCaption(),
                stops.stream().map(TripStop::getName).distinct().toList());
    }

    private boolean happenedOn(TripStop stop, LocalDate date) {
        LocalDate arrivalDate = stop.getArrivalTime().toLocalDate();
        LocalDate departureDate = stop.getDepartureTime() == null
                ? arrivalDate
                : stop.getDepartureTime().toLocalDate();
        return !date.isBefore(arrivalDate) && !date.isAfter(departureDate);
    }

    private boolean happenedOn(Photo photo, LocalDate date) {
        if (photo.getTakenAt() != null) return photo.getTakenAt().toLocalDate().equals(date);
        return photo.getTripStop() != null && happenedOn(photo.getTripStop(), date);
    }

    private List<OnThisDaySpendingResponse> spending(List<Expense> expenses) {
        Map<String, BigDecimal> totals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCurrency,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
        return totals.entrySet().stream()
                .map(entry -> new OnThisDaySpendingResponse(
                        entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private String signedUrl(Photo photo) {
        try {
            return photoStorageService.createSignedUrl(
                    photo.getStoragePath(), authenticatedUserProvider.getCurrentAccessToken());
        } catch (PhotoStorageException exception) {
            return null;
        }
    }
}
