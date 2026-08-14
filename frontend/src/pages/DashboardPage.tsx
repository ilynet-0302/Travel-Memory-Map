import {
  ArrowRight,
  Camera,
  CircleDollarSign,
  Clock3,
  Globe2,
  MapPin,
  PlaneTakeoff,
  Plus,
  RefreshCw,
  Route,
  Sparkles,
  UsersRound,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { buildDashboardTrips } from '../features/dashboard/dashboardModel';
import { OnThisDayCard } from '../features/memories/components/OnThisDayCard';
import { useProfile } from '../features/profile/hooks/useProfile';
import { CreateTripDialog } from '../features/trips/components/CreateTripDialog';
import { TripCard } from '../features/trips/components/TripCard';
import { useTrips } from '../features/trips/hooks/useTrips';
import type { Trip } from '../features/trips/types';
import { useWorldMap } from '../features/worldmap/hooks/useWorldMap';

function countryFlag(countryCode: string) {
  return countryCode.toUpperCase().replace(/./g, (letter) =>
    String.fromCodePoint(127397 + letter.charCodeAt(0)));
}

function formatMoney(amount: number, currency: string) {
  return new Intl.NumberFormat('en', {
    style: 'currency',
    currency,
    maximumFractionDigits: amount >= 1000 ? 0 : 2,
  }).format(amount);
}

function TripSectionEmpty({ title, description, action }: { title: string; description: string; action?: boolean }) {
  return (
    <div className="dashboard-trip-empty">
      <span><PlaneTakeoff size={21} /></span>
      <div><strong>{title}</strong><p>{description}</p></div>
      {action && <Link className="button button--coral" to="/?create=1"><Plus size={15} /> Plan a trip</Link>}
    </div>
  );
}

export function DashboardPage() {
  const tripsQuery = useTrips();
  const profile = useProfile();
  const worldMap = useWorldMap();
  const [searchParams, setSearchParams] = useSearchParams();
  const [createdTrip, setCreatedTrip] = useState<Trip | null>(null);
  const dialogOpen = searchParams.get('create') === '1';
  const closeDialog = useCallback(() => setSearchParams({}, { replace: true }), [setSearchParams]);
  const trips = tripsQuery.data ?? [];
  const dashboard = buildDashboardTrips(trips);

  useEffect(() => {
    if (!createdTrip) return;
    const timer = window.setTimeout(() => setCreatedTrip(null), 4_500);
    return () => window.clearTimeout(timer);
  }, [createdTrip]);

  const statistics = profile.data?.statistics;
  const personality = profile.data?.personality;
  const personalityStrength = personality ? Math.max(...Object.values(personality.scores)) : 0;
  const stats = [
    { label: 'Countries', value: statistics?.countriesVisited, note: `${worldMap.data?.countriesPlanned ?? 0} planned next`, icon: Globe2, tone: 'sage' },
    { label: 'Cities', value: statistics?.citiesVisited, note: 'Unique destinations', icon: MapPin, tone: 'coral' },
    { label: 'Trips', value: statistics?.trips, note: `${dashboard.upcomingCount} upcoming`, icon: PlaneTakeoff, tone: 'sand' },
    { label: 'Places', value: statistics?.placesVisited, note: 'Stops on your map', icon: Route, tone: 'blue' },
    { label: 'Photos', value: statistics?.photosUploaded, note: 'Memories uploaded', icon: Camera, tone: 'rose' },
    { label: 'Travel days', value: statistics?.travelDays, note: `${statistics?.completedTrips ?? 0} completed trips`, icon: Clock3, tone: 'violet' },
  ];
  const displayName = profile.data?.displayName.split(/\s+/)[0] ?? 'Traveller';
  const today = new Intl.DateTimeFormat('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })
    .format(new Date())
    .toUpperCase();
  const worldPercentage = worldMap.data
    ? Math.round((worldMap.data.countriesVisited / worldMap.data.countriesTotal) * 100)
    : 0;
  const featuredCopy = {
    UPCOMING: ['NEXT ADVENTURE', 'Pack your curiosity'],
    ACTIVE: ['ON THE ROAD', 'Keep the story moving'],
    RECENT: ['LATEST STORY', 'A journey worth remembering'],
    EMPTY: ['YOUR FIRST ADVENTURE', 'The map is waiting'],
  }[dashboard.featuredKind];
  const hasError = tripsQuery.isError || profile.isError || worldMap.isError;

  function retryDashboard() {
    void Promise.all([tripsQuery.refetch(), profile.refetch(), worldMap.refetch()]);
  }

  return (
    <div className="page page--dashboard">
      <header className="page-header dashboard-header">
        <div>
          <span className="eyebrow">{today}</span>
          <h1>Welcome back, {displayName} <span aria-hidden="true">✦</span></h1>
          <p>{dashboard.upcomingCount ? `${dashboard.upcomingCount} upcoming ${dashboard.upcomingCount === 1 ? 'journey is' : 'journeys are'} waiting for you.` : 'Your next story is closer than you think.'}</p>
        </div>
        <button className="button button--coral page-header__action" type="button" onClick={() => setSearchParams({ create: '1' })}>
          <Plus size={18} />
          Plan a trip
        </button>
      </header>

      {createdTrip && (
        <div className="toast" role="status">
          <span className="toast__icon"><Sparkles size={17} /></span>
          <span><strong>{createdTrip.title}</strong> is ready for its first stop.</span>
          <Link to={`/trips/${createdTrip.id}`}>Open trip <ArrowRight size={15} /></Link>
        </div>
      )}

      {hasError && (
        <div className="dashboard-error" role="alert">
          <span><RefreshCw size={17} /></span>
          <p><strong>Some dashboard insights took a detour.</strong> The available parts are still shown below.</p>
          <button type="button" onClick={retryDashboard}>Try again</button>
        </div>
      )}

      <section className="stats-grid stats-grid--dashboard" aria-label="Travel statistics">
        {stats.map(({ label, value, note, icon: Icon, tone }) => (
          <article className={`stat-card${profile.isLoading ? ' stat-card--loading' : ''}`} key={label}>
            <span className={`stat-card__icon stat-card__icon--${tone}`}><Icon size={19} /></span>
            <span className="stat-card__value">{profile.isLoading ? '—' : value ?? 0}</span>
            <span className="stat-card__label">{label}</span>
            <small>{note}</small>
          </article>
        ))}
      </section>

      <section className="dashboard-grid">
        <div className="dashboard-primary">
          <div className="section-heading">
            <div><span className="eyebrow">{featuredCopy[0]}</span><h2>{featuredCopy[1]}</h2></div>
            <Link to="/trips">All trips <ArrowRight size={16} /></Link>
          </div>
          {tripsQuery.isLoading ? (
            <div className="skeleton-card dashboard-featured-skeleton" />
          ) : dashboard.featured ? (
            <TripCard trip={dashboard.featured} featured />
          ) : (
            <TripSectionEmpty title="No journeys yet" description="Create your first trip and start turning places into stories." action />
          )}

          <div className="section-heading section-heading--recent">
            <div><span className="eyebrow">RECENT STORIES</span><h2>Places worth remembering</h2></div>
          </div>
          {tripsQuery.isLoading ? (
            <div className="recent-grid"><div className="skeleton-card" /><div className="skeleton-card" /></div>
          ) : dashboard.recent.length ? (
            <div className="recent-grid">{dashboard.recent.map((trip) => <TripCard key={trip.id} trip={trip} />)}</div>
          ) : (
            <TripSectionEmpty title="Your recent stories will appear here" description="Complete a journey to add it to your travel history." />
          )}

          <div className="section-heading section-heading--shared">
            <div><span className="eyebrow">SHARED WITH YOU</span><h2>Journeys made together</h2></div>
            {dashboard.sharedCount > 0 && <Link to="/trips?view=SHARED">View {dashboard.sharedCount} <ArrowRight size={16} /></Link>}
          </div>
          {tripsQuery.isLoading ? (
            <div className="recent-grid"><div className="skeleton-card" /><div className="skeleton-card" /></div>
          ) : dashboard.shared.length ? (
            <div className="recent-grid">{dashboard.shared.map((trip) => <TripCard key={trip.id} trip={trip} />)}</div>
          ) : (
            <TripSectionEmpty title="No shared journeys yet" description="Trips shared by other travellers will be collected here." />
          )}
        </div>

        <aside className="dashboard-aside">
          <article className="world-card world-card--dashboard">
            <header>
              <span className="eyebrow eyebrow--light">YOUR WORLD</span>
              <strong><span>{worldMap.data?.countriesVisited ?? 0}</span> / {worldMap.data?.countriesTotal ?? 195}</strong>
              <p>{worldMap.data?.countriesPlanned ?? 0} countries planned next</p>
            </header>
            <div className="dashboard-world-progress" aria-label={`${worldPercentage}% of the world visited`}><span style={{ width: `${Math.max(worldPercentage, worldMap.data?.countriesVisited ? 2 : 0)}%` }} /></div>
            <div className="dashboard-world-countries">
              {worldMap.isLoading ? <small>Unfolding your map…</small> : worldMap.data?.countries.slice(0, 3).map((country) => (
                <span key={country.countryCode}><i>{countryFlag(country.countryCode)}</i>{country.name}</span>
              ))}
              {!worldMap.isLoading && !worldMap.data?.countries.length && <small>Your first country will appear after you create a trip.</small>}
            </div>
            <div className="abstract-map" aria-hidden="true"><span className="land land--one" /><span className="land land--two" /><span className="land land--three" /></div>
            <Link to="/map">Explore your scratch map <ArrowRight size={16} /></Link>
          </article>

          <article className="dashboard-spending-card">
            <header><span><CircleDollarSign size={18} /></span><div><small>TOTAL TRAVEL SPEND</small><h3>Your journey ledger</h3></div></header>
            {profile.isLoading ? (
              <div className="dashboard-spending-loading"><span /><span /></div>
            ) : statistics?.spending.length ? (
              <div className="dashboard-spending-list">
                {statistics.spending.map((spending) => (
                  <div key={spending.currency}>
                    <span><strong>{formatMoney(spending.totalSpent, spending.currency)}</strong><small>{spending.currency}</small></span>
                    <em>{formatMoney(spending.averageCostPerDay, spending.currency)} / travel day</em>
                  </div>
                ))}
              </div>
            ) : <p className="dashboard-spending-empty">No expenses logged yet. Add costs inside a trip to see your totals.</p>}
            <Link to="/profile">Full statistics <ArrowRight size={15} /></Link>
          </article>

          <OnThisDayCard />

          <article className="dna-card">
            <div>
              <span className="eyebrow">TRAVEL DNA</span>
              <h3>{personality?.type ?? 'Still taking shape'}</h3>
              <p>{personality?.description ?? 'Complete more journeys to reveal your travel personality.'}</p>
              <Link to="/profile">View profile <ArrowRight size={15} /></Link>
            </div>
            <div className="dna-orbit" aria-hidden="true"><span>{personalityStrength}%</span></div>
          </article>

          <article className="dashboard-shared-summary">
            <span><UsersRound size={19} /></span>
            <div><small>COLLABORATION</small><strong>{dashboard.sharedCount} shared {dashboard.sharedCount === 1 ? 'journey' : 'journeys'}</strong><p>Stories planned and remembered together.</p></div>
          </article>
        </aside>
      </section>

      <CreateTripDialog open={dialogOpen} onClose={closeDialog} onCreated={setCreatedTrip} />
    </div>
  );
}
