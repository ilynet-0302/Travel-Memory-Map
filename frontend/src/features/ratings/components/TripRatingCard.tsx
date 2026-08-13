import { Star, X } from 'lucide-react';
import { useState } from 'react';
import { useRateTrip, useRemoveTripRating, useTripRating } from '../hooks/useRatings';

interface TripRatingCardProps {
  tripId: string;
}

export function TripRatingCard({ tripId }: TripRatingCardProps) {
  const rating = useTripRating(tripId);
  const rateTrip = useRateTrip(tripId);
  const removeRating = useRemoveTripRating(tripId);
  const [editing, setEditing] = useState(false);
  const summary = rating.data;
  const pending = rateTrip.isPending || removeRating.isPending;
  const error = rating.error ?? rateTrip.error ?? removeRating.error;

  const chooseScore = async (score: number) => {
    try {
      await rateTrip.mutateAsync(score);
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
          onClick={() => setEditing((open) => !open)}
        >
          {summary.currentUserScore ? `Yours: ${summary.currentUserScore}` : 'Rate'}
        </button>
      )}
      {editing && summary?.canRate && (
        <div className="trip-rating-card__editor">
          <div className="trip-rating-card__editor-heading">
            <span>How was this trip?</span>
            <button type="button" aria-label="Close rating editor" onClick={() => setEditing(false)}><X size={14} /></button>
          </div>
          <div className="trip-rating-scale" role="group" aria-label="Choose a trip rating from 1 to 10">
            {Array.from({ length: 10 }, (_, index) => index + 1).map((score) => (
              <button
                key={score}
                type="button"
                className={summary.currentUserScore === score ? 'is-selected' : ''}
                aria-label={`Rate this trip ${score} out of 10`}
                aria-pressed={summary.currentUserScore === score}
                disabled={pending}
                onClick={() => void chooseScore(score)}
              >
                {score}
              </button>
            ))}
          </div>
          <div className="trip-rating-card__legend"><span>Not for me</span><span>Unforgettable</span></div>
          {summary.currentUserScore && (
            <button className="trip-rating-card__remove" type="button" disabled={pending} onClick={() => void removeScore()}>
              Remove my rating
            </button>
          )}
          {error && <p role="alert">{error.message}</p>}
        </div>
      )}
    </article>
  );
}
