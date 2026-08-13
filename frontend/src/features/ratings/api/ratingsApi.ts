import { apiClient } from '../../../services/apiClient';
import type { TripRatingSummary } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';
const demoBaseScores = [8, 9, 9, 10];
const demoCurrentScores = new Map<string, number>();

const wait = (milliseconds = 120) =>
  new Promise<void>((resolve) => globalThis.setTimeout(resolve, milliseconds));

function demoSummary(tripId: string): TripRatingSummary {
  const currentUserScore = demoCurrentScores.get(tripId) ?? null;
  const scores = currentUserScore == null ? demoBaseScores : [...demoBaseScores, currentUserScore];
  const averageScore = Math.round((scores.reduce((sum, score) => sum + score, 0) / scores.length) * 10) / 10;
  return { averageScore, ratingCount: scores.length, currentUserScore, canRate: true };
}

export const ratingsApi = {
  async summary(tripId: string): Promise<TripRatingSummary> {
    if (!demoMode) return apiClient<TripRatingSummary>(`/trips/${tripId}/rating`);
    await wait();
    return demoSummary(tripId);
  },

  async rate(tripId: string, score: number): Promise<TripRatingSummary> {
    if (!demoMode) {
      return apiClient<TripRatingSummary>(`/trips/${tripId}/rating`, {
        method: 'PUT',
        body: JSON.stringify({ score }),
      });
    }
    await wait(160);
    demoCurrentScores.set(tripId, score);
    return demoSummary(tripId);
  },

  async remove(tripId: string): Promise<void> {
    if (!demoMode) {
      await apiClient<void>(`/trips/${tripId}/rating`, { method: 'DELETE' });
      return;
    }
    await wait();
    demoCurrentScores.delete(tripId);
  },
};
