import type { TripStatus } from '../trips/types';

export type WorldMapCountryStatus = 'VISITED' | 'PLANNED';

export interface WorldMapSpending {
  currency: string;
  totalSpent: number;
}

export interface WorldMapTrip {
  id: string;
  title: string;
  city: string;
  startDate: string;
  endDate: string;
  status: TripStatus;
}

export interface WorldMapCountry {
  countryCode: string;
  name: string;
  status: WorldMapCountryStatus;
  tripCount: number;
  visitedTripCount: number;
  plannedTripCount: number;
  cityCount: number;
  placeCount: number;
  photoCount: number;
  spending: WorldMapSpending[];
  trips: WorldMapTrip[];
}

export interface WorldMapData {
  countriesVisited: number;
  countriesPlanned: number;
  countriesTotal: number;
  countries: WorldMapCountry[];
}
