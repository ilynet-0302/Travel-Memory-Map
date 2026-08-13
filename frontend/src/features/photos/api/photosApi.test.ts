import { afterEach, describe, expect, it, vi } from 'vitest';
import { photosApi } from './photosApi';

describe('demo photos API', () => {
  afterEach(() => vi.restoreAllMocks());

  it('uploads, lists and deletes trip photos', async () => {
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:demo-photo');
    const tripId = `trip-${crypto.randomUUID()}`;
    const file = new File([new Uint8Array([1, 2, 3])], 'memory.jpg', { type: 'image/jpeg' });

    const uploaded = await photosApi.upload(tripId, {
      file,
      caption: 'Golden hour',
      tripStopId: 'colosseum',
    });

    expect(uploaded).toMatchObject({
      tripId,
      tripStopId: 'colosseum',
      caption: 'Golden hour',
      signedUrl: 'blob:demo-photo',
    });
    await expect(photosApi.list(tripId)).resolves.toHaveLength(1);

    await photosApi.delete(tripId, uploaded.id);

    await expect(photosApi.list(tripId)).resolves.toEqual([]);
  });
});
