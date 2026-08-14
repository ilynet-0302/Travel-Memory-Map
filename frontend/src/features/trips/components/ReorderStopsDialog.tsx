import { ArrowDown, ArrowUp, GripVertical, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useReorderStops } from '../hooks/useTrips';
import type { Trip, TripStop } from '../types';

interface ReorderStopsDialogProps {
  open: boolean;
  trip: Trip;
  onClose: () => void;
}

export function ReorderStopsDialog({ open, trip, onClose }: ReorderStopsDialogProps) {
  const [orderedStops, setOrderedStops] = useState<TripStop[]>(trip.stops);
  const [draggedId, setDraggedId] = useState<string | null>(null);
  const reorderStops = useReorderStops(trip.id, trip.startDate);

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

  const moveBy = (stopId: string, offset: number) => {
    setOrderedStops((current) => {
      const from = current.findIndex(({ id }) => id === stopId);
      const to = from + offset;
      if (from < 0 || to < 0 || to >= current.length) return current;
      const next = [...current];
      const [moved] = next.splice(from, 1);
      next.splice(to, 0, moved);
      return next;
    });
  };

  const moveDraggedTo = (targetId: string) => {
    if (!draggedId || draggedId === targetId) return;
    setOrderedStops((current) => {
      const from = current.findIndex(({ id }) => id === draggedId);
      const to = current.findIndex(({ id }) => id === targetId);
      if (from < 0 || to < 0) return current;
      const next = [...current];
      const [moved] = next.splice(from, 1);
      next.splice(to, 0, moved);
      return next;
    });
  };

  const save = async () => {
    try {
      await reorderStops.mutateAsync(orderedStops.map(({ id }) => id));
      onClose();
    } catch {
      return;
    }
  };

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card reorder-dialog" role="dialog" aria-modal="true" aria-labelledby="reorder-dialog-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-card__header">
          <div><span className="eyebrow">ITINERARY</span><h2 id="reorder-dialog-title">Reorder places</h2></div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close dialog"><X size={20} /></button>
        </header>
        <p className="reorder-dialog__hint">Drag places into the route order you want. Their dates and times will stay unchanged.</p>
        <ol className="reorder-list">
          {orderedStops.map((stop, index) => (
            <li
              className={`reorder-item${draggedId === stop.id ? ' reorder-item--dragging' : ''}`}
              key={stop.id}
              onDragOver={(event) => event.preventDefault()}
              onDragEnter={() => moveDraggedTo(stop.id)}
              onDrop={() => setDraggedId(null)}
            >
              <span
                className="reorder-item__handle"
                draggable
                role="button"
                tabIndex={0}
                aria-label={`Drag ${stop.name}`}
                onDragStart={(event) => {
                  setDraggedId(stop.id);
                  event.dataTransfer.effectAllowed = 'move';
                  event.dataTransfer.setData('text/plain', stop.id);
                }}
                onDragEnd={() => setDraggedId(null)}
              ><GripVertical size={19} /></span>
              <span className="reorder-item__number">{index + 1}</span>
              <span className="reorder-item__copy"><strong>{stop.name}</strong><small>Day {stop.day} · {stop.arrivalTime} · {stop.category.toLowerCase()}</small></span>
              <span className="reorder-item__buttons">
                <button type="button" disabled={index === 0} onClick={() => moveBy(stop.id, -1)} aria-label={`Move ${stop.name} up`}><ArrowUp size={15} /></button>
                <button type="button" disabled={index === orderedStops.length - 1} onClick={() => moveBy(stop.id, 1)} aria-label={`Move ${stop.name} down`}><ArrowDown size={15} /></button>
              </span>
            </li>
          ))}
        </ol>
        {reorderStops.error && <p className="form-error reorder-dialog__error">{reorderStops.error.message}</p>}
        <footer className="modal-card__footer reorder-dialog__footer">
          <button className="button button--ghost" type="button" onClick={onClose}>Cancel</button>
          <button className="button button--coral" type="button" disabled={reorderStops.isPending} onClick={() => void save()}>{reorderStops.isPending ? 'Saving…' : 'Save order'}</button>
        </footer>
      </section>
    </div>
  );
}
