import { zodResolver } from '@hookform/resolvers/zod';
import { Clock3, MapPin, X } from 'lucide-react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useCreateStop, useUpdateStop } from '../hooks/useTrips';
import type { StopCategory, Trip, TripStop } from '../types';

const stopCategories: StopCategory[] = [
  'LANDMARK', 'RESTAURANT', 'HOTEL', 'AIRPORT', 'BEACH', 'MUSEUM',
  'BAR', 'SHOP', 'NATURE', 'TRANSPORT', 'OTHER',
];

const stopSchema = z.object({
  name: z.string().trim().min(2, 'Place name is required.').max(160),
  description: z.string().trim().max(2000),
  latitude: z.string().refine((value) => value !== '' && Number(value) >= -90 && Number(value) <= 90, 'Enter a latitude from -90 to 90.'),
  longitude: z.string().refine((value) => value !== '' && Number(value) >= -180 && Number(value) <= 180, 'Enter a longitude from -180 to 180.'),
  arrivalTime: z.string().min(1, 'Arrival time is required.'),
  departureTime: z.string(),
  category: z.enum(stopCategories),
  rating: z.string().refine((value) => value === '' || (Number(value) >= 1 && Number(value) <= 10), 'Rating must be from 1 to 10.'),
  position: z.string().refine((value) => Number.isInteger(Number(value)) && Number(value) >= 0, 'Position cannot be negative.'),
}).refine(({ arrivalTime, departureTime }) => !departureTime || departureTime >= arrivalTime, {
  path: ['departureTime'],
  message: 'Departure cannot be before arrival.',
});

type StopForm = z.infer<typeof stopSchema>;

interface TripStopDialogProps {
  open: boolean;
  trip: Trip;
  stop?: TripStop;
  onClose: () => void;
}

function dateTimeLocal(value: string) {
  const date = new Date(value);
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 16);
}

function offsetDateTime(value: string) {
  const date = new Date(value);
  const offset = -date.getTimezoneOffset();
  const sign = offset >= 0 ? '+' : '-';
  const hours = String(Math.floor(Math.abs(offset) / 60)).padStart(2, '0');
  const minutes = String(Math.abs(offset) % 60).padStart(2, '0');
  return `${value}:00${sign}${hours}:${minutes}`;
}

function inferredArrival(trip: Trip, stop: TripStop) {
  if (stop.arrivalAt) return dateTimeLocal(stop.arrivalAt);
  const date = new Date(`${trip.startDate}T00:00:00`);
  date.setDate(date.getDate() + stop.day - 1);
  return `${dateTimeLocal(date.toISOString()).slice(0, 10)}T${stop.arrivalTime}`;
}

export function TripStopDialog({ open, trip, stop, onClose }: TripStopDialogProps) {
  const createStop = useCreateStop(trip.id, trip.startDate);
  const updateStop = useUpdateStop(trip.id, trip.startDate);
  const mutation = stop ? updateStop : createStop;
  const { register, handleSubmit, reset, formState: { errors } } = useForm<StopForm>({
    resolver: zodResolver(stopSchema),
  });

  useEffect(() => {
    if (!open) return;
    reset(stop ? {
      name: stop.name,
      description: stop.description,
      latitude: String(stop.coordinates[1]),
      longitude: String(stop.coordinates[0]),
      arrivalTime: inferredArrival(trip, stop),
      departureTime: stop.departureAt ? dateTimeLocal(stop.departureAt) : '',
      category: stop.category,
      rating: stop.rating ? String(stop.rating) : '',
      position: String(stop.position ?? trip.stops.findIndex(({ id }) => id === stop.id)),
    } : {
      name: '',
      description: '',
      latitude: '',
      longitude: '',
      arrivalTime: `${trip.startDate}T09:00`,
      departureTime: '',
      category: 'LANDMARK',
      rating: '',
      position: String(trip.stops.length),
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
  }, [onClose, open, reset, stop, trip]);

  if (!open) return null;

  const submit = handleSubmit(async (values) => {
    const input = {
      name: values.name,
      description: values.description,
      latitude: Number(values.latitude),
      longitude: Number(values.longitude),
      arrivalTime: offsetDateTime(values.arrivalTime),
      departureTime: values.departureTime ? offsetDateTime(values.departureTime) : undefined,
      category: values.category,
      rating: values.rating ? Number(values.rating) : undefined,
      position: Number(values.position),
    };
    try {
      if (stop) await updateStop.mutateAsync({ stopId: stop.id, input });
      else await createStop.mutateAsync(input);
      onClose();
    } catch {
      return;
    }
  });

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="stop-dialog-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-card__header">
          <div><span className="eyebrow">ITINERARY</span><h2 id="stop-dialog-title">{stop ? 'Edit this place' : 'Add a place'}</h2></div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close dialog"><X size={20} /></button>
        </header>
        <form className="trip-form" onSubmit={submit}>
          <label className="field field--full"><span>Place name</span><input autoFocus placeholder="Colosseum" {...register('name')} />{errors.name && <small className="field__error">{errors.name.message}</small>}</label>
          <label className="field field--full"><span>Description</span><textarea rows={3} placeholder="What made this stop memorable?" {...register('description')} />{errors.description && <small className="field__error">{errors.description.message}</small>}</label>
          <label className="field"><span>Latitude</span><span className="input-with-icon"><MapPin size={17} /><input inputMode="decimal" placeholder="41.8902" {...register('latitude')} /></span>{errors.latitude && <small className="field__error">{errors.latitude.message}</small>}</label>
          <label className="field"><span>Longitude</span><input inputMode="decimal" placeholder="12.4922" {...register('longitude')} />{errors.longitude && <small className="field__error">{errors.longitude.message}</small>}</label>
          <label className="field"><span>Arrival</span><span className="input-with-icon"><Clock3 size={17} /><input type="datetime-local" min={`${trip.startDate}T00:00`} max={`${trip.endDate}T23:59`} {...register('arrivalTime')} /></span>{errors.arrivalTime && <small className="field__error">{errors.arrivalTime.message}</small>}</label>
          <label className="field"><span>Departure (optional)</span><input type="datetime-local" min={`${trip.startDate}T00:00`} max={`${trip.endDate}T23:59`} {...register('departureTime')} />{errors.departureTime && <small className="field__error">{errors.departureTime.message}</small>}</label>
          <label className="field"><span>Category</span><select {...register('category')}>{stopCategories.map((category) => <option key={category} value={category}>{category.toLowerCase()}</option>)}</select></label>
          <label className="field"><span>Rating (1–10)</span><input type="number" min="1" max="10" {...register('rating')} />{errors.rating && <small className="field__error">{errors.rating.message}</small>}</label>
          <label className="field"><span>Timeline position</span><input type="number" min="0" {...register('position')} />{errors.position && <small className="field__error">{errors.position.message}</small>}</label>
          {mutation.error && <p className="form-error">{mutation.error.message}</p>}
          <footer className="modal-card__footer field--full">
            <button className="button button--ghost" type="button" onClick={onClose}>Cancel</button>
            <button className="button button--coral" type="submit" disabled={mutation.isPending}>{mutation.isPending ? 'Saving…' : stop ? 'Save place' : 'Add place'}</button>
          </footer>
        </form>
      </section>
    </div>
  );
}
