import { apiClient } from '../../../services/apiClient';
import { photosApi } from '../../photos/api/photosApi';
import { tripsApi } from '../../trips/api/tripsApi';
import { haversineDistanceKm } from '../replayPlayback';
import type { TripReplay } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';

export const replayApi = {
  async get(tripId: string): Promise<TripReplay> {
    if (!demoMode) return apiClient<TripReplay>(`/trips/${tripId}/replay`);
    const [trip, photos] = await Promise.all([tripsApi.getById(tripId), photosApi.list(tripId)]);
    return {
      tripId,
      title: trip.title,
      startDate: trip.startDate,
      endDate: trip.endDate,
      totalDistanceKm: haversineDistanceKm(trip.stops.map(({ coordinates }) => coordinates)),
      estimatedDurationSeconds: Math.max(0, trip.stops.length - 1) * 3,
      travelDurationSeconds: 0,
      routeSource: trip.stops.length > 1 ? 'DIRECT_FALLBACK' : 'NO_ROUTE',
      routeProfile: 'driving',
      routeSegments: trip.stops.slice(1).map((stop, sequence) => {
        const previous = trip.stops[sequence];
        return {
          sequence,
          fromStopId: previous.id,
          toStopId: stop.id,
          distanceKm: haversineDistanceKm([previous.coordinates, stop.coordinates]),
          durationSeconds: 0,
          followsRoads: false,
          coordinates: [previous.coordinates, stop.coordinates].map(([longitude, latitude]) => ({
            longitude,
            latitude,
          })),
        };
      }),
      frames: trip.stops.map((stop, sequence) => ({
        sequence,
        day: stop.day,
        stopId: stop.id,
        name: stop.name,
        description: stop.description || null,
        category: stop.category,
        latitude: stop.coordinates[1],
        longitude: stop.coordinates[0],
        arrivalTime: stop.arrivalAt ?? `${trip.startDate}T${stop.arrivalTime}:00Z`,
        photos: photos
          .filter(({ tripStopId }) => tripStopId === stop.id)
          .map((photo) => ({
            id: photo.id,
            signedUrl: photo.signedUrl,
            caption: photo.caption,
            takenAt: photo.takenAt,
          })),
      })),
    };
  },
};
