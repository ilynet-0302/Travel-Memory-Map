import { afterEach, describe, expect, it, vi } from 'vitest';
import { onThisDayApi } from './onThisDayApi';

describe('demo on this day API', () => {
  afterEach(() => vi.useRealTimers());

  it('finds trips active on the same calendar day in an earlier year', async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2029-05-04T10:00:00Z'));

    const responsePromise = onThisDayApi.get();
    await vi.advanceTimersByTimeAsync(200);
    const response = await responsePromise;

    expect(response.date).toBe('2029-05-04');
    expect(response.memories).toHaveLength(1);
    expect(response.memories[0]).toMatchObject({
      tripId: 'budapest-2026',
      city: 'Budapest',
      memoryDate: '2026-05-04',
      yearsAgo: 3,
    });
  });

  it('returns an empty collection when the date has no travel history', async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2029-01-20T10:00:00Z'));

    const responsePromise = onThisDayApi.get();
    await vi.advanceTimersByTimeAsync(200);
    const response = await responsePromise;

    expect(response.memories).toEqual([]);
  });
});
