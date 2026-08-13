import {
  ArrowLeftRight,
  CalendarDays,
  Camera,
  CircleDollarSign,
  MapPin,
  Scale,
  Sparkles,
  Star,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTripComparison } from '../features/comparison/hooks/useComparison';
import type { TripComparisonSide } from '../features/comparison/types';
import type { RatingAverages } from '../features/ratings/types';
import { useTrips } from '../features/trips/hooks/useTrips';
import { formatDateRange } from '../utils/date';

type RatingField = keyof RatingAverages;

const dimensions: { field: RatingField; label: string }[] = [
  { field: 'food', label: 'Food' },
  { field: 'valueForMoney', label: 'Value' },
  { field: 'nightlife', label: 'Nightlife' },
  { field: 'culture', label: 'Culture' },
  { field: 'nature', label: 'Nature' },
  { field: 'walkability', label: 'Walkability' },
  { field: 'crowds', label: 'Crowds' },
  { field: 'relaxation', label: 'Relaxation' },
];

function countryFlag(countryCode: string) {
  return countryCode.toUpperCase().replace(/./g, (letter) =>
    String.fromCodePoint(127397 + letter.charCodeAt(0)));
}

function formatScore(score: number | null) {
  return score == null ? '—' : score.toFixed(1);
}

function ComparisonTripHeader({ trip, side }: { trip: TripComparisonSide; side: 'left' | 'right' }) {
  return (
    <article className={`comparison-trip comparison-trip--${side}`}>
      <span className="comparison-trip__flag" aria-hidden="true">{countryFlag(trip.countryCode)}</span>
      <div>
        <small>{trip.country} · {trip.status.toLowerCase()}</small>
        <h2>{trip.title}</h2>
        <p>{trip.city} · {formatDateRange(trip.startDate, trip.endDate)}</p>
      </div>
      <span className="comparison-trip__score"><Star size={15} fill="currentColor" /> {formatScore(trip.averageScore)}</span>
    </article>
  );
}

function RatingComparison({ left, right }: { left: TripComparisonSide; right: TripComparisonSide }) {
  return (
    <section className="comparison-panel comparison-ratings">
      <div className="comparison-panel__heading">
        <div><span className="eyebrow">HEAD TO HEAD</span><h2>What made each trip shine</h2></div>
        <small>Community average · 1–10</small>
      </div>
      <div className="comparison-rating-head"><strong>{left.city}</strong><span>Category</span><strong>{right.city}</strong></div>
      <div className="comparison-rating-list">
        {dimensions.map(({ field, label }) => {
          const leftScore = left.ratings[field];
          const rightScore = right.ratings[field];
          return (
            <div className="comparison-rating-row" key={field}>
              <div className={leftScore != null && rightScore != null && leftScore > rightScore ? 'is-winner' : ''}>
                <b>{formatScore(leftScore)}</b>
                <i><span style={{ width: `${(leftScore ?? 0) * 10}%` }} /></i>
              </div>
              <strong>{label}</strong>
              <div className={leftScore != null && rightScore != null && rightScore > leftScore ? 'is-winner' : ''}>
                <i><span style={{ width: `${(rightScore ?? 0) * 10}%` }} /></i>
                <b>{formatScore(rightScore)}</b>
              </div>
            </div>
          );
        })}
      </div>
      {left.ratingCount === 0 || right.ratingCount === 0 ? (
        <p className="comparison-note">Trips without ratings stay visible, but their category scores need at least one traveller rating.</p>
      ) : null}
    </section>
  );
}

function JourneyFacts({ left, right }: { left: TripComparisonSide; right: TripComparisonSide }) {
  const facts = [
    { label: 'Travel days', icon: CalendarDays, left: left.travelDays, right: right.travelDays },
    { label: 'Places saved', icon: MapPin, left: left.stopCount, right: right.stopCount },
    { label: 'Memories', icon: Camera, left: left.photoCount, right: right.photoCount },
    { label: 'Would return', icon: Sparkles, left: left.wouldReturnYesPercent, right: right.wouldReturnYesPercent, suffix: '%' },
  ];
  return (
    <section className="comparison-panel comparison-facts">
      <div className="comparison-panel__heading">
        <div><span className="eyebrow">JOURNEY FOOTPRINT</span><h2>Beyond the ratings</h2></div>
      </div>
      <div className="comparison-fact-list">
        {facts.map(({ label, icon: Icon, left: leftValue, right: rightValue, suffix = '' }) => (
          <div key={label}>
            <strong>{leftValue == null ? '—' : `${leftValue}${suffix}`}</strong>
            <span><Icon size={15} /> {label}</span>
            <strong>{rightValue == null ? '—' : `${rightValue}${suffix}`}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}

function SpendingComparison({ left, right }: { left: TripComparisonSide; right: TripComparisonSide }) {
  const currencies = [...new Set([...left.spending, ...right.spending].map((spending) => spending.currency))].sort();
  const money = (value: number, currency: string) => new Intl.NumberFormat('en', {
    style: 'currency', currency, maximumFractionDigits: 2,
  }).format(value);
  return (
    <section className="comparison-panel comparison-spending">
      <div className="comparison-panel__heading">
        <div><span className="eyebrow">TRIP COST</span><h2>Budget comparison</h2></div>
        <small>Each currency stays separate</small>
      </div>
      {currencies.length ? (
        <div className="comparison-spending-list">
          {currencies.map((currency) => {
            const leftSpend = left.spending.find((item) => item.currency === currency);
            const rightSpend = right.spending.find((item) => item.currency === currency);
            return (
              <article key={currency}>
                <div><small>{left.city}</small><strong>{leftSpend ? money(leftSpend.total, currency) : '—'}</strong><span>{leftSpend ? `${money(leftSpend.costPerDay, currency)} / day` : 'No expenses'}</span></div>
                <span className="comparison-spending__currency"><CircleDollarSign size={16} /> {currency}</span>
                <div><small>{right.city}</small><strong>{rightSpend ? money(rightSpend.total, currency) : '—'}</strong><span>{rightSpend ? `${money(rightSpend.costPerDay, currency)} / day` : 'No expenses'}</span></div>
              </article>
            );
          })}
        </div>
      ) : <p className="comparison-note">Add trip expenses to unlock the budget comparison.</p>}
    </section>
  );
}

export function TripComparisonPage() {
  const tripsQuery = useTrips();
  const trips = useMemo(
    () => [...(tripsQuery.data ?? [])].sort((left, right) =>
      Number(right.status === 'COMPLETED') - Number(left.status === 'COMPLETED')),
    [tripsQuery.data],
  );
  const [leftTripId, setLeftTripId] = useState('');
  const [rightTripId, setRightTripId] = useState('');
  const selectedLeftTripId = trips.some((trip) => trip.id === leftTripId)
    ? leftTripId
    : trips[0]?.id ?? '';
  const selectedRightTripId = trips.some((trip) => trip.id === rightTripId) && rightTripId !== selectedLeftTripId
    ? rightTripId
    : trips.find((trip) => trip.id !== selectedLeftTripId)?.id ?? '';
  const comparison = useTripComparison(selectedLeftTripId, selectedRightTripId);
  const swap = () => {
    setLeftTripId(selectedRightTripId);
    setRightTripId(selectedLeftTripId);
  };

  if (!tripsQuery.isLoading && trips.length < 2) {
    return (
      <div className="page page--comparison">
        <header className="page-header"><div><span className="eyebrow">TRIP COMPARISON</span><h1>Two journeys, one story</h1></div></header>
        <div className="empty-state"><span><Scale size={28} /></span><h2>Two trips are needed</h2><p>Create another journey and come back to see how they compare.</p><Link className="button button--dark" to="/trips">View my trips</Link></div>
      </div>
    );
  }

  return (
    <div className="page page--comparison">
      <header className="page-header comparison-page-header">
        <div><span className="eyebrow">TRAVEL INTELLIGENCE</span><h1>Trip comparison</h1><p>See what made each journey memorable — side by side.</p></div>
        <span className="comparison-page-header__mark"><Scale size={26} /></span>
      </header>

      <section className="comparison-picker" aria-label="Choose trips to compare">
        <label><span>First journey</span><select value={selectedLeftTripId} onChange={(event) => setLeftTripId(event.target.value)}>{trips.map((trip) => <option key={trip.id} value={trip.id} disabled={trip.id === selectedRightTripId}>{trip.title} · {trip.city}</option>)}</select></label>
        <button type="button" aria-label="Swap compared trips" onClick={swap} disabled={!selectedLeftTripId || !selectedRightTripId}><ArrowLeftRight size={18} /></button>
        <label><span>Second journey</span><select value={selectedRightTripId} onChange={(event) => setRightTripId(event.target.value)}>{trips.map((trip) => <option key={trip.id} value={trip.id} disabled={trip.id === selectedLeftTripId}>{trip.title} · {trip.city}</option>)}</select></label>
      </section>

      {comparison.isLoading || tripsQuery.isLoading ? (
        <div className="comparison-loading"><span /><span /><span /></div>
      ) : comparison.error || !comparison.data ? (
        <div className="empty-state"><span><Scale size={28} /></span><h2>Comparison unavailable</h2><p>{comparison.error?.message ?? 'Choose two different trips and try again.'}</p><button className="button button--dark" type="button" onClick={() => void comparison.refetch()}>Try again</button></div>
      ) : (
        <>
          <section className="comparison-matchup">
            <ComparisonTripHeader trip={comparison.data.left} side="left" />
            <span className="comparison-matchup__versus">VS</span>
            <ComparisonTripHeader trip={comparison.data.right} side="right" />
          </section>
          <div className="comparison-grid">
            <RatingComparison left={comparison.data.left} right={comparison.data.right} />
            <JourneyFacts left={comparison.data.left} right={comparison.data.right} />
          </div>
          <SpendingComparison left={comparison.data.left} right={comparison.data.right} />
        </>
      )}
    </div>
  );
}
