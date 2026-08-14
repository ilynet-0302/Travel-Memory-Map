import type { ReplayRouteSegment, ReplayRouteSource } from '../replay/types';
import type { TripStatus, TripStop } from '../trips/types';

export interface PublicTripPhoto {
  id: string;
  tripStopId: string | null;
  signedUrl: string | null;
  caption: string | null;
  takenAt: string | null;
}

export interface PublicTripRatingBreakdown {
  food: number | null;
  nightlife: number | null;
  culture: number | null;
  nature: number | null;
  walkability: number | null;
  valueForMoney: number | null;
  crowds: number | null;
  relaxation: number | null;
}

export interface PublicTripRating {
  averageScore: number | null;
  ratingCount: number;
  averages: PublicTripRatingBreakdown;
  returnIntent: { yes: number; maybe: number; no: number };
}

export interface PublicTrip {
  publicSlug: string;
  title: string;
  description: string | null;
  country: string;
  countryCode: string;
  city: string;
  startDate: string;
  endDate: string;
  status: TripStatus;
  coverImageUrl: string | null;
  statistics: {
    travelDays: number;
    placeCount: number;
    selectedPhotoCount: number;
    totalDistanceKm: number;
  };
  routeSource: ReplayRouteSource;
  routeSegments: ReplayRouteSegment[];
  stops: TripStop[];
  photos: PublicTripPhoto[];
  rating: PublicTripRating;
}
