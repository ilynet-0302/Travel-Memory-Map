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
    travelDays: 19,
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
