import { ApiError, apiClient } from '../../../services/apiClient';
import { tripsApi } from '../../trips/api/tripsApi';
import type { StopCategory, Trip } from '../../trips/types';
import type { PublicTrip } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';

interface BackendPublicStop {
  id: string;
  name: string;
  description: string | null;
  latitude: number;
  longitude: number;
  arrivalTime: string;
  departureTime: string | null;
  category: StopCategory;
  rating: number | null;
  position: number;
}

interface BackendPublicTrip extends Omit<PublicTrip, 'stops'> {
  stops: BackendPublicStop[];
}

function mapStop(stop: BackendPublicStop, startDate: string) {
  const start = new Date(`${startDate}T00:00:00Z`);
  const arrivalDate = new Date(`${stop.arrivalTime.slice(0, 10)}T00:00:00Z`);
  const arrival = new Date(stop.arrivalTime);
  return {
    id: stop.id,
    name: stop.name,
    description: stop.description ?? '',
    coordinates: [Number(stop.longitude), Number(stop.latitude)] as [number, number],
    arrivalTime: arrival.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
    arrivalAt: stop.arrivalTime,
    departureAt: stop.departureTime ?? undefined,
    dateLabel: arrival.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }),
    day: Math.floor((arrivalDate.getTime() - start.getTime()) / 86_400_000) + 1,
    category: stop.category,
    rating: stop.rating ?? undefined,
    position: stop.position,
  };
}

function mapBackendTrip(trip: BackendPublicTrip): PublicTrip {
  return {
    ...trip,
    stops: trip.stops.map((stop) => mapStop(stop, trip.startDate)),
  };
}

function demoPublicTrip(trip: Trip): PublicTrip {
  return {
    publicSlug: trip.publicSlug ?? '',
    title: trip.title,
    description: trip.description,
    country: trip.country,
    countryCode: trip.countryCode,
    city: trip.city,
    startDate: trip.startDate,
    endDate: trip.endDate,
    status: trip.status,
    coverImageUrl: null,
    statistics: {
      travelDays: Math.floor((new Date(trip.endDate).getTime() - new Date(trip.startDate).getTime()) / 86_400_000) + 1,
      placeCount: trip.stops.length,
      selectedPhotoCount: 0,
      totalDistanceKm: 0,
    },
    routeSource: trip.stops.length > 1 ? 'DIRECT_FALLBACK' : 'NO_ROUTE',
    routeSegments: trip.stops.slice(1).map((stop, index) => ({
      sequence: index,
      fromStopId: trip.stops[index].id,
      toStopId: stop.id,
      distanceKm: 0,
      durationSeconds: 0,
      followsRoads: false,
      coordinates: [
        { longitude: trip.stops[index].coordinates[0], latitude: trip.stops[index].coordinates[1] },
        { longitude: stop.coordinates[0], latitude: stop.coordinates[1] },
      ],
    })),
    stops: trip.stops,
    photos: [],
    rating: {
      averageScore: 8.8,
      ratingCount: 4,
      averages: { food: 9, nightlife: 7, culture: 9, nature: 9, walkability: 8, valueForMoney: 8, crowds: 7, relaxation: 10 },
      returnIntent: { yes: 3, maybe: 1, no: 0 },
    },
  };
}

export const publicTripApi = {
  async get(publicSlug: string): Promise<PublicTrip> {
    if (!demoMode) return mapBackendTrip(await apiClient<BackendPublicTrip>(`/public/trips/${publicSlug}`));
    const trip = (await tripsApi.list()).find((candidate) =>
      candidate.visibility === 'PUBLIC' && candidate.publicSlug === publicSlug);
    if (!trip) throw new ApiError('This public trip does not exist.', 404, 'PUBLIC_TRIP_NOT_FOUND');
    return demoPublicTrip(trip);
  },
};
