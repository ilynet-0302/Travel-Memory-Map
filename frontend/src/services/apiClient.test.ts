import { afterEach, describe, expect, it, vi } from 'vitest';

const auth = vi.hoisted(() => ({
  getSession: vi.fn(),
  refreshSession: vi.fn(),
  signOut: vi.fn(),
}));

vi.mock('./supabase', () => ({ supabase: { auth } }));

import { apiClient } from './apiClient';

describe('apiClient authentication recovery', () => {
  afterEach(() => {
    vi.resetAllMocks();
    vi.unstubAllGlobals();
  });

  it('refreshes a rejected session and retries with the new access token', async () => {
    auth.getSession.mockResolvedValue({ data: { session: { access_token: 'old-token' } } });
    auth.refreshSession.mockResolvedValue({
      data: { session: { access_token: 'new-token' } },
      error: null,
    });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 'trip-1' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient<{ id: string }>('/trips/trip-1')).resolves.toEqual({ id: 'trip-1' });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect((fetchMock.mock.calls[0][1].headers as Headers).get('Authorization')).toBe('Bearer old-token');
    expect((fetchMock.mock.calls[1][1].headers as Headers).get('Authorization')).toBe('Bearer new-token');
    expect(auth.signOut).not.toHaveBeenCalled();
  });

  it('clears only the local session when the refreshed token is still rejected', async () => {
    auth.getSession.mockResolvedValue({ data: { session: { access_token: 'old-token' } } });
    auth.refreshSession.mockResolvedValue({
      data: { session: { access_token: 'new-token' } },
      error: null,
    });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
    auth.signOut.mockResolvedValue({ error: null });

    const request = apiClient('/profile');

    await expect(request).rejects.toMatchObject({
      status: 401,
      message: 'Your session is no longer valid. Please sign in again.',
    });
    expect(auth.signOut).toHaveBeenCalledWith({ scope: 'local' });
  });
});
