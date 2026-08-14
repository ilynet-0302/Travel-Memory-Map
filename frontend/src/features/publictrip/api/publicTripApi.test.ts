import { describe, expect, it } from 'vitest';
import { ApiError } from '../../../services/apiClient';
import { publicTripApi } from './publicTripApi';

describe('demo public trip API', () => {
  it('returns the curated story for a public slug', async () => {
    const trip = await publicTripApi.get('corfu-in-blue');

    expect(trip).toMatchObject({
      publicSlug: 'corfu-in-blue',
      title: 'Corfu in Blue',
      country: 'Greece',
      city: 'Corfu',
    });
    expect(trip).not.toHaveProperty('memberCount');
    expect(trip).not.toHaveProperty('spent');
    expect(trip).not.toHaveProperty('currentUserRole');
  });

  it('does not expose a private trip through a guessed slug', async () => {
    await expect(publicTripApi.get('budapest-2026')).rejects.toEqual(
      expect.objectContaining({ status: 404, code: 'PUBLIC_TRIP_NOT_FOUND' } satisfies Partial<ApiError>),
    );
  });
});
