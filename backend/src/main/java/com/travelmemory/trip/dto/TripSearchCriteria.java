package com.travelmemory.trip.dto;

import com.travelmemory.trip.entity.TripStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Locale;

public record TripSearchCriteria(
        @Size(max = 120) String q,
        TripStatus status,
        @Min(1900) @Max(2200) Integer year,
        @Size(max = 100) String country,
        @Size(max = 100) String city,
        @DecimalMin("1.0") @DecimalMax("10.0") BigDecimal minRating,
        @DecimalMin("0.0") BigDecimal minPrice,
        @DecimalMin("0.0") BigDecimal maxPrice,
        @Min(1) @Max(3650) Integer minDuration,
        @Min(1) @Max(3650) Integer maxDuration,
        @Pattern(regexp = "EXPLORER|FOODIE|CULTURE|NIGHTLIFE|RELAXATION|NATURE|ADVENTURE") String dna,
        TripRelationship relationship,
        TripSearchSort sort,
        @Pattern(regexp = "[A-Za-z]{3}") String currency) {

    public TripSearchCriteria {
        q = clean(q);
        country = clean(country);
        city = clean(city);
        dna = clean(dna);
        relationship = relationship == null ? TripRelationship.ALL : relationship;
        sort = sort == null ? TripSearchSort.START_DESC : sort;
        currency = currency == null || currency.isBlank() ? "EUR" : currency.toUpperCase(Locale.ROOT);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
