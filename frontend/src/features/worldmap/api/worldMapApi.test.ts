import { describe, expect, it } from 'vitest';
import { worldMapApi } from './worldMapApi';

describe('demo world map API', () => {
  it('separates visited and planned countries and builds country details', async () => {
    const worldMap = await worldMapApi.get();

    expect(worldMap).toMatchObject({
      countriesVisited: 2,
      countriesPlanned: 1,
      countriesTotal: 195,
    });
    expect(worldMap.countries.map((country) => country.countryCode)).toEqual(['GR', 'HU', 'IT']);
    expect(worldMap.countries.find((country) => country.countryCode === 'HU')).toMatchObject({
      name: 'Hungary',
      status: 'VISITED',
      tripCount: 1,
      cityCount: 1,
      photoCount: 84,
      spending: [{ currency: 'EUR', totalSpent: 486 }],
    });
    expect(worldMap.countries.find((country) => country.countryCode === 'IT')).toMatchObject({
      status: 'PLANNED',
      plannedTripCount: 1,
    });
  });
});
