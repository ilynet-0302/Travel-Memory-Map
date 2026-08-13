import { describe, expect, it } from 'vitest';
import { buildAuthRedirectUrl } from './authRedirect';

describe('authentication redirects', () => {
  it('keeps an invitation path under the deployed base path', () => {
    expect(buildAuthRedirectUrl('/join/secure-token', 'https://ilia.example', '/travel-memory-map/'))
      .toBe('https://ilia.example/travel-memory-map/join/secure-token');
  });

  it('rejects external destinations', () => {
    expect(buildAuthRedirectUrl('//attacker.example', 'https://ilia.example', '/travel-memory-map/'))
      .toBe('https://ilia.example/travel-memory-map/');
  });
});
