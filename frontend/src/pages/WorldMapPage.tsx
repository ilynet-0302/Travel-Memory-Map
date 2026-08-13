import {
  CalendarDays,
  Camera,
  ChevronRight,
  CircleDollarSign,
  Flag,
  Globe2,
  MapPin,
  PlaneTakeoff,
  Route,
  Sparkles,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { WorldScratchMap } from '../features/worldmap/components/WorldScratchMap';
import { useWorldMap } from '../features/worldmap/hooks/useWorldMap';
import type { WorldMapCountry } from '../features/worldmap/types';
import { formatDateRange } from '../utils/date';

function countryFlag(countryCode: string) {
  return countryCode.toUpperCase().replace(/./g, (letter) =>
    String.fromCodePoint(127397 + letter.charCodeAt(0)));
}

function CountryDetails({ country, fallbackName }: { country?: WorldMapCountry; fallbackName?: string }) {
  if (!country) {
    return (
      <aside className="world-country-panel world-country-panel--empty">
        <span><Globe2 size={25} /></span>
        <small>UNEXPLORED</small>
        <h2>{fallbackName || 'Choose a country'}</h2>
        <p>{fallbackName ? 'No journeys here yet. Maybe this is where the next story begins.' : 'Select a country on the map to see its travel story.'}</p>
        {fallbackName && <Link className="button button--coral" to="/?create=1">Plan a trip</Link>}
      </aside>
    );
  }

  return (
    <aside className="world-country-panel">
      <header>
        <span className="world-country-panel__flag" aria-hidden="true">{countryFlag(country.countryCode)}</span>
        <div><small>{country.status === 'VISITED' ? 'VISITED COUNTRY' : 'NEXT DESTINATION'}</small><h2>{country.name}</h2></div>
        <span className={`world-country-status world-country-status--${country.status.toLowerCase()}`}>{country.status.toLowerCase()}</span>
      </header>

      <div className="world-country-stats">
        <div><PlaneTakeoff size={15} /><strong>{country.tripCount}</strong><span>trips</span></div>
        <div><MapPin size={15} /><strong>{country.cityCount}</strong><span>cities</span></div>
        <div><Route size={15} /><strong>{country.placeCount}</strong><span>places</span></div>
        <div><Camera size={15} /><strong>{country.photoCount}</strong><span>photos</span></div>
      </div>

      <section className="world-country-spending">
        <div><CircleDollarSign size={15} /><strong>Travel spend</strong></div>
        {country.spending.length ? country.spending.map((spending) => (
          <span key={spending.currency}>
            <small>{spending.currency}</small>
            <b>{new Intl.NumberFormat('en', { style: 'currency', currency: spending.currency }).format(spending.totalSpent)}</b>
          </span>
        )) : <p>No expenses logged yet.</p>}
      </section>

      <section className="world-country-trips">
        <div className="world-country-trips__heading"><strong>Journeys</strong><small>{country.visitedTripCount} visited · {country.plannedTripCount} planned</small></div>
        <div>
          {country.trips.map((trip) => (
            <Link to={`/trips/${trip.id}`} key={trip.id}>
              <span><strong>{trip.title}</strong><small><CalendarDays size={11} /> {trip.city} · {formatDateRange(trip.startDate, trip.endDate)}</small></span>
              <ChevronRight size={15} />
            </Link>
          ))}
        </div>
      </section>
    </aside>
  );
}

export function WorldMapPage() {
  const worldMap = useWorldMap();
  const [selectedCode, setSelectedCode] = useState('');
  const [selectedFallbackName, setSelectedFallbackName] = useState('');
  const countries = useMemo(() => worldMap.data?.countries ?? [], [worldMap.data?.countries]);
  const selectedCountry = useMemo(
    () => countries.find((country) => country.countryCode === selectedCode) ?? countries[0],
    [countries, selectedCode],
  );
  const effectiveSelectedCode = selectedCode || selectedCountry?.countryCode || '';
  const visitedPercentage = worldMap.data
    ? Math.round((worldMap.data.countriesVisited / worldMap.data.countriesTotal) * 100)
    : 0;

  const selectCountry = (countryCode: string, countryName: string) => {
    setSelectedCode(countryCode);
    setSelectedFallbackName(countryName);
  };

  if (worldMap.isLoading) {
    return <div className="page page--world-map"><div className="world-map-loading"><span /><span /></div></div>;
  }

  if (worldMap.error || !worldMap.data) {
    return (
      <div className="page page--world-map">
        <div className="empty-state"><span><Globe2 size={28} /></span><h2>We could not unfold your world</h2><p>{worldMap.error?.message ?? 'Try again in a moment.'}</p><button className="button button--dark" type="button" onClick={() => void worldMap.refetch()}>Try again</button></div>
      </div>
    );
  }

  return (
    <div className="page page--world-map">
      <header className="world-map-header">
        <div><span className="eyebrow">YOUR WORLD, UNFOLDED</span><h1>Every country holds a story.</h1><p>Scratch the map by travelling — your journeys fill it in automatically.</p></div>
        <div className="world-map-progress-card">
          <span><Globe2 size={19} /></span>
          <div><small>COUNTRIES VISITED</small><strong>{worldMap.data.countriesVisited} <em>/ {worldMap.data.countriesTotal}</em></strong><i><b style={{ width: `${Math.max(visitedPercentage, worldMap.data.countriesVisited ? 2 : 0)}%` }} /></i></div>
          <p>{worldMap.data.countriesPlanned} planned next</p>
        </div>
      </header>

      <div className="world-map-layout">
        <section className="world-map-stage">
          <WorldScratchMap
            countries={countries}
            selectedCountryCode={effectiveSelectedCode}
            onCountrySelect={selectCountry}
          />
          <div className="world-map-legend" aria-label="Map legend"><span><i className="is-visited" /> Visited</span><span><i className="is-planned" /> Planned</span><span><i /> Unexplored</span></div>
          <a className="world-map-data-credit" href="https://www.naturalearthdata.com/" target="_blank" rel="noreferrer">Boundaries · Natural Earth</a>
        </section>
        <CountryDetails
          country={countries.find((country) => country.countryCode === effectiveSelectedCode)}
          fallbackName={selectedFallbackName}
        />
      </div>

      <section className="world-map-country-list">
        <div className="section-heading"><div><span className="eyebrow">YOUR FOOTPRINT</span><h2>Countries in your story</h2></div><small>Choose a country to explore its journeys.</small></div>
        {countries.length ? (
          <div>
            {countries.map((country) => (
              <button key={country.countryCode} type="button" className={effectiveSelectedCode === country.countryCode ? 'is-selected' : ''} onClick={() => selectCountry(country.countryCode, country.name)}>
                <span className="world-map-country-list__flag">{countryFlag(country.countryCode)}</span>
                <span><strong>{country.name}</strong><small>{country.tripCount} {country.tripCount === 1 ? 'trip' : 'trips'} · {country.cityCount} {country.cityCount === 1 ? 'city' : 'cities'}</small></span>
                {country.status === 'PLANNED' ? <Sparkles size={15} /> : <Flag size={15} />}
              </button>
            ))}
          </div>
        ) : (
          <div className="world-map-first-trip"><Globe2 size={25} /><span><strong>Your world is waiting</strong><small>Create a trip and your first country will appear here.</small></span><Link className="button button--coral" to="/?create=1">Plan first trip</Link></div>
        )}
      </section>
    </div>
  );
}
