export type WouldReturn = 'YES' | 'MAYBE' | 'NO';

export interface TripRatingInput {
  food: number;
  nightlife: number;
  culture: number;
  nature: number;
  walkability: number;
  valueForMoney: number;
  crowds: number;
  relaxation: number;
  wouldReturn: WouldReturn;
}

export interface DetailedTripRating extends TripRatingInput {
  overallScore: number;
}

export type RatingAverages = Record<Exclude<keyof TripRatingInput, 'wouldReturn'>, number | null>;

export interface ReturnIntentSummary {
  yes: number;
  maybe: number;
  no: number;
}

export interface TripRatingSummary {
  averageScore: number | null;
  ratingCount: number;
  currentUserRating: DetailedTripRating | null;
  averages: RatingAverages;
  returnIntent: ReturnIntentSummary;
  canRate: boolean;
}
