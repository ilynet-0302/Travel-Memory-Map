package com.travelmemory.rating.dto;

import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.entity.WouldReturn;

public record TripRatingResponse(
        int overallScore,
        int food,
        int nightlife,
        int culture,
        int nature,
        int walkability,
        int valueForMoney,
        int crowds,
        int relaxation,
        WouldReturn wouldReturn) {

    public static TripRatingResponse from(TripRating rating) {
        return new TripRatingResponse(
                rating.getOverallScore(),
                rating.getFood(),
                rating.getNightlife(),
                rating.getCulture(),
                rating.getNature(),
                rating.getWalkability(),
                rating.getValueForMoney(),
                rating.getCrowds(),
                rating.getRelaxation(),
                rating.getWouldReturn());
    }
}
