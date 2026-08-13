import { describe, expect, it } from 'vitest';
import { ratingsApi } from './ratingsApi';

describe('demo ratings API', () => {
  it('adds, replaces and removes the current traveller rating', async () => {
    const tripId = `trip-${crypto.randomUUID()}`;

    await expect(ratingsApi.rate(tripId, 7)).resolves.toMatchObject({
      currentUserScore: 7,
      ratingCount: 5,
      canRate: true,
    });
    await expect(ratingsApi.rate(tripId, 10)).resolves.toMatchObject({
      currentUserScore: 10,
      ratingCount: 5,
    });

    await ratingsApi.remove(tripId);
    await expect(ratingsApi.summary(tripId)).resolves.toMatchObject({
      currentUserScore: null,
      ratingCount: 4,
    });
  });
});
