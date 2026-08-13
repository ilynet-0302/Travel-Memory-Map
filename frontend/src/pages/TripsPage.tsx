import { Compass, Plus, Search, SlidersHorizontal } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { TripCard } from '../features/trips/components/TripCard';
import { useTrips } from '../features/trips/hooks/useTrips';

const filters = ['ALL', 'UPCOMING', 'COMPLETED', 'SHARED'] as const;

export function TripsPage() {
  const { data: trips = [], isLoading } = useTrips();
  const [filter, setFilter] = useState<(typeof filters)[number]>('ALL');
  const [query, setQuery] = useState('');

  const visibleTrips = useMemo(
    () =>
      trips.filter((trip) => {
        const matchesFilter =
          filter === 'ALL' ||
          (filter === 'SHARED' ? trip.memberCount > 1 : trip.status === filter);
        const searchable = `${trip.title} ${trip.city} ${trip.country}`.toLowerCase();
        return matchesFilter && searchable.includes(query.toLowerCase());
      }),
    [filter, query, trips],
  );

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">YOUR JOURNEYS</span>
          <h1>My trips</h1>
          <p>Every pin has a story attached to it.</p>
        </div>
        <Link className="button button--coral" to="/?create=1">
          <Plus size={18} /> New trip
        </Link>
      </header>

      <div className="trip-toolbar">
        <label className="search-box">
          <Search size={18} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search city, country or trip" />
        </label>
        <div className="filter-tabs" role="group" aria-label="Filter trips">
          {filters.map((value) => (
            <button key={value} className={filter === value ? 'is-active' : ''} type="button" onClick={() => setFilter(value)}>
              {value.toLowerCase()}
            </button>
          ))}
        </div>
        <button className="icon-button toolbar-filter" type="button" aria-label="Advanced filters" disabled>
          <SlidersHorizontal size={18} />
        </button>
      </div>

      {isLoading ? (
        <div className="trips-grid"><div className="skeleton-card" /><div className="skeleton-card" /></div>
      ) : visibleTrips.length ? (
        <div className="trips-grid">
          {visibleTrips.map((trip) => <TripCard key={trip.id} trip={trip} />)}
        </div>
      ) : (
        <div className="empty-state">
          <span><Compass size={28} /></span>
          <h2>No journeys found</h2>
          <p>Try a different search or start a brand-new adventure.</p>
          <Link className="button button--dark" to="/?create=1"><Plus size={17} /> Create trip</Link>
        </div>
      )}
    </div>
  );
}
