import type { ImportedMapPlace } from '../types';

const coordinatesInPath = [
  /\/@(-?\d{1,2}(?:\.\d+)?),(-?\d{1,3}(?:\.\d+)?)/,
  /!3d(-?\d{1,2}(?:\.\d+)?)!4d(-?\d{1,3}(?:\.\d+)?)/,
];
const plainCoordinates = /(-?\d{1,2}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)/;

function supportedUrl(value: string) {
  let url: URL;
  try {
    url = new URL(value.trim());
  } catch {
    throw new Error('Paste a valid Google Maps link.');
  }
  const host = url.hostname.toLowerCase();
  const googleHost = host === 'google.com' || host.endsWith('.google.com');
  const mapsPath = host === 'maps.google.com' || url.pathname === '/maps' || url.pathname.startsWith('/maps/');
  if (url.protocol !== 'https:' || !googleHost || !mapsPath) {
    throw new Error('Demo mode supports full HTTPS Google Maps place links.');
  }
  return url;
}

function validCoordinates(match: RegExpMatchArray | null) {
  if (!match) return null;
  const latitude = Number(match[1]);
  const longitude = Number(match[2]);
  if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
    throw new Error('The Google Maps link contains invalid coordinates.');
  }
  return { latitude, longitude };
}

export function parseGoogleMapsPlace(value: string): ImportedMapPlace {
  const url = supportedUrl(value);
  const decoded = decodeURIComponent(url.toString());
  let coordinates = coordinatesInPath
    .map((pattern) => validCoordinates(decoded.match(pattern)))
    .find(Boolean);

  if (!coordinates) {
    for (const key of ['q', 'query', 'll', 'destination', 'daddr']) {
      coordinates = validCoordinates(url.searchParams.get(key)?.match(plainCoordinates) ?? null);
      if (coordinates) break;
    }
  }
  if (!coordinates) {
    throw new Error('This link does not contain coordinates. Copy the share link for a selected Google Maps place.');
  }

  const placeMatch = decodeURIComponent(url.pathname).match(/(?:^|\/)place\/([^/]+)/);
  const name = placeMatch?.[1].replaceAll('+', ' ').trim() || 'Imported place';
  return { ...coordinates, name, resolvedUrl: url.toString() };
}
