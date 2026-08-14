import { describe, expect, it } from 'vitest';
import { tripsApi } from './tripsApi';

describe('demo trip search API', () => {
  it('searches trip places and years', async () => {
    await expect(tripsApi.search({ q: 'Colosseum' })).resolves.toMatchObject([
      { id: 'rome-2026' },
    ]);
    const in2025 = await tripsApi.search({ year: 2025 });
    expect(in2025.map(({ id }) => id)).toEqual(['corfu-2025']);
  });

  it('combines advanced filters and uses the current users relationship', async () => {
    const filtered = await tripsApi.search({
      relationship: 'OWNER',
      minRating: 9,
      minPrice: 700,
      maxPrice: 800,
      maxDuration: 5,
      dna: 'CULTURE',
    });
    expect(filtered.map(({ id }) => id)).toEqual(['rome-2026']);

    const shared = await tripsApi.search({ relationship: 'SHARED', sort: 'PRICE_DESC' });
    expect(shared.map(({ id }) => id)).toEqual(['corfu-2025', 'budapest-2026']);
  });
});
