import { apiClient } from '../../../services/apiClient';
import { photosApi } from '../../photos/api/photosApi';
import { tripsApi } from '../../trips/api/tripsApi';
import type { MemoryPhoto } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';

async function demoMemories(): Promise<MemoryPhoto[]> {
  const trips = await tripsApi.list();
  const photosByTrip = await Promise.all(trips.map(async (trip) => ({ trip, photos: await photosApi.list(trip.id) })));
  return photosByTrip.flatMap(({ trip, photos }) => photos.map((photo) => ({
    ...photo,
    tripTitle: trip.title,
    country: trip.country,
    countryCode: trip.countryCode,
    city: trip.city,
    tripStartDate: trip.startDate,
    tripEndDate: trip.endDate,
    tripStopName: trip.stops.find(({ id }) => id === photo.tripStopId)?.name ?? null,
  }))).sort((left, right) =>
    (right.takenAt ?? right.createdAt).localeCompare(left.takenAt ?? left.createdAt));
}

export const memoryGalleryApi = {
  async get(): Promise<MemoryPhoto[]> {
    if (!demoMode) return apiClient<MemoryPhoto[]>('/profile/memories');
    return demoMemories();
  },
};
