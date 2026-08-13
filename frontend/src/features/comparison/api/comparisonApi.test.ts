import { describe, expect, it } from 'vitest';
import { comparisonApi } from './comparisonApi';

describe('demo comparison API', () => {
  it('compares two demo journeys with ratings, footprint and spending', async () => {
    const comparison = await comparisonApi.compare('budapest-2026', 'corfu-2025');

    expect(comparison.left).toMatchObject({
      id: 'budapest-2026',
      city: 'Budapest',
      travelDays: 4,
      photoCount: 84,
      ratingCount: 4,
    });
    expect(comparison.right).toMatchObject({
      id: 'corfu-2025',
      city: 'Corfu',
      travelDays: 7,
      photoCount: 127,
    });
    expect(comparison.left.ratings.food).toBeGreaterThanOrEqual(6);
    expect(comparison.left.spending).toEqual([
      { currency: 'EUR', total: 486, costPerDay: 121.5 },
    ]);
  });
});
