import { apiClient } from '../../../services/apiClient';
import { tripsApi } from '../../trips/api/tripsApi';
import type { Trip } from '../../trips/types';
import type { TripComparison, TripComparisonSide } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';
const ratingFields = [
  'food', 'nightlife', 'culture', 'nature', 'walkability', 'valueForMoney', 'crowds', 'relaxation',
] as const;

function demoScore(seed: string, offset: number) {
  const hash = [...seed].reduce((sum, letter) => sum + letter.charCodeAt(0), 0);
  return 6 + ((hash + offset * 7) % 5);
}

function demoSide(trip: Trip): TripComparisonSide {
  const start = new Date(`${trip.startDate}T00:00:00Z`).getTime();
  const end = new Date(`${trip.endDate}T00:00:00Z`).getTime();
  const ratings = Object.fromEntries(
    ratingFields.map((field, index) => [field, demoScore(trip.id, index)]),
  ) as TripComparisonSide['ratings'];
  const ratingValues = Object.values(ratings).filter((value): value is number => value != null);

  return {
    id: trip.id,
    title: trip.title,
    country: trip.country,
    countryCode: trip.countryCode,
    city: trip.city,
    startDate: trip.startDate,
    endDate: trip.endDate,
    status: trip.status,
    coverImageUrl: null,
    travelDays: Math.floor((end - start) / 86_400_000) + 1,
    stopCount: trip.stops.length,
    photoCount: trip.photos,
    ratingCount: 4,
    averageScore: Math.round((ratingValues.reduce((sum, value) => sum + value, 0) / ratingValues.length) * 10) / 10,
    wouldReturnYesPercent: 75,
    ratings,
    spending: trip.spent > 0
      ? [{ currency: trip.currency, total: trip.spent, costPerDay: Math.round((trip.spent / (Math.floor((end - start) / 86_400_000) + 1)) * 100) / 100 }]
      : [],
  };
}

export const comparisonApi = {
  async compare(leftTripId: string, rightTripId: string): Promise<TripComparison> {
    if (!demoMode) {
      const query = new URLSearchParams({ leftTripId, rightTripId });
      return apiClient<TripComparison>(`/trips/comparison?${query}`);
    }
    const [left, right] = await Promise.all([
      tripsApi.getById(leftTripId),
      tripsApi.getById(rightTripId),
    ]);
    return { left: demoSide(left), right: demoSide(right) };
  },
};
