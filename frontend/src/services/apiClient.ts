import { supabase } from './supabase';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

interface ApiErrorPayload {
  code?: string;
  message?: string;
  fieldErrors?: Record<string, string>;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code = 'API_ERROR',
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken() {
  if (!supabase) return null;
  if (!refreshPromise) {
    refreshPromise = supabase.auth.refreshSession()
      .then(({ data, error }) => error ? null : data.session?.access_token ?? null)
      .catch(() => null)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

function requestHeaders(init: RequestInit | undefined, accessToken: string | null, hasFormBody: boolean) {
  const headers = new Headers(init?.headers);
  if (!hasFormBody && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  return headers;
}

export async function apiClient<T>(path: string, init?: RequestInit): Promise<T> {
  const session = supabase ? (await supabase.auth.getSession()).data.session : null;
  const hasFormBody = init?.body instanceof FormData;
  const request = (accessToken: string | null) => fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: requestHeaders(init, accessToken, hasFormBody),
  });
  let response = await request(session?.access_token ?? null);

  if (response.status === 401 && session && supabase) {
    const refreshedAccessToken = await refreshAccessToken();
    if (refreshedAccessToken) response = await request(refreshedAccessToken);
    if (response.status === 401) await supabase.auth.signOut({ scope: 'local' });
  }

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ApiErrorPayload | null;
    throw new ApiError(
      error?.message ?? (response.status === 401
        ? 'Your session is no longer valid. Please sign in again.'
        : 'Something went wrong while talking to the server.'),
      response.status,
      error?.code,
      error?.fieldErrors,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
