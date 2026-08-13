import {
  ArrowRight,
  Camera,
  Clock3,
  Coins,
  Compass,
  Globe2,
  MapPin,
  Plus,
  Sparkles,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CreateTripDialog } from '../features/trips/components/CreateTripDialog';
import { TripCard } from '../features/trips/components/TripCard';
import { useTrips } from '../features/trips/hooks/useTrips';
import type { Trip } from '../features/trips/types';

const stats = [
  { label: 'Countries', value: '12', note: '+3 this year', icon: Globe2, tone: 'sage' },
  { label: 'Cities', value: '31', note: 'Across 8 trips', icon: MapPin, tone: 'coral' },
  { label: 'Travel days', value: '47', note: '8 more planned', icon: Clock3, tone: 'sand' },
  { label: 'Memories', value: '427', note: 'Photos & notes', icon: Camera, tone: 'blue' },
];

export function DashboardPage() {
  const { data: trips = [], isLoading } = useTrips();
  const [searchParams, setSearchParams] = useSearchParams();
  const [createdTrip, setCreatedTrip] = useState<Trip | null>(null);
  const dialogOpen = searchParams.get('create') === '1';
  const closeDialog = useCallback(() => setSearchParams({}, { replace: true }), [setSearchParams]);

  useEffect(() => {
    if (!createdTrip) return;
    const timer = window.setTimeout(() => setCreatedTrip(null), 4_500);
    return () => window.clearTimeout(timer);
  }, [createdTrip]);

  const featuredTrip = trips.find(({ id }) => id === 'rome-2026') ?? trips[0];
  const recentTrips = trips.filter(({ id }) => id !== featuredTrip?.id).slice(0, 2);

  return (
    <div className="page page--dashboard">
      <header className="page-header">
        <div>
          <span className="eyebrow">TUESDAY · 11 AUGUST</span>
          <h1>Good evening, Iliya <span aria-hidden="true">✦</span></h1>
          <p>Your next story is closer than you think.</p>
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

      <section className="stats-grid" aria-label="Travel statistics">
        {stats.map(({ label, value, note, icon: Icon, tone }) => (
          <article className="stat-card" key={label}>
            <span className={`stat-card__icon stat-card__icon--${tone}`}><Icon size={19} /></span>
            <span className="stat-card__value">{value}</span>
            <span className="stat-card__label">{label}</span>
            <small>{note}</small>
          </article>
        ))}
      </section>

      <section className="dashboard-grid">
        <div className="dashboard-primary">
          <div className="section-heading">
            <div>
              <span className="eyebrow">NEXT ADVENTURE</span>
              <h2>Pack your curiosity</h2>
            </div>
            <Link to="/trips">All trips <ArrowRight size={16} /></Link>
          </div>
          {isLoading ? <div className="skeleton-card" /> : featuredTrip && <TripCard trip={featuredTrip} featured />}

          <div className="section-heading section-heading--recent">
            <div>
              <span className="eyebrow">RECENT STORIES</span>
              <h2>Places worth remembering</h2>
            </div>
          </div>
          <div className="recent-grid">
            {recentTrips.map((trip) => <TripCard key={trip.id} trip={trip} />)}
          </div>
        </div>

        <aside className="dashboard-aside">
          <article className="world-card">
            <header>
              <span className="eyebrow eyebrow--light">YOUR WORLD</span>
              <strong><span>12</span> / 195</strong>
              <p>countries explored</p>
            </header>
            <div className="abstract-map" aria-hidden="true">
              <span className="land land--one" />
              <span className="land land--two" />
              <span className="land land--three" />
              <span className="map-dot map-dot--rome" />
              <span className="map-dot map-dot--corfu" />
              <span className="map-dot map-dot--budapest" />
            </div>
            <Link to="/map">Explore your map <ArrowRight size={16} /></Link>
          </article>

          <article className="memory-card">
            <div className="memory-card__icon"><Compass size={21} /></div>
            <span className="eyebrow">ON THIS DAY</span>
            <h3>3 years ago, you were in <em>Budapest</em>.</h3>
            <div className="memory-card__meta">
              <span><MapPin size={15} /> 6 places</span>
              <span><Camera size={15} /> 18 photos</span>
              <span><Coins size={15} /> €82 spent</span>
            </div>
            <button type="button" className="text-button">Relive the day <ArrowRight size={15} /></button>
          </article>

          <article className="dna-card">
            <div>
              <span className="eyebrow">TRAVEL DNA</span>
              <h3>Urban explorer</h3>
              <p>You collect street corners, local tables and stories hidden in plain sight.</p>
              <Link to="/profile">View profile <ArrowRight size={15} /></Link>
            </div>
            <div className="dna-orbit" aria-hidden="true">
              <span>82%</span>
            </div>
          </article>
        </aside>
      </section>

      <CreateTripDialog
        open={dialogOpen}
        onClose={closeDialog}
        onCreated={setCreatedTrip}
      />
    </div>
  );
}
