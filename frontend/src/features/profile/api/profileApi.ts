import { apiClient } from '../../../services/apiClient';
import type { UpdateUserProfileInput, UserProfile } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';

let demoProfile: UserProfile = {
  id: 'demo-profile',
  email: 'iliya@example.com',
  displayName: 'Iliya Petrov',
  avatarUrl: null,
  createdAt: '2025-01-12T10:00:00Z',
  updatedAt: '2026-08-13T09:00:00Z',
  statistics: {
    countriesVisited: 3,
    citiesVisited: 3,
    trips: 3,
    completedTrips: 2,
    placesVisited: 10,
    photosUploaded: 0,
    travelDays: 19,
    spending: [{
      currency: 'EUR',
      totalSpent: 1460,
      averageCostPerTrip: 486.67,
      averageCostPerDay: 76.84,
      mostExpensiveTrip: { tripId: 'rome-2026', title: 'Roman Holiday', amount: 742 },
      cheapestTrip: { tripId: 'budapest-2026', title: 'Budapest Weekend', amount: 298 },
    }],
    favouriteCountry: { name: 'Italy', tripCount: 1, averageRating: 9.2 },
    favouriteCity: { name: 'Rome', tripCount: 1, averageRating: 9.2 },
    mostVisitedCountry: { name: 'Italy', tripCount: 1, averageRating: 9.2 },
    longestTrip: {
      tripId: 'corfu-2025', title: 'Corfu Escape', country: 'Greece', city: 'Corfu',
      startDate: '2025-08-17', endDate: '2025-08-23', travelDays: 7,
    },
    shortestTrip: {
      tripId: 'budapest-2026', title: 'Budapest Weekend', country: 'Hungary', city: 'Budapest',
      startDate: '2026-05-03', endDate: '2026-05-06', travelDays: 4,
    },
  },
  personality: {
    type: 'Still taking shape',
    description: 'Complete more journeys to reveal your travel personality.',
    revealed: false,
    completedTrips: 2,
    completedTripsRequired: 3,
    scores: { explorer: 82, foodie: 71, culture: 64, nightlife: 20, relaxation: 34, nature: 41, adventure: 57 },
  },
};

export const profileApi = {
  async get(): Promise<UserProfile> {
    if (!demoMode) return apiClient<UserProfile>('/profile');
    return demoProfile;
  },

  async update(input: UpdateUserProfileInput): Promise<UserProfile> {
    if (!demoMode) {
      return apiClient<UserProfile>('/profile', {
        method: 'PUT',
        body: JSON.stringify(input),
      });
    }
    demoProfile = {
      ...demoProfile,
      displayName: input.displayName.trim(),
      updatedAt: new Date().toISOString(),
    };
    return demoProfile;
  },
};
