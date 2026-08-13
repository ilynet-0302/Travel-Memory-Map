import type { TravelPersonality } from '../dna/types';

export interface LocationStatistic {
  name: string;
  tripCount: number;
  averageRating: number | null;
}

export interface TripStatistic {
  tripId: string;
  title: string;
  country: string;
  city: string;
  startDate: string;
  endDate: string;
  travelDays: number;
}

export interface SpendingTrip {
  tripId: string;
  title: string;
  amount: number;
}

export interface TravelSpending {
  currency: string;
  totalSpent: number;
  averageCostPerTrip: number;
  averageCostPerDay: number;
  mostExpensiveTrip: SpendingTrip;
  cheapestTrip: SpendingTrip;
}

export interface TravelStatistics {
  countriesVisited: number;
  citiesVisited: number;
  trips: number;
  completedTrips: number;
  placesVisited: number;
  photosUploaded: number;
  travelDays: number;
  spending: TravelSpending[];
  favouriteCountry: LocationStatistic | null;
  favouriteCity: LocationStatistic | null;
  mostVisitedCountry: LocationStatistic | null;
  longestTrip: TripStatistic | null;
  shortestTrip: TripStatistic | null;
}

export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  createdAt: string;
  updatedAt: string;
  statistics: TravelStatistics;
  personality: TravelPersonality;
}

export interface UpdateUserProfileInput {
  displayName: string;
}
