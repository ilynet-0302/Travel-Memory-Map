import { apiClient } from '../../../services/apiClient';
import { demoTrips } from '../data/demoTrips';
import type { CreateTripInput, Trip, TripStop, TripStopInput, UpdateTripInput } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';
let demoStore = [...demoTrips];

interface BackendStop {
  id: string;
  name: string;
  description: string | null;
  latitude: number;
  longitude: number;
  arrivalTime: string;
  departureTime?: string | null;
  category: Trip['stops'][number]['category'];
  rating?: number;
  position: number;
}

interface BackendTrip {
  id: string;
  title: string;
  description: string | null;
  country: string;
  countryCode: string;
  city: string;
  startDate: string;
  endDate: string;
  status: Trip['status'];
  visibility: Trip['visibility'];
  currentUserRole?: Trip['currentUserRole'];
  memberCount: number;
  photoCount?: number;
  stops?: BackendStop[];
}

const wait = (milliseconds: number) =>
  new Promise<void>((resolve) => globalThis.setTimeout(resolve, milliseconds));

function mapBackendStop(stop: BackendStop, tripStartDate: string): TripStop {
  const tripStart = new Date(`${tripStartDate}T00:00:00Z`);
  const arrival = new Date(stop.arrivalTime);
  const arrivalDate = new Date(`${stop.arrivalTime.slice(0, 10)}T00:00:00Z`);
  const day = Math.floor((arrivalDate.getTime() - tripStart.getTime()) / 86_400_000) + 1;
  return {
    id: stop.id,
    name: stop.name,
    description: stop.description ?? '',
    coordinates: [Number(stop.longitude), Number(stop.latitude)],
    arrivalTime: arrival.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
    arrivalAt: stop.arrivalTime,
    departureAt: stop.departureTime ?? undefined,
    dateLabel: arrival.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }),
    day,
    category: stop.category,
    rating: stop.rating,
    position: stop.position,
  };
}

function mapBackendTrip(trip: BackendTrip): Trip {
  return {
    ...trip,
    description: trip.description ?? 'A journey waiting to be filled with stories.',
    currentUserRole: trip.currentUserRole ?? 'VIEWER',
    accent: ['terracotta', 'indigo', 'aqua', 'sage'][trip.city.length % 4],
    photos: trip.photoCount ?? 0,
    spent: 0,
    currency: 'EUR',
    stops: (trip.stops ?? [])
      .sort((left, right) => left.position - right.position)
      .map((stop) => mapBackendStop(stop, trip.startDate)),
  };
}

function requireDemoTrip(tripId: string) {
  const trip = demoStore.find(({ id }) => id === tripId);
  if (!trip) throw new Error('We could not find that trip.');
  return trip;
}

function toDemoStop(input: TripStopInput, tripStartDate: string, id: string): TripStop {
  return mapBackendStop(
    {
      id,
      name: input.name,
      description: input.description || null,
      latitude: input.latitude,
      longitude: input.longitude,
      arrivalTime: input.arrivalTime,
      departureTime: input.departureTime || null,
      category: input.category,
      rating: input.rating,
      position: input.position,
    },
    tripStartDate,
  );
}

export const tripsApi = {
  async list(): Promise<Trip[]> {
    if (!demoMode) {
      const trips = await apiClient<BackendTrip[]>('/trips');
      return trips.map(mapBackendTrip);
    }
    await wait(180);
    return [...demoStore];
  },

  async getById(tripId: string): Promise<Trip> {
    if (!demoMode) return mapBackendTrip(await apiClient<BackendTrip>(`/trips/${tripId}`));
    await wait(120);
    return requireDemoTrip(tripId);
  },

  async create(input: CreateTripInput): Promise<Trip> {
    if (!demoMode) {
      const createdTrip = await apiClient<BackendTrip>('/trips', {
        method: 'POST',
        body: JSON.stringify(input),
      });
      return mapBackendTrip(createdTrip);
    }

    await wait(400);
    const trip: Trip = {
      ...input,
      id: `${input.city.toLowerCase().replace(/[^a-z0-9]+/g, '-')}-${Date.now()}`,
      description: 'A new journey waiting to be filled with stories.',
      countryCode: input.country.slice(0, 2).toUpperCase(),
      status: new Date(input.startDate) > new Date() ? 'UPCOMING' : 'ACTIVE',
      currentUserRole: 'OWNER',
      accent: 'sage',
      photos: 0,
      spent: 0,
      currency: 'EUR',
      memberCount: 1,
      progress: 8,
      stops: [],
    };
    demoStore = [trip, ...demoStore];
    return trip;
  },

  async update(tripId: string, input: UpdateTripInput): Promise<Trip> {
    if (!demoMode) {
      return mapBackendTrip(await apiClient<BackendTrip>(`/trips/${tripId}`, {
        method: 'PUT',
        body: JSON.stringify(input),
      }));
    }
    await wait(280);
    const current = requireDemoTrip(tripId);
    const updated: Trip = {
      ...current,
      ...input,
      countryCode: input.countryCode || input.country.slice(0, 2).toUpperCase(),
      status: new Date(input.startDate) > new Date()
        ? 'UPCOMING'
        : new Date(input.endDate) < new Date() ? 'COMPLETED' : 'ACTIVE',
    };
    demoStore = demoStore.map((trip) => trip.id === tripId ? updated : trip);
    return updated;
  },

  async archive(tripId: string): Promise<void> {
    if (!demoMode) {
      await apiClient<void>(`/trips/${tripId}/archive`, { method: 'PATCH' });
      return;
    }
    await wait(220);
    requireDemoTrip(tripId);
    demoStore = demoStore.filter(({ id }) => id !== tripId);
  },

  async delete(tripId: string): Promise<void> {
    if (!demoMode) {
      await apiClient<void>(`/trips/${tripId}`, { method: 'DELETE' });
      return;
    }
    await wait(220);
    requireDemoTrip(tripId);
    demoStore = demoStore.filter(({ id }) => id !== tripId);
  },

  async createStop(tripId: string, tripStartDate: string, input: TripStopInput): Promise<TripStop> {
    if (!demoMode) {
      const stop = await apiClient<BackendStop>(`/trips/${tripId}/stops`, {
        method: 'POST',
        body: JSON.stringify(input),
      });
      return mapBackendStop(stop, tripStartDate);
    }
    await wait(260);
    const trip = requireDemoTrip(tripId);
    const stop = toDemoStop(input, trip.startDate, `stop-${crypto.randomUUID()}`);
    const stops = [...trip.stops, stop].sort((left, right) => (left.position ?? 0) - (right.position ?? 0));
    demoStore = demoStore.map((item) => item.id === tripId ? { ...item, stops } : item);
    return stop;
  },

  async updateStop(tripId: string, stopId: string, tripStartDate: string, input: TripStopInput): Promise<TripStop> {
    if (!demoMode) {
      const stop = await apiClient<BackendStop>(`/trips/${tripId}/stops/${stopId}`, {
        method: 'PUT',
        body: JSON.stringify(input),
      });
      return mapBackendStop(stop, tripStartDate);
    }
    await wait(240);
    const trip = requireDemoTrip(tripId);
    const stop = toDemoStop(input, trip.startDate, stopId);
    const stops = trip.stops
      .map((item) => item.id === stopId ? stop : item)
      .sort((left, right) => (left.position ?? 0) - (right.position ?? 0));
    demoStore = demoStore.map((item) => item.id === tripId ? { ...item, stops } : item);
    return stop;
  },

  async deleteStop(tripId: string, stopId: string): Promise<void> {
    if (!demoMode) {
      await apiClient<void>(`/trips/${tripId}/stops/${stopId}`, { method: 'DELETE' });
      return;
    }
    await wait(200);
    const trip = requireDemoTrip(tripId);
    const stops = trip.stops.filter(({ id }) => id !== stopId);
    demoStore = demoStore.map((item) => item.id === tripId ? { ...item, stops } : item);
  },
};
