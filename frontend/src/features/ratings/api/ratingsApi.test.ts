import { describe, expect, it } from 'vitest';
import { ratingsApi } from './ratingsApi';

describe('demo ratings API', () => {
  it('adds, replaces and removes the current traveller rating', async () => {
    const tripId = `trip-${crypto.randomUUID()}`;
    const firstRating = {
      food: 7,
      nightlife: 6,
      culture: 9,
      nature: 8,
      walkability: 7,
      valueForMoney: 8,
      crowds: 6,
      relaxation: 9,
      wouldReturn: 'YES' as const,
    };

    await expect(ratingsApi.rate(tripId, firstRating)).resolves.toMatchObject({
      currentUserRating: { overallScore: 8, culture: 9, wouldReturn: 'YES' },
      ratingCount: 5,
      returnIntent: { yes: 4, maybe: 1, no: 0 },
      canRate: true,
    });
    await expect(ratingsApi.rate(tripId, { ...firstRating, food: 10, wouldReturn: 'NO' })).resolves.toMatchObject({
      currentUserRating: { food: 10, wouldReturn: 'NO' },
      ratingCount: 5,
      returnIntent: { yes: 3, maybe: 1, no: 1 },
    });

    await ratingsApi.remove(tripId);
    await expect(ratingsApi.summary(tripId)).resolves.toMatchObject({
      currentUserRating: null,
      ratingCount: 4,
    });
  });
});
