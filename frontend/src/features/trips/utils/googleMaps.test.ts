import { describe, expect, it } from 'vitest';
import { parseGoogleMapsPlace } from './googleMaps';

describe('parseGoogleMapsPlace', () => {
  it('imports the place name and coordinates from a standard Maps URL', () => {
    expect(parseGoogleMapsPlace(
      'https://www.google.com/maps/place/Colosseum/@41.8902,12.4922,17z/data=!3m1!4b1',
    )).toMatchObject({ name: 'Colosseum', latitude: 41.8902, longitude: 12.4922 });
  });

  it('imports coordinates from a Maps query URL', () => {
    expect(parseGoogleMapsPlace(
      'https://maps.google.com/?q=41.8902%2C12.4922',
    )).toMatchObject({ latitude: 41.8902, longitude: 12.4922 });
  });

  it('rejects non-Google URLs', () => {
    expect(() => parseGoogleMapsPlace('https://example.com/maps?q=41.89,12.49'))
      .toThrow('full HTTPS Google Maps');
  });
});
