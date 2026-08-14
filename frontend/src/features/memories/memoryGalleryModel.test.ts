import { describe, expect, it } from 'vitest';
import type { MemoryPhoto } from './types';
import { filterMemories, groupMemoriesByYear } from './memoryGalleryModel';

const memories: MemoryPhoto[] = [
  {
    id: 'rome-photo', tripId: 'rome', tripTitle: 'Roman Holiday', country: 'Italy', countryCode: 'IT', city: 'Rome',
    tripStartDate: '2025-09-12', tripEndDate: '2025-09-16', tripStopId: 'colosseum', tripStopName: 'Colosseum',
    uploadedByUserId: 'user', uploadedByDisplayName: 'Iliya', signedUrl: 'signed-rome', originalFileName: 'rome.jpg',
    contentType: 'image/jpeg', fileSize: 100, takenAt: '2025-09-13T10:00:00Z', latitude: 41.89, longitude: 12.49,
    caption: 'Golden morning', publicVisible: false, createdAt: '2025-09-14T10:00:00Z',
  },
  {
    id: 'corfu-photo', tripId: 'corfu', tripTitle: 'Corfu in Blue', country: 'Greece', countryCode: 'GR', city: 'Corfu',
    tripStartDate: '2024-08-17', tripEndDate: '2024-08-23', tripStopId: null, tripStopName: null,
    uploadedByUserId: 'user', uploadedByDisplayName: 'Iliya', signedUrl: 'signed-corfu', originalFileName: 'sea.webp',
    contentType: 'image/webp', fileSize: 100, takenAt: null, latitude: null, longitude: null,
    caption: 'Blue water', publicVisible: true, createdAt: '2024-08-20T12:00:00Z',
  },
];

describe('memory gallery model', () => {
  it('searches across trip, place and caption metadata', () => {
    expect(filterMemories(memories, { query: 'colosseum', year: 'ALL', tripId: 'ALL', locationOnly: false }))
      .toHaveLength(1);
    expect(filterMemories(memories, { query: 'blue water', year: 'ALL', tripId: 'ALL', locationOnly: false })[0].id)
      .toBe('corfu-photo');
  });

  it('filters located memories and groups newest years first', () => {
    expect(filterMemories(memories, { query: '', year: '2025', tripId: 'rome', locationOnly: true }))
      .toEqual([memories[0]]);
    expect(groupMemoriesByYear(memories).map(([year]) => year)).toEqual(['2025', '2024']);
  });
});
