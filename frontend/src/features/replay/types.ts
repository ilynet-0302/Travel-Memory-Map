import type { StopCategory } from '../trips/types';

export interface ReplayPhoto {
  id: string;
  signedUrl: string;
  caption: string | null;
  takenAt: string | null;
}

export interface ReplayFrame {
  sequence: number;
  day: number;
  stopId: string;
  name: string;
  description: string | null;
  category: StopCategory;
  latitude: number;
  longitude: number;
  arrivalTime: string;
  photos: ReplayPhoto[];
}

export type ReplayRouteSource = 'ROUTED' | 'DIRECT_FALLBACK' | 'NO_ROUTE';

export interface ReplayCoordinate {
  longitude: number;
  latitude: number;
}

export interface ReplayRouteSegment {
  sequence: number;
  fromStopId: string;
  toStopId: string;
  distanceKm: number;
  durationSeconds: number;
  followsRoads: boolean;
  coordinates: ReplayCoordinate[];
}

export interface TripReplay {
  tripId: string;
  title: string;
  startDate: string;
  endDate: string;
  totalDistanceKm: number;
  estimatedDurationSeconds: number;
  travelDurationSeconds: number;
  routeSource: ReplayRouteSource;
  routeProfile: string;
  routeSegments: ReplayRouteSegment[];
  frames: ReplayFrame[];
}
