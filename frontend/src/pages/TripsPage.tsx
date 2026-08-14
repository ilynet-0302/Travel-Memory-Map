import { ArrowLeftRight, Compass, Plus, Search, SlidersHorizontal, X } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { TripCard } from '../features/trips/components/TripCard';
import { TripFiltersDialog } from '../features/trips/components/TripFiltersDialog';
import { useTripSearch } from '../features/trips/hooks/useTrips';
import type { TripSearchFilters } from '../features/trips/types';

const quickFilters = ['ALL', 'UPCOMING', 'COMPLETED', 'SHARED'] as const;
type QuickFilter = (typeof quickFilters)[number];

const numberParam = (params: URLSearchParams, name: string) => {
  const value = params.get(name);
  return value ? Number(value) : undefined;
};

function filtersFromParams(params: URLSearchParams): TripSearchFilters {
  return {
    status: (params.get('status') || undefined) as TripSearchFilters['status'],
    year: numberParam(params, 'year'),
    country: params.get('country') || undefined,
    city: params.get('city') || undefined,
    minRating: numberParam(params, 'minRating'),
    minPrice: numberParam(params, 'minPrice'),
    maxPrice: numberParam(params, 'maxPrice'),
    minDuration: numberParam(params, 'minDuration'),
    maxDuration: numberParam(params, 'maxDuration'),
    dna: (params.get('dna') || undefined) as TripSearchFilters['dna'],
    relationship: (params.get('relationship') || undefined) as TripSearchFilters['relationship'],
    sort: (params.get('sort') || 'START_DESC') as TripSearchFilters['sort'],
    currency: params.get('currency') || 'EUR',
  };
}

const filterLabels: Partial<Record<keyof TripSearchFilters, (value: string | number) => string>> = {
  status: (value) => String(value).toLowerCase(),
  year: (value) => String(value),
  country: (value) => `Country: ${value}`,
  city: (value) => `City: ${value}`,
  minRating: (value) => `Rating ${value}+`,
  minPrice: (value) => `From ${value}`,
  maxPrice: (value) => `Up to ${value}`,
  minDuration: (value) => `${value}+ days`,
  maxDuration: (value) => `Up to ${value} days`,
  dna: (value) => String(value).toLowerCase().replace('_', ' '),
  relationship: (value) => value === 'OWNER' ? 'Owned by me' : 'Shared with me',
};

export function TripsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [query, setQuery] = useState(() => searchParams.get('q') ?? '');
  const [debouncedQuery, setDebouncedQuery] = useState(query);
  const [quickFilter, setQuickFilter] = useState<QuickFilter>(() => {
    const value = searchParams.get('view') as QuickFilter | null;
    return value && quickFilters.includes(value) ? value : 'ALL';
  });
  const [advanced, setAdvanced] = useState<TripSearchFilters>(() => filtersFromParams(searchParams));
  const [filtersOpen, setFiltersOpen] = useState(false);

  useEffect(() => {
    const timer = globalThis.setTimeout(() => setDebouncedQuery(query.trim()), 300);
    return () => globalThis.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const next = new URLSearchParams();
    if (debouncedQuery) next.set('q', debouncedQuery);
    if (quickFilter !== 'ALL') next.set('view', quickFilter);
    Object.entries(advanced).forEach(([key, value]) => {
      if (value !== undefined && value !== '' && value !== 'ALL'
          && !(key === 'sort' && value === 'START_DESC')
          && !(key === 'currency' && value === 'EUR')) {
        next.set(key, String(value));
      }
    });
    if (next.toString() !== searchParams.toString()) setSearchParams(next, { replace: true });
  }, [advanced, debouncedQuery, quickFilter, searchParams, setSearchParams]);

  const filters = useMemo<TripSearchFilters>(() => ({
    ...advanced,
    q: debouncedQuery || undefined,
    status: quickFilter === 'UPCOMING' || quickFilter === 'COMPLETED' ? quickFilter : advanced.status,
    relationship: quickFilter === 'SHARED' ? 'SHARED' : advanced.relationship,
  }), [advanced, debouncedQuery, quickFilter]);
  const { data: trips = [], isLoading, isFetching, isError } = useTripSearch(filters);

  const activeAdvanced = Object.entries(advanced).filter(([key, value]) =>
    value !== undefined && value !== '' && value !== 'ALL'
      && key !== 'sort' && key !== 'currency');
  const filterCount = activeAdvanced.length;

  function chooseQuickFilter(value: QuickFilter) {
    setQuickFilter(value);
    if (value !== 'ALL') {
      setAdvanced((current) => ({ ...current, status: undefined, relationship: undefined }));
    }
  }

  function applyAdvanced(next: TripSearchFilters) {
    setAdvanced(next);
    setQuickFilter('ALL');
    setFiltersOpen(false);
  }

  function clearAdvanced(name: string) {
    setAdvanced((current) => ({ ...current, [name]: undefined }));
  }

  function clearAll() {
    setQuery('');
    setDebouncedQuery('');
    setQuickFilter('ALL');
    setAdvanced({ sort: 'START_DESC', currency: 'EUR' });
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">YOUR JOURNEYS</span>
          <h1>My trips</h1>
          <p>Every pin has a story attached to it.</p>
        </div>
        <div className="page-header__actions">
          <Link className="button button--ghost" to="/compare"><ArrowLeftRight size={17} /> Compare</Link>
          <Link className="button button--coral" to="/?create=1"><Plus size={18} /> New trip</Link>
        </div>
      </header>

      <div className="trip-toolbar">
        <label className="search-box">
          <Search size={18} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search trip, country, city, place or year" />
          {query && <button type="button" aria-label="Clear search" onClick={() => setQuery('')}><X size={15} /></button>}
        </label>
        <div className="filter-tabs" role="group" aria-label="Filter trips">
          {quickFilters.map((value) => (
            <button key={value} className={quickFilter === value ? 'is-active' : ''} type="button" onClick={() => chooseQuickFilter(value)}>
              {value.toLowerCase()}
            </button>
          ))}
        </div>
        <button className={`icon-button toolbar-filter${filterCount ? ' is-active' : ''}`} type="button" aria-label="Advanced filters" onClick={() => setFiltersOpen(true)}>
          <SlidersHorizontal size={18} />
          {filterCount > 0 && <span>{filterCount}</span>}
        </button>
      </div>

      {(activeAdvanced.length > 0 || quickFilter !== 'ALL' || debouncedQuery) && (
        <div className="active-filter-row">
          {quickFilter !== 'ALL' && <button type="button" onClick={() => setQuickFilter('ALL')}>{quickFilter.toLowerCase()} <X size={13} /></button>}
          {activeAdvanced.map(([key, value]) => {
            const label = filterLabels[key as keyof TripSearchFilters];
            return label && <button key={key} type="button" onClick={() => clearAdvanced(key)}>{label(value as string | number)} <X size={13} /></button>;
          })}
          <button className="clear-filter-button" type="button" onClick={clearAll}>Clear all</button>
        </div>
      )}

      <div className="trip-results-summary" aria-live="polite">
        <p><strong>{trips.length}</strong> {trips.length === 1 ? 'journey' : 'journeys'} found</p>
        {isFetching && !isLoading && <span>Updating results…</span>}
      </div>

      {isLoading ? (
        <div className="trips-grid"><div className="skeleton-card" /><div className="skeleton-card" /></div>
      ) : isError ? (
        <div className="empty-state"><span><Compass size={28} /></span><h2>Search is taking a detour</h2><p>We could not load these journeys. Please try again.</p></div>
      ) : trips.length ? (
        <div className="trips-grid">
          {trips.map((trip) => <TripCard key={trip.id} trip={trip} />)}
        </div>
      ) : (
        <div className="empty-state">
          <span><Compass size={28} /></span>
          <h2>No journeys found</h2>
          <p>Adjust the filters or start a brand-new adventure.</p>
          <button className="button button--ghost" type="button" onClick={clearAll}>Clear filters</button>
          <Link className="button button--dark" to="/?create=1"><Plus size={17} /> Create trip</Link>
        </div>
      )}

      {filtersOpen && <TripFiltersDialog filters={advanced} onApply={applyAdvanced} onClose={() => setFiltersOpen(false)} />}
    </div>
  );
}
