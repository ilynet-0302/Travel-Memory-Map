import { RotateCcw, SlidersHorizontal, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { TripSearchFilters } from '../types';

interface TripFiltersDialogProps {
  filters: TripSearchFilters;
  onApply: (filters: TripSearchFilters) => void;
  onClose: () => void;
}

const dnaOptions = [
  ['EXPLORER', 'Urban explorer'],
  ['FOODIE', 'Food hunter'],
  ['CULTURE', 'Culture seeker'],
  ['NIGHTLIFE', 'Night owl'],
  ['RELAXATION', 'Relaxed traveler'],
  ['NATURE', 'Nature wanderer'],
  ['ADVENTURE', 'Adventure traveler'],
] as const;

const emptyValue = (value: number | undefined) => value === undefined ? '' : String(value);
const numberValue = (value: string) => value === '' ? undefined : Number(value);

export function TripFiltersDialog({ filters, onApply, onClose }: TripFiltersDialogProps) {
  const [draft, setDraft] = useState({
    status: filters.status ?? '',
    year: emptyValue(filters.year),
    country: filters.country ?? '',
    city: filters.city ?? '',
    minRating: emptyValue(filters.minRating),
    minPrice: emptyValue(filters.minPrice),
    maxPrice: emptyValue(filters.maxPrice),
    minDuration: emptyValue(filters.minDuration),
    maxDuration: emptyValue(filters.maxDuration),
    dna: filters.dna ?? '',
    relationship: filters.relationship ?? 'ALL',
    sort: filters.sort ?? 'START_DESC',
    currency: filters.currency ?? 'EUR',
  });
  const [error, setError] = useState('');

  useEffect(() => {
    document.body.classList.add('is-modal-open');
    const closeOnEscape = (event: KeyboardEvent) => event.key === 'Escape' && onClose();
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.body.classList.remove('is-modal-open');
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [onClose]);

  function update(name: keyof typeof draft, value: string) {
    setDraft((current) => ({ ...current, [name]: value }));
  }

  function apply() {
    const minPrice = numberValue(draft.minPrice);
    const maxPrice = numberValue(draft.maxPrice);
    const minDuration = numberValue(draft.minDuration);
    const maxDuration = numberValue(draft.maxDuration);
    if (minPrice !== undefined && maxPrice !== undefined && minPrice > maxPrice) {
      setError('Minimum price cannot be higher than maximum price.');
      return;
    }
    if (minDuration !== undefined && maxDuration !== undefined && minDuration > maxDuration) {
      setError('Minimum duration cannot be longer than maximum duration.');
      return;
    }
    onApply({
      status: draft.status || undefined,
      year: numberValue(draft.year),
      country: draft.country.trim() || undefined,
      city: draft.city.trim() || undefined,
      minRating: numberValue(draft.minRating),
      minPrice,
      maxPrice,
      minDuration,
      maxDuration,
      dna: draft.dna || undefined,
      relationship: draft.relationship === 'ALL' ? undefined : draft.relationship,
      sort: draft.sort,
      currency: draft.currency,
    } as TripSearchFilters);
  }

  function reset() {
    onApply({ sort: 'START_DESC', currency: 'EUR' });
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card trip-filters-dialog" role="dialog" aria-modal="true" aria-labelledby="trip-filters-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-card__header">
          <div>
            <span className="eyebrow">FIND YOUR STORY</span>
            <h2 id="trip-filters-title"><SlidersHorizontal size={24} /> Advanced filters</h2>
          </div>
          <button className="icon-button" type="button" aria-label="Close filters" onClick={onClose}><X size={20} /></button>
        </header>

        <div className="trip-form trip-filters-form">
          <label className="field"><span>Status</span><select value={draft.status} onChange={(event) => update('status', event.target.value)}><option value="">Any status</option><option value="UPCOMING">Upcoming</option><option value="ACTIVE">Active</option><option value="COMPLETED">Completed</option></select></label>
          <label className="field"><span>Year</span><input type="number" min="1900" max="2200" placeholder="e.g. 2026" value={draft.year} onChange={(event) => update('year', event.target.value)} /></label>
          <label className="field"><span>Country</span><input placeholder="e.g. Italy" value={draft.country} onChange={(event) => update('country', event.target.value)} /></label>
          <label className="field"><span>City</span><input placeholder="e.g. Rome" value={draft.city} onChange={(event) => update('city', event.target.value)} /></label>
          <label className="field"><span>Minimum rating</span><select value={draft.minRating} onChange={(event) => update('minRating', event.target.value)}><option value="">Any rating</option>{[10, 9, 8, 7, 6, 5, 4, 3, 2, 1].map((rating) => <option key={rating} value={rating}>{rating}+ / 10</option>)}</select></label>
          <label className="field"><span>Trip DNA</span><select value={draft.dna} onChange={(event) => update('dna', event.target.value)}><option value="">Any personality</option>{dnaOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>

          <fieldset className="filter-fieldset field--full">
            <legend>Price range</legend>
            <label className="field"><span>Minimum</span><input type="number" min="0" step="0.01" placeholder="0" value={draft.minPrice} onChange={(event) => update('minPrice', event.target.value)} /></label>
            <label className="field"><span>Maximum</span><input type="number" min="0" step="0.01" placeholder="No limit" value={draft.maxPrice} onChange={(event) => update('maxPrice', event.target.value)} /></label>
            <label className="field filter-currency"><span>Currency</span><select value={draft.currency} onChange={(event) => update('currency', event.target.value)}><option>EUR</option><option>BGN</option><option>USD</option><option>GBP</option></select></label>
          </fieldset>

          <fieldset className="filter-fieldset field--full filter-fieldset--duration">
            <legend>Duration in days</legend>
            <label className="field"><span>Minimum</span><input type="number" min="1" max="3650" placeholder="1" value={draft.minDuration} onChange={(event) => update('minDuration', event.target.value)} /></label>
            <label className="field"><span>Maximum</span><input type="number" min="1" max="3650" placeholder="No limit" value={draft.maxDuration} onChange={(event) => update('maxDuration', event.target.value)} /></label>
          </fieldset>

          <label className="field"><span>Your role</span><select value={draft.relationship} onChange={(event) => update('relationship', event.target.value)}><option value="ALL">Owner or shared</option><option value="OWNER">Owned by me</option><option value="SHARED">Shared with me</option></select></label>
          <label className="field"><span>Sort results</span><select value={draft.sort} onChange={(event) => update('sort', event.target.value)}><option value="START_DESC">Newest first</option><option value="START_ASC">Oldest first</option><option value="TITLE_ASC">Title A–Z</option><option value="RATING_DESC">Highest rated</option><option value="PRICE_DESC">Highest price</option><option value="DURATION_DESC">Longest first</option></select></label>

          {error && <p className="form-error">{error}</p>}
          <footer className="modal-card__footer field--full">
            <button className="button button--ghost filter-reset" type="button" onClick={reset}><RotateCcw size={16} /> Reset all</button>
            <button className="button button--ghost" type="button" onClick={onClose}>Cancel</button>
            <button className="button button--coral" type="button" onClick={apply}>Show journeys</button>
          </footer>
        </div>
      </section>
    </div>
  );
}
