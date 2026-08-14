import {
  ArrowRight,
  Camera,
  ChevronLeft,
  ChevronRight,
  Coins,
  Compass,
  MapPin,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useOnThisDay } from '../hooks/useOnThisDay';
import type { OnThisDayMemory } from '../types';

function countryFlag(countryCode: string) {
  if (!/^[A-Z]{2}$/i.test(countryCode)) return '✦';
  return countryCode.toUpperCase().replace(/./g, (letter) =>
    String.fromCodePoint(127397 + letter.charCodeAt(0)));
}

function plural(value: number, singular: string, pluralForm = `${singular}s`) {
  return `${value} ${value === 1 ? singular : pluralForm}`;
}

function formatMoney(value: number, currency: string) {
  return new Intl.NumberFormat('en-GB', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(value);
}

function formatMemoryDate(value: string) {
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(`${value}T00:00:00Z`));
}

function MemoryDetails({ memory }: { memory: OnThisDayMemory }) {
  const firstSpending = memory.spending[0];
  return (
    <>
      {memory.heroPhotoUrl && (
        <img
          className="memory-card__photo"
          src={memory.heroPhotoUrl}
          alt=""
          aria-hidden="true"
        />
      )}
      <div className="memory-card__icon" aria-hidden="true">
        <span>{countryFlag(memory.countryCode)}</span>
      </div>
      <span className="eyebrow">ON THIS DAY</span>
      <small className="memory-card__date">{formatMemoryDate(memory.memoryDate)}</small>
      <h3>{memory.yearsAgo} {memory.yearsAgo === 1 ? 'year' : 'years'} ago, you were in <em>{memory.city}</em>.</h3>
      {memory.placeNames.length > 0 && (
        <p className="memory-card__places" title={memory.placeNames.join(', ')}>
          {memory.placeNames.slice(0, 2).join(' · ')}
          {memory.placeNames.length > 2 && ` +${memory.placeNames.length - 2}`}
        </p>
      )}
      <div className="memory-card__meta">
        <span><MapPin size={15} /> {plural(memory.placeCount, 'place')}</span>
        <span><Camera size={15} /> {plural(memory.photoCount, 'photo')}</span>
        {firstSpending && (
          <span title={memory.spending.length > 1 ? 'More currencies are recorded for this day' : undefined}>
            <Coins size={15} /> {formatMoney(firstSpending.totalSpent, firstSpending.currency)} spent
            {memory.spending.length > 1 && ' +'}
          </span>
        )}
      </div>
      <Link className="text-button memory-card__link" to={`/trips/${memory.tripId}`}>
        Relive the day <ArrowRight size={15} />
      </Link>
    </>
  );
}

export function OnThisDayCard() {
  const memoryQuery = useOnThisDay();
  const [activeIndex, setActiveIndex] = useState(0);
  const memories = memoryQuery.data?.memories ?? [];
  const safeIndex = memories.length ? Math.min(activeIndex, memories.length - 1) : 0;

  if (memoryQuery.isLoading) {
    return (
      <article className="memory-card memory-card--loading" aria-label="Loading memories">
        <span /><span /><span /><span />
      </article>
    );
  }

  if (memoryQuery.isError) {
    return (
      <article className="memory-card memory-card--empty">
        <div className="memory-card__icon"><RefreshCw size={18} /></div>
        <span className="eyebrow">ON THIS DAY</span>
        <h3>This memory is taking a little longer to find.</h3>
        <p>Your other travel stories are still safe.</p>
        <button className="text-button" type="button" onClick={() => memoryQuery.refetch()}>
          Try again <RefreshCw size={14} />
        </button>
      </article>
    );
  }

  if (!memories.length) {
    return (
      <article className="memory-card memory-card--empty">
        <div className="memory-card__icon"><Sparkles size={19} /></div>
        <span className="eyebrow">ON THIS DAY</span>
        <h3>No old footprints on today's date — yet.</h3>
        <p>Every journey adds another day worth rediscovering.</p>
        <Link className="text-button" to="/trips">Browse your stories <ArrowRight size={15} /></Link>
      </article>
    );
  }

  return (
    <article className={`memory-card${memories[safeIndex].heroPhotoUrl ? ' memory-card--with-photo' : ''}`}>
      <MemoryDetails memory={memories[safeIndex]} />
      {memories.length > 1 && (
        <div className="memory-card__navigation" aria-label="On this day memories">
          <button
            type="button"
            aria-label="Previous memory"
            onClick={() => setActiveIndex((safeIndex - 1 + memories.length) % memories.length)}
          >
            <ChevronLeft size={14} />
          </button>
          <span>{safeIndex + 1} / {memories.length}</span>
          <button
            type="button"
            aria-label="Next memory"
            onClick={() => setActiveIndex((safeIndex + 1) % memories.length)}
          >
            <ChevronRight size={14} />
          </button>
        </div>
      )}
      {!memories[safeIndex].heroPhotoUrl && <Compass className="memory-card__watermark" size={90} aria-hidden="true" />}
    </article>
  );
}
