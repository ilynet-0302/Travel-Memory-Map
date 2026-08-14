import { describe, expect, it } from 'vitest';
import { demoTrips } from '../trips/data/demoTrips';
import { buildDashboardTrips } from './dashboardModel';

describe('dashboard trip model', () => {
  it('selects the nearest upcoming trip and recent completed stories', () => {
    const dashboard = buildDashboardTrips(demoTrips, new Date('2026-08-13T10:00:00Z'));

    expect(dashboard.featured?.id).toBe('rome-2026');
    expect(dashboard.featuredKind).toBe('UPCOMING');
    expect(dashboard.recent.map(({ id }) => id)).toEqual(['budapest-2026', 'corfu-2025']);
    expect(dashboard.upcomingCount).toBe(1);
  });

  it('separates trips shared with the current user from owned trips', () => {
    const dashboard = buildDashboardTrips(demoTrips, new Date('2026-08-13T10:00:00Z'));

    expect(dashboard.shared.map(({ id }) => id)).toEqual(['budapest-2026', 'corfu-2025']);
    expect(dashboard.sharedCount).toBe(2);
  });
});
