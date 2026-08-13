import { apiClient } from '../../../services/apiClient';
import type { DetailedTripRating, TripRatingInput, TripRatingSummary } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';
const scoreFields = [
  'food', 'nightlife', 'culture', 'nature', 'walkability', 'valueForMoney', 'crowds', 'relaxation',
] as const;

const demoBaseRatings: TripRatingInput[] = [
  { food: 9, nightlife: 7, culture: 10, nature: 8, walkability: 9, valueForMoney: 8, crowds: 7, relaxation: 8, wouldReturn: 'YES' },
  { food: 8, nightlife: 8, culture: 9, nature: 9, walkability: 8, valueForMoney: 9, crowds: 6, relaxation: 9, wouldReturn: 'YES' },
  { food: 9, nightlife: 9, culture: 9, nature: 7, walkability: 10, valueForMoney: 8, crowds: 7, relaxation: 8, wouldReturn: 'YES' },
  { food: 10, nightlife: 8, culture: 10, nature: 9, walkability: 9, valueForMoney: 9, crowds: 8, relaxation: 9, wouldReturn: 'MAYBE' },
];
const demoCurrentRatings = new Map<string, TripRatingInput>();

const wait = (milliseconds = 120) =>
  new Promise<void>((resolve) => globalThis.setTimeout(resolve, milliseconds));

function demoSummary(tripId: string): TripRatingSummary {
  const current = demoCurrentRatings.get(tripId) ?? null;
  const ratings = current == null ? demoBaseRatings : [...demoBaseRatings, current];
  const overall = (rating: TripRatingInput) =>
    Math.round(scoreFields.reduce((sum, field) => sum + rating[field], 0) / scoreFields.length);
  const average = (values: number[]) =>
    Math.round((values.reduce((sum, value) => sum + value, 0) / values.length) * 10) / 10;
  const currentUserRating: DetailedTripRating | null = current == null
    ? null
    : { ...current, overallScore: overall(current) };

  return {
    averageScore: average(ratings.map(overall)),
    ratingCount: ratings.length,
    currentUserRating,
    averages: Object.fromEntries(
      scoreFields.map((field) => [field, average(ratings.map((rating) => rating[field]))]),
    ) as TripRatingSummary['averages'],
    returnIntent: {
      yes: ratings.filter((rating) => rating.wouldReturn === 'YES').length,
      maybe: ratings.filter((rating) => rating.wouldReturn === 'MAYBE').length,
      no: ratings.filter((rating) => rating.wouldReturn === 'NO').length,
    },
    canRate: true,
  };
}

export const ratingsApi = {
  async summary(tripId: string): Promise<TripRatingSummary> {
    if (!demoMode) return apiClient<TripRatingSummary>(`/trips/${tripId}/rating`);
    await wait();
    return demoSummary(tripId);
  },

  async rate(tripId: string, rating: TripRatingInput): Promise<TripRatingSummary> {
    if (!demoMode) {
      return apiClient<TripRatingSummary>(`/trips/${tripId}/rating`, {
        method: 'PUT',
        body: JSON.stringify(rating),
      });
    }
    await wait(160);
    demoCurrentRatings.set(tripId, rating);
    return demoSummary(tripId);
  },

  async remove(tripId: string): Promise<void> {
    if (!demoMode) {
      await apiClient<void>(`/trips/${tripId}/rating`, { method: 'DELETE' });
      return;
    }
    await wait();
    demoCurrentRatings.delete(tripId);
  },
};
