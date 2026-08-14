export type TripStatus = 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED';
export type TripVisibility = 'PRIVATE' | 'PUBLIC';
export type TripRole = 'OWNER' | 'EDITOR' | 'VIEWER';
export type TripRelationship = 'ALL' | 'OWNER' | 'SHARED';
export type TripSort = 'START_DESC' | 'START_ASC' | 'TITLE_ASC' | 'RATING_DESC' | 'PRICE_DESC' | 'DURATION_DESC';
export type TripDnaTrait = 'EXPLORER' | 'FOODIE' | 'CULTURE' | 'NIGHTLIFE' | 'RELAXATION' | 'NATURE' | 'ADVENTURE';

export type StopCategory =
  | 'LANDMARK'
  | 'RESTAURANT'
  | 'HOTEL'
  | 'AIRPORT'
  | 'BEACH'
  | 'MUSEUM'
  | 'BAR'
  | 'SHOP'
  | 'NATURE'
  | 'TRANSPORT'
  | 'OTHER';

export interface TripStop {
  id: string;
  name: string;
  description: string;
  coordinates: [longitude: number, latitude: number];
  arrivalTime: string;
  arrivalAt?: string;
  arrivalLocalDateTime?: string;
  departureAt?: string;
  departureLocalDateTime?: string;
  dateLabel: string;
  day: number;
  category: StopCategory;
  rating?: number;
  position?: number;
}

export interface Trip {
  id: string;
  title: string;
  description: string;
  country: string;
  countryCode: string;
  city: string;
  startDate: string;
  endDate: string;
  status: TripStatus;
  visibility: TripVisibility;
  publicSlug: string | null;
  currentUserRole: TripRole;
  accent: string;
  photos: number;
  spent: number;
  currency: string;
  memberCount: number;
  progress?: number;
  stops: TripStop[];
  durationDays?: number;
  averageRating?: number;
  dominantTrait?: TripDnaTrait;
}

export interface TripSearchFilters {
  q?: string;
  status?: Exclude<TripStatus, 'ARCHIVED'>;
  year?: number;
  country?: string;
  city?: string;
  minRating?: number;
  minPrice?: number;
  maxPrice?: number;
  minDuration?: number;
  maxDuration?: number;
  dna?: TripDnaTrait;
  relationship?: TripRelationship;
  sort?: TripSort;
  currency?: string;
}

export interface CreateTripInput {
  title: string;
  description?: string;
  country: string;
  city: string;
  startDate: string;
  endDate: string;
  visibility: TripVisibility;
}

export interface UpdateTripInput extends CreateTripInput {
  description: string;
  countryCode?: string;
}

export interface TripStopInput {
  name: string;
  description: string;
  latitude: number;
  longitude: number;
  arrivalTime: string;
  departureTime?: string;
  category: StopCategory;
  rating?: number;
  position: number;
}

export interface ImportedMapPlace {
  name: string;
  latitude: number;
  longitude: number;
  resolvedUrl: string;
}
