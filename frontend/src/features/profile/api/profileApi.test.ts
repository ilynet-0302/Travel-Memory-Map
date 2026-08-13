import { describe, expect, it } from 'vitest';
import { profileApi } from './profileApi';

describe('demo profile API', () => {
  it('returns statistics and persists profile edits in the demo adapter', async () => {
    const initialProfile = await profileApi.get();

    expect(initialProfile.statistics.trips).toBeGreaterThan(0);

    const updatedProfile = await profileApi.update({ displayName: 'Updated Traveller' });

    expect(updatedProfile.displayName).toBe('Updated Traveller');
    await expect(profileApi.get()).resolves.toMatchObject({ displayName: 'Updated Traveller' });
  });
});
