import { zodResolver } from '@hookform/resolvers/zod';
import { CalendarDays, LockKeyhole, MapPin, X } from 'lucide-react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useUpdateTrip } from '../hooks/useTrips';
import type { Trip } from '../types';

const editTripSchema = z
  .object({
    title: z.string().trim().min(3, 'Give your trip a memorable title.').max(120),
    description: z.string().trim().max(2000),
    country: z.string().trim().min(2, 'Country is required.').max(100),
    city: z.string().trim().min(2, 'City is required.').max(100),
    startDate: z.string().min(1, 'Start date is required.'),
    endDate: z.string().min(1, 'End date is required.'),
    visibility: z.enum(['PRIVATE', 'PUBLIC']),
  })
  .refine(({ startDate, endDate }) => endDate >= startDate, {
    path: ['endDate'],
    message: 'End date must be on or after the start date.',
  });

type EditTripForm = z.infer<typeof editTripSchema>;

interface EditTripDialogProps {
  open: boolean;
  trip: Trip;
  onClose: () => void;
}

export function EditTripDialog({ open, trip, onClose }: EditTripDialogProps) {
  const updateTrip = useUpdateTrip(trip.id);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditTripForm>({ resolver: zodResolver(editTripSchema) });

  useEffect(() => {
    if (!open) return;
    reset({
      title: trip.title,
      description: trip.description,
      country: trip.country,
      city: trip.city,
      startDate: trip.startDate,
      endDate: trip.endDate,
      visibility: trip.visibility,
    });
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    document.body.classList.add('is-modal-open');
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.classList.remove('is-modal-open');
    };
  }, [onClose, open, reset, trip]);

  if (!open) return null;

  const submit = handleSubmit(async (values) => {
    try {
      await updateTrip.mutateAsync(values);
      onClose();
    } catch {
      return;
    }
  });

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="edit-trip-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modal-card__header">
          <div>
            <span className="eyebrow">TRIP SETTINGS</span>
            <h2 id="edit-trip-title">Edit your journey</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close dialog"><X size={20} /></button>
        </header>

        <form className="trip-form" onSubmit={submit}>
          <label className="field field--full">
            <span>Trip title</span>
            <input autoFocus {...register('title')} />
            {errors.title && <small className="field__error">{errors.title.message}</small>}
          </label>
          <label className="field field--full">
            <span>Description</span>
            <textarea rows={3} {...register('description')} />
            {errors.description && <small className="field__error">{errors.description.message}</small>}
          </label>
          <label className="field">
            <span>Country</span>
            <span className="input-with-icon"><MapPin size={17} /><input {...register('country')} /></span>
            {errors.country && <small className="field__error">{errors.country.message}</small>}
          </label>
          <label className="field"><span>City</span><input {...register('city')} />{errors.city && <small className="field__error">{errors.city.message}</small>}</label>
          <label className="field">
            <span>Starts</span>
            <span className="input-with-icon"><CalendarDays size={17} /><input type="date" {...register('startDate')} /></span>
            {errors.startDate && <small className="field__error">{errors.startDate.message}</small>}
          </label>
          <label className="field"><span>Ends</span><input type="date" {...register('endDate')} />{errors.endDate && <small className="field__error">{errors.endDate.message}</small>}</label>
          <fieldset className="visibility-field field--full">
            <legend>Who can see it?</legend>
            <label><input type="radio" value="PRIVATE" {...register('visibility')} /><span><LockKeyhole size={18} /><strong>Private</strong><small>Only you and invited members</small></span></label>
            <label><input type="radio" value="PUBLIC" {...register('visibility')} /><span><span className="visibility-globe">◎</span><strong>Public</strong><small>Anyone with the public link</small></span></label>
          </fieldset>
          {updateTrip.error && <p className="form-error">{updateTrip.error.message}</p>}
          <footer className="modal-card__footer field--full">
            <button className="button button--ghost" type="button" onClick={onClose}>Cancel</button>
            <button className="button button--coral" type="submit" disabled={updateTrip.isPending}>{updateTrip.isPending ? 'Saving…' : 'Save changes'}</button>
          </footer>
        </form>
      </section>
    </div>
  );
}
