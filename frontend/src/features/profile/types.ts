export interface TravelStatistics {
  countriesVisited: number;
  citiesVisited: number;
  trips: number;
  completedTrips: number;
  placesVisited: number;
  travelDays: number;
}

export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  createdAt: string;
  updatedAt: string;
  statistics: TravelStatistics;
}

export interface UpdateUserProfileInput {
  displayName: string;
}
