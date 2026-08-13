import { Save, Star, X } from 'lucide-react';
import { useState } from 'react';
import { useRateTrip, useRemoveTripRating, useTripRating } from '../hooks/useRatings';
import type { TripRatingInput, WouldReturn } from '../types';

interface TripRatingCardProps {
  tripId: string;
}

type ScoreField = Exclude<keyof TripRatingInput, 'wouldReturn'>;

const dimensions: { field: ScoreField; label: string }[] = [
  { field: 'food', label: 'Food' },
  { field: 'nightlife', label: 'Nightlife' },
  { field: 'culture', label: 'Culture' },
  { field: 'nature', label: 'Nature' },
  { field: 'walkability', label: 'Walkability' },
  { field: 'valueForMoney', label: 'Value for money' },
  { field: 'crowds', label: 'Crowds' },
  { field: 'relaxation', label: 'Relaxation' },
];

const defaultRating: TripRatingInput = {
  food: 7,
  nightlife: 7,
  culture: 7,
  nature: 7,
  walkability: 7,
  valueForMoney: 7,
  crowds: 7,
  relaxation: 7,
  wouldReturn: 'MAYBE',
};

const returnOptions: { value: WouldReturn; label: string }[] = [
  { value: 'YES', label: 'Yes' },
  { value: 'MAYBE', label: 'Maybe' },
  { value: 'NO', label: 'No' },
];

export function TripRatingCard({ tripId }: TripRatingCardProps) {
  const rating = useTripRating(tripId);
  const rateTrip = useRateTrip(tripId);
  const removeRating = useRemoveTripRating(tripId);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState<TripRatingInput>(defaultRating);
  const summary = rating.data;
  const pending = rateTrip.isPending || removeRating.isPending;
  const error = rating.error ?? rateTrip.error ?? removeRating.error;

  const openEditor = () => {
    if (editing) {
      setEditing(false);
      return;
    }
    const current = summary?.currentUserRating;
    setDraft(current ? {
      food: current.food,
      nightlife: current.nightlife,
      culture: current.culture,
      nature: current.nature,
      walkability: current.walkability,
      valueForMoney: current.valueForMoney,
      crowds: current.crowds,
      relaxation: current.relaxation,
      wouldReturn: current.wouldReturn,
    } : defaultRating);
    setEditing(true);
  };

  const saveRating = async () => {
    try {
      await rateTrip.mutateAsync(draft);
      setEditing(false);
    } catch {
      return;
    }
  };

  const removeScore = async () => {
    try {
      await removeRating.mutateAsync();
      setEditing(false);
    } catch {
      return;
    }
  };

  const returnVotes = summary?.returnIntent;

  return (
    <article className={`trip-rating-card${editing ? ' trip-rating-card--editing' : ''}`}>
      <span className="insight-icon insight-icon--sand"><Star size={19} /></span>
      <span className="trip-rating-card__value">
        <small>TRIP RATING</small>
        <strong>{rating.isLoading ? '…' : summary?.averageScore?.toFixed(1) ?? '—'}</strong>
        {!!summary?.ratingCount && <em>{summary.ratingCount} {summary.ratingCount === 1 ? 'rating' : 'ratings'}</em>}
      </span>
      {summary?.canRate && (
        <button
          className="trip-rating-card__trigger"
          type="button"
          aria-expanded={editing}
          onClick={openEditor}
        >
          {summary.currentUserRating ? `Yours: ${summary.currentUserRating.overallScore}` : 'Rate'}
        </button>
      )}
      {editing && summary?.canRate && (
        <div className="trip-rating-card__editor">
          <div className="trip-rating-card__editor-heading">
            <span>Rate your experience</span>
            <button type="button" aria-label="Close rating editor" onClick={() => setEditing(false)}><X size={15} /></button>
          </div>

          <div className="trip-rating-dimensions">
            {dimensions.map(({ field, label }) => (
              <label className="trip-rating-dimension" key={field}>
                <span>
                  <strong>{label}</strong>
                  <small>Community {summary.averages[field]?.toFixed(1) ?? '—'}</small>
                </span>
                <input
                  type="range"
                  min="1"
                  max="10"
                  step="1"
                  value={draft[field]}
                  disabled={pending}
                  aria-label={`${label} rating`}
                  onChange={(event) => setDraft((current) => ({
                    ...current,
                    [field]: Number(event.target.value),
                  }))}
                />
                <output>{draft[field]}</output>
              </label>
            ))}
          </div>

          <fieldset className="trip-rating-return">
            <legend>Would you return?</legend>
            <div>
              {returnOptions.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  className={draft.wouldReturn === option.value ? 'is-selected' : ''}
                  aria-pressed={draft.wouldReturn === option.value}
                  disabled={pending}
                  onClick={() => setDraft((current) => ({ ...current, wouldReturn: option.value }))}
                >
                  {option.label}
                </button>
              ))}
            </div>
            {!!summary.ratingCount && returnVotes && (
              <small>{returnVotes.yes} of {summary.ratingCount} travellers would return</small>
            )}
          </fieldset>

          <div className="trip-rating-card__actions">
            {summary.currentUserRating && (
              <button className="trip-rating-card__remove" type="button" disabled={pending} onClick={() => void removeScore()}>
                Remove my rating
              </button>
            )}
            <button className="trip-rating-card__save" type="button" disabled={pending} onClick={() => void saveRating()}>
              <Save size={13} /> {pending ? 'Saving…' : 'Save rating'}
            </button>
          </div>
          {error && <p role="alert">{error.message}</p>}
        </div>
      )}
    </article>
  );
}
