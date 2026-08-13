export type TripStatus = 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED';
export type TripVisibility = 'PRIVATE' | 'PUBLIC';
export type TripRole = 'OWNER' | 'EDITOR' | 'VIEWER';

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
  departureAt?: string;
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
  currentUserRole: TripRole;
  accent: string;
  photos: number;
  spent: number;
  currency: string;
  memberCount: number;
  progress?: number;
  stops: TripStop[];
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
