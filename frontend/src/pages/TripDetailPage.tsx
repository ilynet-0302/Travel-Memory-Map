import {
  ArrowLeft,
  Archive,
  CalendarDays,
  Camera,
  ChevronRight,
  CircleDollarSign,
  Compass,
  Clock3,
  LockKeyhole,
  MapPin,
  MoreHorizontal,
  Navigation,
  Pencil,
  Star,
  Trash2,
} from 'lucide-react';
import { useCallback, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AvatarStack } from '../components/ui/AvatarStack';
import { TripMembersPanel } from '../features/collaboration/components/TripMembersPanel';
import { TripMap } from '../features/map/components/TripMap';
import { PhotosPanel } from '../features/photos/components/PhotosPanel';
import { ReplayControls } from '../features/replay/components/ReplayControls';
import { EditTripDialog } from '../features/trips/components/EditTripDialog';
import { TripStopDialog } from '../features/trips/components/TripStopDialog';
import { useArchiveTrip, useDeleteStop, useDeleteTrip, useTrip } from '../features/trips/hooks/useTrips';
import type { StopCategory, TripStop } from '../features/trips/types';
import { formatDateRange, tripDuration } from '../utils/date';

const categoryEmoji: Record<StopCategory, string> = {
  AIRPORT: '✈',
  HOTEL: '⌂',
  LANDMARK: '◇',
  RESTAURANT: '◉',
  BEACH: '≈',
  MUSEUM: '▤',
  BAR: '♢',
  SHOP: '▣',
  NATURE: '♧',
  TRANSPORT: '↗',
  OTHER: '•',
};

export function TripDetailPage() {
  const { tripId = '' } = useParams();
  const { data: trip, isLoading, error } = useTrip(tripId);
  const [activeIndex, setActiveIndex] = useState(0);
  const [activeTab, setActiveTab] = useState('Journey');
  const [editTripOpen, setEditTripOpen] = useState(false);
  const [stopDialogOpen, setStopDialogOpen] = useState(false);
  const [selectedStop, setSelectedStop] = useState<TripStop | undefined>();
  const [actionsOpen, setActionsOpen] = useState(false);
  const archiveTrip = useArchiveTrip(tripId);
  const deleteTrip = useDeleteTrip(tripId);
  const deleteStop = useDeleteStop(tripId);
  const navigate = useNavigate();

  const selectStop = useCallback(
    (stopId: string) => {
      const index = trip?.stops.findIndex(({ id }) => id === stopId) ?? -1;
      if (index >= 0) setActiveIndex(index);
    },
    [trip?.stops],
  );

  const groupedStops = useMemo(() => {
    if (!trip) return [];
    return [...new Set(trip.stops.map(({ day }) => day))].map((day) => ({
      day,
      stops: trip.stops.filter((stop) => stop.day === day),
    }));
  }, [trip]);

  if (isLoading) return <div className="page"><div className="detail-skeleton" /></div>;
  if (!trip || error) {
    return (
      <div className="page empty-state">
        <h1>Trip not found</h1>
        <p>{error?.message ?? 'This journey may have been archived.'}</p>
        <Link className="button button--dark" to="/trips">Back to trips</Link>
      </div>
    );
  }

  const safeActiveIndex = Math.min(activeIndex, Math.max(0, trip.stops.length - 1));
  const activeStop = trip.stops[safeActiveIndex];
  const isOwner = trip.currentUserRole === 'OWNER';
  const canEditStops = trip.currentUserRole === 'OWNER' || trip.currentUserRole === 'EDITOR';
  const actionError = archiveTrip.error ?? deleteTrip.error ?? deleteStop.error;

  const openNewStop = () => {
    setSelectedStop(undefined);
    setStopDialogOpen(true);
  };

  const openEditStop = (stop: TripStop) => {
    setSelectedStop(stop);
    setStopDialogOpen(true);
  };

  const confirmArchive = async () => {
    setActionsOpen(false);
    if (!window.confirm(`Archive “${trip.title}”? It will disappear from your active trip list.`)) return;
    try {
      await archiveTrip.mutateAsync();
      navigate('/trips');
    } catch {
      return;
    }
  };

  const confirmDeleteTrip = async () => {
    setActionsOpen(false);
    if (!window.confirm(`Permanently delete “${trip.title}” and all its content? This cannot be undone.`)) return;
    try {
      await deleteTrip.mutateAsync();
      navigate('/trips');
    } catch {
      return;
    }
  };

  const confirmDeleteStop = async (stop: TripStop) => {
    if (!window.confirm(`Remove “${stop.name}” from this journey?`)) return;
    try {
      await deleteStop.mutateAsync(stop.id);
    } catch {
      return;
    }
  };

  return (
    <div className="page page--trip-detail">
      <div className="trip-detail__topbar">
        <Link to="/trips" className="back-link"><ArrowLeft size={17} /> All trips</Link>
        {isOwner && (
          <div className="trip-actions">
            <button className="button button--ghost" type="button" onClick={() => setEditTripOpen(true)}><Pencil size={16} /> Edit trip</button>
            <div className="trip-actions__menu-wrap">
              <button className="icon-button" type="button" aria-label="More trip actions" aria-expanded={actionsOpen} onClick={() => setActionsOpen((open) => !open)}><MoreHorizontal size={20} /></button>
              {actionsOpen && (
                <div className="trip-actions__menu">
                  <button type="button" onClick={() => void confirmArchive()}><Archive size={15} /> Archive trip</button>
                  <button className="is-danger" type="button" onClick={() => void confirmDeleteTrip()}><Trash2 size={15} /> Delete permanently</button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {actionError && <p className="page-action-error" role="alert">{actionError.message}</p>}

      <header className={`trip-hero trip-hero--${trip.accent}`}>
        <div className="trip-hero__art" aria-hidden="true">
          <span className="trip-hero__sun" />
          <span className="trip-hero__dome trip-hero__dome--one" />
          <span className="trip-hero__dome trip-hero__dome--two" />
          <span className="trip-hero__line" />
        </div>
        <div className="trip-hero__content">
          <span className="trip-hero__privacy"><LockKeyhole size={14} /> {trip.visibility.toLowerCase()} trip</span>
          <span className="eyebrow eyebrow--light">{trip.city.toUpperCase()} · {trip.country.toUpperCase()}</span>
          <h1>{trip.title}</h1>
          <p>{trip.description}</p>
          <div className="trip-hero__meta">
            <span><CalendarDays size={16} /> {formatDateRange(trip.startDate, trip.endDate)}</span>
            <span><Clock3 size={16} /> {tripDuration(trip.startDate, trip.endDate)} days</span>
            <span><MapPin size={16} /> {trip.stops.length} places</span>
          </div>
        </div>
        <div className="trip-hero__people">
          <AvatarStack count={trip.memberCount} />
          <span>{trip.memberCount} travellers</span>
        </div>
      </header>

      <nav className="trip-tabs" aria-label="Trip sections">
        {['Journey', 'Photos', 'Expenses', 'Members'].map((tab) => (
          <button key={tab} type="button" className={activeTab === tab ? 'is-active' : ''} onClick={() => setActiveTab(tab)}>
            {tab}
          </button>
        ))}
      </nav>

      {activeTab === 'Journey' ? (
        <>
          <section className="journey-grid">
            <div className="map-panel">
              <div className="map-panel__header">
                <div>
                  <span className="eyebrow">INTERACTIVE ROUTE</span>
                  <h2>Follow the story</h2>
                </div>
                <span className="route-distance"><Navigation size={15} /> 48 km</span>
              </div>
              <div className="map-panel__canvas">
                <TripMap stops={trip.stops} activeStopId={activeStop?.id} onStopSelect={selectStop} />
                {activeStop && (
                  <div className="active-stop-card">
                    <span className="active-stop-card__number">{safeActiveIndex + 1}</span>
                    <span><small>NOW EXPLORING</small><strong>{activeStop.name}</strong></span>
                    <ChevronRight size={17} />
                  </div>
                )}
              </div>
              <ReplayControls stops={trip.stops} activeIndex={safeActiveIndex} onActiveIndexChange={setActiveIndex} />
            </div>

            <aside className="timeline-panel">
              <header>
                <div><span className="eyebrow">YOUR TIMELINE</span><h2>Day by day</h2></div>
                {canEditStops && <button className="icon-button" type="button" aria-label="Add stop" onClick={openNewStop}>+</button>}
              </header>
              <div className="timeline-scroll">
                {!trip.stops.length && (
                  <div className="timeline-empty">
                    <MapPin size={24} />
                    <strong>No places yet</strong>
                    <span>Build the story one stop at a time.</span>
                    {canEditStops && <button className="button button--coral" type="button" onClick={openNewStop}>Add first place</button>}
                  </div>
                )}
                {groupedStops.map(({ day, stops }) => (
                  <section className="timeline-day" key={day}>
                    <div className="timeline-day__heading">
                      <span>DAY {day}</span>
                      <small>{stops[0]?.dateLabel}</small>
                    </div>
                    {stops.map((stop) => {
                      const index = trip.stops.findIndex(({ id }) => id === stop.id);
                      const active = index === safeActiveIndex;
                      return (
                        <div className="timeline-stop-row" key={stop.id}>
                          <button
                            type="button"
                            className={`timeline-stop${active ? ' timeline-stop--active' : ''}`}
                            onClick={() => setActiveIndex(index)}
                          >
                            <span className="timeline-stop__time">{stop.arrivalTime}</span>
                            <span className="timeline-stop__dot">{categoryEmoji[stop.category]}</span>
                            <span className="timeline-stop__copy">
                              <strong>{stop.name}</strong>
                              <small>{stop.category.toLowerCase()}</small>
                            </span>
                            {stop.rating && <span className="timeline-stop__rating"><Star size={12} fill="currentColor" /> {stop.rating}</span>}
                          </button>
                          {canEditStops && (
                            <div className="timeline-stop__actions">
                              <button type="button" aria-label={`Edit ${stop.name}`} onClick={() => openEditStop(stop)}><Pencil size={13} /></button>
                              <button type="button" aria-label={`Delete ${stop.name}`} disabled={deleteStop.isPending} onClick={() => void confirmDeleteStop(stop)}><Trash2 size={13} /></button>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </section>
                ))}
              </div>
            </aside>
          </section>

          <section className="trip-insights">
            <article><span className="insight-icon insight-icon--coral"><MapPin size={19} /></span><span><small>PLACES SAVED</small><strong>{trip.stops.length}</strong></span></article>
            <article><span className="insight-icon insight-icon--blue"><Camera size={19} /></span><span><small>MEMORIES</small><strong>{trip.photos || '—'}</strong></span></article>
            <article><span className="insight-icon insight-icon--sage"><CircleDollarSign size={19} /></span><span><small>TRIP BUDGET</small><strong>€{trip.spent}</strong></span></article>
            <article><span className="insight-icon insight-icon--sand"><Star size={19} /></span><span><small>TRIP RATING</small><strong>9.2</strong></span></article>
          </section>
        </>
      ) : activeTab === 'Photos' ? (
        <PhotosPanel trip={trip} stops={trip.stops} />
      ) : activeTab === 'Members' ? (
        <TripMembersPanel tripId={trip.id} currentUserRole={trip.currentUserRole} />
      ) : (
        <section className="tab-placeholder">
          <span>{activeTab === 'Expenses' ? <CircleDollarSign size={26} /> : <Compass size={26} />}</span>
          <h2>{activeTab} are coming next</h2>
          <p>This first slice focuses on the journey map, timeline and replay. {activeTab} will connect to the same secure trip permissions.</p>
        </section>
      )}

      <EditTripDialog open={editTripOpen} trip={trip} onClose={() => setEditTripOpen(false)} />
      <TripStopDialog open={stopDialogOpen} trip={trip} stop={selectedStop} onClose={() => setStopDialogOpen(false)} />
    </div>
  );
}
