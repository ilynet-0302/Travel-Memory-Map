import type { Trip } from '../trips/types';

export interface DashboardTrips {
  featured: Trip | undefined;
  featuredKind: 'UPCOMING' | 'ACTIVE' | 'RECENT' | 'EMPTY';
  recent: Trip[];
  shared: Trip[];
  upcomingCount: number;
  sharedCount: number;
}

export function buildDashboardTrips(trips: Trip[], today = new Date()): DashboardTrips {
  const todayKey = today.toISOString().slice(0, 10);
  const upcoming = trips
    .filter((trip) => trip.startDate > todayKey)
    .sort((left, right) => left.startDate.localeCompare(right.startDate));
  const active = trips
    .filter((trip) => trip.startDate <= todayKey && trip.endDate >= todayKey)
    .sort((left, right) => right.startDate.localeCompare(left.startDate));
  const completed = trips
    .filter((trip) => trip.endDate < todayKey)
    .sort((left, right) => right.endDate.localeCompare(left.endDate));
  const featured = upcoming[0] ?? active[0] ?? completed[0];
  const featuredKind = upcoming[0]
    ? 'UPCOMING'
    : active[0] ? 'ACTIVE' : completed[0] ? 'RECENT' : 'EMPTY';
  const shared = trips
    .filter((trip) => trip.currentUserRole !== 'OWNER')
    .sort((left, right) => right.startDate.localeCompare(left.startDate));

  return {
    featured,
    featuredKind,
    recent: completed.filter((trip) => trip.id !== featured?.id).slice(0, 2),
    shared: shared.slice(0, 2),
    upcomingCount: upcoming.length,
    sharedCount: shared.length,
  };
}
