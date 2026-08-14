import {
  ArrowRight,
  CalendarDays,
  Camera,
  Compass,
  Globe2,
  MapPin,
  Navigation,
  Route,
  Star,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { TripMap } from '../features/map/components/TripMap';
import { usePublicTrip } from '../features/publictrip/hooks/usePublicTrip';
import type { PublicTripRatingBreakdown } from '../features/publictrip/types';
import { formatDateRange } from '../utils/date';

function countryFlag(countryCode: string) {
  return countryCode.toUpperCase().replace(/./g, (letter) =>
    String.fromCodePoint(127397 + letter.charCodeAt(0)));
}

const ratingLabels: Array<[keyof PublicTripRatingBreakdown, string]> = [
  ['food', 'Food'],
  ['culture', 'Culture'],
  ['nature', 'Nature'],
  ['walkability', 'Walkability'],
  ['relaxation', 'Relaxation'],
  ['valueForMoney', 'Value'],
];

export function PublicTripPage() {
  const { publicSlug = '' } = useParams();
  const tripQuery = usePublicTrip(publicSlug);
  const [selectedStopId, setSelectedStopId] = useState('');
  const trip = tripQuery.data;
  const groupedStops = useMemo(() => {
    if (!trip) return [];
    return [...new Set(trip.stops.map(({ day }) => day))].map((day) => ({
      day,
      stops: trip.stops.filter((stop) => stop.day === day),
    }));
  }, [trip]);

  if (tripQuery.isLoading) {
    return <div className="public-trip-loading"><span /><span /><span /></div>;
  }

  if (!trip || tripQuery.error) {
    return (
      <main className="public-trip-not-found">
        <span><Compass size={31} /></span>
        <small>THE TRAIL ENDS HERE</small>
        <h1>This travel story is no longer public.</h1>
        <p>The owner may have made it private or changed the link.</p>
        <Link className="button button--dark" to="/login">Create your own memory map</Link>
      </main>
    );
  }

  const heroPhoto = trip.coverImageUrl || trip.photos.find(({ signedUrl }) => signedUrl)?.signedUrl;
  const selectedStop = trip.stops.find(({ id }) => id === selectedStopId) ?? trip.stops[0];
  const publicPhotos = trip.photos.filter((photo) => photo.signedUrl);

  return (
    <div className="public-trip-page">
      <header className="public-trip-nav">
        <Link className="public-trip-brand" to="/login"><span><Globe2 size={20} /></span><strong>Travel<small>MEMORY MAP</small></strong></Link>
        <span>Shared travel story</span>
        <Link to="/login">Map your journeys <ArrowRight size={14} /></Link>
      </header>

      <main>
        <section className={`public-trip-hero${heroPhoto ? ' public-trip-hero--photo' : ''}`}>
          {heroPhoto && <img src={heroPhoto} alt="" aria-hidden="true" />}
          <div className="public-trip-hero__art" aria-hidden="true"><span /><span /><span /></div>
          <div className="public-trip-hero__content">
            <span className="eyebrow eyebrow--light">A PUBLIC JOURNEY · {trip.status}</span>
            <div className="public-trip-hero__location"><span>{countryFlag(trip.countryCode)}</span>{trip.city}, {trip.country}</div>
            <h1>{trip.title}</h1>
            <p>{trip.description || 'A journey told one place and memory at a time.'}</p>
            <div>
              <span><CalendarDays size={16} /> {formatDateRange(trip.startDate, trip.endDate)}</span>
              <span><MapPin size={16} /> {trip.statistics.placeCount} places</span>
            </div>
          </div>
        </section>

        <section className="public-trip-stats" aria-label="Trip statistics">
          <article><CalendarDays size={18} /><strong>{trip.statistics.travelDays}</strong><span>travel days</span></article>
          <article><Route size={18} /><strong>{trip.statistics.placeCount}</strong><span>places visited</span></article>
          <article><Navigation size={18} /><strong>{trip.statistics.totalDistanceKm}</strong><span>route kilometres</span></article>
          <article><Camera size={18} /><strong>{trip.statistics.selectedPhotoCount}</strong><span>shared memories</span></article>
        </section>

        <section className="public-trip-story">
          <div className="public-trip-map-card">
            <header><div><span className="eyebrow">THE ROUTE</span><h2>Follow the journey</h2></div><small>{trip.routeSource === 'ROUTED' ? 'Road route' : 'Trip route'} · {trip.statistics.totalDistanceKm} km</small></header>
            <div className="public-trip-map">
              {trip.stops.length ? (
                <TripMap
                  stops={trip.stops}
                  activeStopId={selectedStop?.id}
                  routeProgress={Math.max(0, trip.stops.length - 1)}
                  routeSegments={trip.routeSegments}
                  onStopSelect={setSelectedStopId}
                />
              ) : <div className="public-trip-map__empty"><MapPin size={28} /><span>No places were added to this story.</span></div>}
              {selectedStop && <div className="public-trip-map__stop"><span>{selectedStop.day}</span><div><small>DAY {selectedStop.day}</small><strong>{selectedStop.name}</strong></div></div>}
            </div>
          </div>

          <aside className="public-trip-timeline">
            <span className="eyebrow">DAY BY DAY</span>
            <h2>The places that shaped it</h2>
            {groupedStops.length ? groupedStops.map(({ day, stops }) => (
              <section key={day}>
                <header><strong>DAY {day}</strong><small>{stops[0]?.dateLabel}</small></header>
                {stops.map((stop) => (
                  <button key={stop.id} type="button" className={selectedStop?.id === stop.id ? 'is-active' : ''} onClick={() => setSelectedStopId(stop.id)}>
                    <span>{stop.arrivalTime}</span><i /><div><strong>{stop.name}</strong><small>{stop.category.toLowerCase()}</small></div>{stop.rating && <em><Star size={10} fill="currentColor" /> {stop.rating}</em>}
                  </button>
                ))}
              </section>
            )) : <p className="public-trip-timeline__empty">This journey has no public timeline yet.</p>}
          </aside>
        </section>

        <section className="public-trip-memories">
          <header><div><span className="eyebrow">SELECTED MEMORIES</span><h2>A few moments from the road</h2></div><span><Camera size={15} /> {publicPhotos.length} photos</span></header>
          {publicPhotos.length ? (
            <div className="public-trip-photo-grid">
              {publicPhotos.map((photo, index) => (
                <figure key={photo.id} className={index === 0 ? 'public-trip-photo--featured' : ''}>
                  <img src={photo.signedUrl ?? ''} alt={photo.caption || `Travel memory ${index + 1}`} loading="lazy" />
                  {photo.caption && <figcaption>{photo.caption}</figcaption>}
                </figure>
              ))}
            </div>
          ) : <div className="public-trip-memories__empty"><Camera size={25} /><p>No photos were selected for the public story.</p></div>}
        </section>

        <section className="public-trip-rating">
          <div className="public-trip-rating__score"><span className="eyebrow eyebrow--light">TRAVELLER RATING</span><strong>{trip.rating.averageScore ?? '—'}<small>/10</small></strong><p>{trip.rating.ratingCount} {trip.rating.ratingCount === 1 ? 'traveller' : 'travellers'} rated this journey</p></div>
          <div className="public-trip-rating__breakdown">
            {ratingLabels.map(([key, label]) => {
              const value = trip.rating.averages[key];
              return <div key={key}><span>{label}</span><i><b style={{ width: `${(value ?? 0) * 10}%` }} /></i><strong>{value ?? '—'}</strong></div>;
            })}
          </div>
          <div className="public-trip-rating__return"><Compass size={23} /><strong>{trip.rating.returnIntent.yes}</strong><span>would return</span><small>{trip.rating.returnIntent.maybe} maybe · {trip.rating.returnIntent.no} no</small></div>
        </section>
      </main>

      <footer className="public-trip-footer"><Globe2 size={18} /><span>Made with Travel Memory Map</span><Link to="/login">Start your own story <ArrowRight size={14} /></Link></footer>
    </div>
  );
}
