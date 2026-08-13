import { zodResolver } from '@hookform/resolvers/zod';
import { CalendarDays, LockKeyhole, MapPin, X } from 'lucide-react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useCreateTrip } from '../hooks/useTrips';
import type { Trip } from '../types';

const tripSchema = z
  .object({
    title: z.string().trim().min(3, 'Give your trip a memorable title.'),
    country: z.string().trim().min(2, 'Country is required.'),
    city: z.string().trim().min(2, 'City is required.'),
    startDate: z.string().min(1, 'Start date is required.'),
    endDate: z.string().min(1, 'End date is required.'),
    visibility: z.enum(['PRIVATE', 'PUBLIC']),
  })
  .refine(({ startDate, endDate }) => endDate >= startDate, {
    path: ['endDate'],
    message: 'End date must be on or after the start date.',
  });

type TripForm = z.infer<typeof tripSchema>;

interface CreateTripDialogProps {
  open: boolean;
  onClose: () => void;
  onCreated: (trip: Trip) => void;
}

export function CreateTripDialog({ open, onClose, onCreated }: CreateTripDialogProps) {
  const createTrip = useCreateTrip();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TripForm>({
    resolver: zodResolver(tripSchema),
    defaultValues: { visibility: 'PRIVATE' },
  });

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    document.body.classList.add('is-modal-open');
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.classList.remove('is-modal-open');
    };
  }, [onClose, open]);

  if (!open) return null;

  const submit = handleSubmit(async (values) => {
    const trip = await createTrip.mutateAsync(values);
    reset();
    onCreated(trip);
    onClose();
  });

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-trip-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modal-card__header">
          <div>
            <span className="eyebrow">NEW JOURNEY</span>
            <h2 id="create-trip-title">Where are you going next?</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close dialog">
            <X size={20} />
          </button>
        </header>

        <form className="trip-form" onSubmit={submit}>
          <label className="field field--full">
            <span>Trip title</span>
            <input placeholder="A Roman Holiday" autoFocus {...register('title')} />
            {errors.title && <small className="field__error">{errors.title.message}</small>}
          </label>

          <label className="field">
            <span>Country</span>
            <span className="input-with-icon">
              <MapPin size={17} />
              <input placeholder="Italy" {...register('country')} />
            </span>
            {errors.country && <small className="field__error">{errors.country.message}</small>}
          </label>

          <label className="field">
            <span>City</span>
            <input placeholder="Rome" {...register('city')} />
            {errors.city && <small className="field__error">{errors.city.message}</small>}
          </label>

          <label className="field">
            <span>Starts</span>
            <span className="input-with-icon">
              <CalendarDays size={17} />
              <input type="date" {...register('startDate')} />
            </span>
            {errors.startDate && <small className="field__error">{errors.startDate.message}</small>}
          </label>

          <label className="field">
            <span>Ends</span>
            <input type="date" {...register('endDate')} />
            {errors.endDate && <small className="field__error">{errors.endDate.message}</small>}
          </label>

          <fieldset className="visibility-field field--full">
            <legend>Who can see it?</legend>
            <label>
              <input type="radio" value="PRIVATE" {...register('visibility')} />
              <span>
                <LockKeyhole size={18} />
                <strong>Private</strong>
                <small>Only you and invited members</small>
              </span>
            </label>
            <label>
              <input type="radio" value="PUBLIC" {...register('visibility')} />
              <span>
                <span className="visibility-globe">◎</span>
                <strong>Public</strong>
                <small>Anyone with the public link</small>
              </span>
            </label>
          </fieldset>

          {createTrip.error && <p className="form-error">{createTrip.error.message}</p>}

          <footer className="modal-card__footer field--full">
            <button className="button button--ghost" type="button" onClick={onClose}>
              Cancel
            </button>
            <button className="button button--coral" type="submit" disabled={createTrip.isPending}>
              {createTrip.isPending ? 'Creating…' : 'Create trip'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}
