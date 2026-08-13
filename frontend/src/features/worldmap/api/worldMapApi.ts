import { apiClient } from '../../../services/apiClient';
import { tripsApi } from '../../trips/api/tripsApi';
import type { Trip } from '../../trips/types';
import type { WorldMapCountry, WorldMapData } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';

function demoCountry(countryTrips: Trip[]): WorldMapCountry {
  const ordered = [...countryTrips].sort((left, right) => right.startDate.localeCompare(left.startDate));
  const visited = ordered.filter((trip) => trip.status !== 'UPCOMING');
  const spendingByCurrency = new Map<string, number>();
  ordered.forEach((trip) => spendingByCurrency.set(
    trip.currency,
    (spendingByCurrency.get(trip.currency) ?? 0) + trip.spent,
  ));
  return {
    countryCode: ordered[0].countryCode,
    name: ordered[0].country,
    status: visited.length ? 'VISITED' : 'PLANNED',
    tripCount: ordered.length,
    visitedTripCount: visited.length,
    plannedTripCount: ordered.length - visited.length,
    cityCount: new Set(ordered.map((trip) => trip.city.toLowerCase())).size,
    placeCount: ordered.reduce((total, trip) => total + trip.stops.length, 0),
    photoCount: ordered.reduce((total, trip) => total + trip.photos, 0),
    spending: [...spendingByCurrency.entries()]
      .filter(([, totalSpent]) => totalSpent > 0)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([currency, totalSpent]) => ({ currency, totalSpent })),
    trips: ordered.map((trip) => ({
      id: trip.id,
      title: trip.title,
      city: trip.city,
      startDate: trip.startDate,
      endDate: trip.endDate,
      status: trip.status,
    })),
  };
}

async function demoWorldMap(): Promise<WorldMapData> {
  const trips = await tripsApi.list();
  const groups = new Map<string, Trip[]>();
  trips.forEach((trip) => groups.set(trip.countryCode, [...(groups.get(trip.countryCode) ?? []), trip]));
  const countries = [...groups.values()]
    .map((countryTrips) => demoCountry(countryTrips))
    .sort((left, right) =>
      Number(left.status === 'PLANNED') - Number(right.status === 'PLANNED')
      || left.name.localeCompare(right.name));
  return {
    countriesVisited: countries.filter((country) => country.status === 'VISITED').length,
    countriesPlanned: countries.filter((country) => country.status === 'PLANNED').length,
    countriesTotal: 195,
    countries,
  };
}

export const worldMapApi = {
  async get(): Promise<WorldMapData> {
    if (!demoMode) return apiClient<WorldMapData>('/profile/world-map');
    return demoWorldMap();
  },
};
