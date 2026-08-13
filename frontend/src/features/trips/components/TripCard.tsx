import { ArrowUpRight, Camera, LockKeyhole, MapPin, UsersRound } from 'lucide-react';
import { Link } from 'react-router-dom';
import { formatDateRange } from '../../../utils/date';
import type { Trip } from '../types';
import { AvatarStack } from '../../../components/ui/AvatarStack';

interface TripCardProps {
  trip: Trip;
  featured?: boolean;
}

export function TripCard({ trip, featured = false }: TripCardProps) {
  return (
    <Link
      to={`/trips/${trip.id}`}
      className={`trip-card trip-card--${trip.accent}${featured ? ' trip-card--featured' : ''}`}
    >
      <div className="trip-card__art" aria-hidden="true">
        <span className="trip-card__sun" />
        <span className="trip-card__land trip-card__land--back" />
        <span className="trip-card__land trip-card__land--front" />
        <span className="trip-card__route" />
        <span className="trip-card__pin" />
      </div>

      <div className="trip-card__content">
        <div className="trip-card__topline">
          <span className="eyebrow">{trip.status === 'UPCOMING' ? 'UPCOMING' : `${trip.city.toUpperCase()} · ${trip.startDate.slice(0, 4)}`}</span>
          <span className="trip-card__arrow">
            <ArrowUpRight size={18} />
          </span>
        </div>
        <h3>{trip.title}</h3>
        <p className="trip-card__location">
          <MapPin size={15} />
          {trip.city}, {trip.country}
        </p>
        <p className="trip-card__dates">{formatDateRange(trip.startDate, trip.endDate)}</p>

        {trip.progress !== undefined && featured && (
          <div className="trip-progress">
            <div className="trip-progress__labels">
              <span>Trip planning</span>
              <strong>{trip.progress}%</strong>
            </div>
            <span className="trip-progress__track">
              <span style={{ width: `${trip.progress}%` }} />
            </span>
          </div>
        )}

        <div className="trip-card__footer">
          <AvatarStack count={trip.memberCount} compact />
          <span className="trip-card__meta">
            {trip.visibility === 'PRIVATE' && <LockKeyhole size={14} />}
            <UsersRound size={14} /> {trip.memberCount}
            <Camera size={14} /> {trip.photos}
          </span>
        </div>
      </div>
    </Link>
  );
}
