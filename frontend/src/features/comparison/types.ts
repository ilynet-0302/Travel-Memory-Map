import type { RatingAverages } from '../ratings/types';
import type { TripStatus } from '../trips/types';

export interface TripComparisonSpending {
  currency: string;
  total: number;
  costPerDay: number;
}

export interface TripComparisonSide {
  id: string;
  title: string;
  country: string;
  countryCode: string;
  city: string;
  startDate: string;
  endDate: string;
  status: TripStatus;
  coverImageUrl: string | null;
  travelDays: number;
  stopCount: number;
  photoCount: number;
  ratingCount: number;
  averageScore: number | null;
  wouldReturnYesPercent: number | null;
  ratings: RatingAverages;
  spending: TripComparisonSpending[];
}

export interface TripComparison {
  left: TripComparisonSide;
  right: TripComparisonSide;
}
