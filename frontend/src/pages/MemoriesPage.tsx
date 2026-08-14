import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  CalendarDays,
  Camera,
  Compass,
  GalleryHorizontalEnd,
  Images,
  MapPin,
  Search,
  Sparkles,
  X,
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMemoryGallery } from '../features/memories/hooks/useMemoryGallery';
import { filterMemories, groupMemoriesByYear, memoryDate } from '../features/memories/memoryGalleryModel';
import type { MemoryGalleryFilters, MemoryPhoto } from '../features/memories/types';

function formatMemoryDate(value: string) {
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function formatFileSize(bytes: number) {
  return bytes >= 1024 * 1024
    ? `${(bytes / 1024 / 1024).toFixed(1)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

function MemoryLightbox({
  memories,
  activeIndex,
  onChange,
  onClose,
}: {
  memories: MemoryPhoto[];
  activeIndex: number;
  onChange: (index: number) => void;
  onClose: () => void;
}) {
  const memory = memories[activeIndex];

  useEffect(() => {
    document.body.classList.add('is-modal-open');
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
      if (event.key === 'ArrowLeft') onChange((activeIndex - 1 + memories.length) % memories.length);
      if (event.key === 'ArrowRight') onChange((activeIndex + 1) % memories.length);
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.classList.remove('is-modal-open');
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [activeIndex, memories.length, onChange, onClose]);

  return (
    <div className="memory-lightbox" role="presentation" onMouseDown={onClose}>
      <section role="dialog" aria-modal="true" aria-label={memory.caption || memory.originalFileName} onMouseDown={(event) => event.stopPropagation()}>
        <div className="memory-lightbox__image">
          <img src={memory.signedUrl} alt={memory.caption || memory.originalFileName} />
          <button className="memory-lightbox__close" type="button" onClick={onClose} aria-label="Close memory"><X size={21} /></button>
          {memories.length > 1 && (
            <>
              <button className="memory-lightbox__previous" type="button" onClick={() => onChange((activeIndex - 1 + memories.length) % memories.length)} aria-label="Previous memory"><ArrowLeft size={21} /></button>
              <button className="memory-lightbox__next" type="button" onClick={() => onChange((activeIndex + 1) % memories.length)} aria-label="Next memory"><ArrowRight size={21} /></button>
            </>
          )}
          <span className="memory-lightbox__counter">{activeIndex + 1} / {memories.length}</span>
        </div>
        <aside className="memory-lightbox__details">
          <span className="eyebrow">TRAVEL MEMORY</span>
          <h2>{memory.caption || memory.tripTitle}</h2>
          <p className="memory-lightbox__location"><MapPin size={15} /> {memory.tripStopName ? `${memory.tripStopName}, ` : ''}{memory.city}, {memory.country}</p>
          <dl>
            <div><dt>Captured</dt><dd>{formatMemoryDate(memoryDate(memory))}</dd></div>
            <div><dt>Journey</dt><dd>{memory.tripTitle}</dd></div>
            <div><dt>Added by</dt><dd>{memory.uploadedByDisplayName}</dd></div>
            <div><dt>File</dt><dd>{memory.originalFileName} · {formatFileSize(memory.fileSize)}</dd></div>
            {memory.latitude !== null && memory.longitude !== null && <div><dt>Coordinates</dt><dd>{memory.latitude.toFixed(4)}, {memory.longitude.toFixed(4)}</dd></div>}
          </dl>
          {memory.publicVisible && <span className="memory-lightbox__public"><Sparkles size={13} /> Featured on the public trip page</span>}
          <Link className="button button--dark" to={`/trips/${memory.tripId}`} onClick={onClose}>Open journey <ArrowUpRight size={16} /></Link>
        </aside>
      </section>
    </div>
  );
}

export function MemoriesPage() {
  const gallery = useMemoryGallery();
  const [query, setQuery] = useState('');
  const [year, setYear] = useState('ALL');
  const [tripId, setTripId] = useState('ALL');
  const [locationOnly, setLocationOnly] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const memories = useMemo(() => gallery.data ?? [], [gallery.data]);
  const filters: MemoryGalleryFilters = { query, year, tripId, locationOnly };
  const visibleMemories = filterMemories(memories, filters);
  const groups = groupMemoriesByYear(visibleMemories);
  const years = [...new Set(memories.map((memory) => memoryDate(memory).slice(0, 4)))].sort().reverse();
  const trips = [...new Map(memories.map((memory) => [memory.tripId, memory.tripTitle])).entries()]
    .sort((left, right) => left[1].localeCompare(right[1]));
  const selectedIndex = selectedId ? visibleMemories.findIndex(({ id }) => id === selectedId) : -1;
  const countryCount = new Set(memories.map(({ countryCode }) => countryCode)).size;
  const tripCount = new Set(memories.map(({ tripId: memoryTripId }) => memoryTripId)).size;
  const locatedCount = memories.filter((memory) => memory.latitude !== null || memory.tripStopId !== null).length;
  const filtersActive = query || year !== 'ALL' || tripId !== 'ALL' || locationOnly;

  function clearFilters() {
    setQuery('');
    setYear('ALL');
    setTripId('ALL');
    setLocationOnly(false);
  }

  return (
    <div className="page page--memories">
      <header className="page-header memories-header">
        <div><span className="eyebrow">YOUR VISUAL JOURNAL</span><h1>Memories, collected.</h1><p>Every frame stays connected to the journey, place and people behind it.</p></div>
        <Link className="button button--coral" to="/trips"><Camera size={17} /> Add through a trip</Link>
      </header>

      <section className="memories-overview" aria-label="Memory statistics">
        <article><span><Images size={18} /></span><div><strong>{memories.length}</strong><small>photos</small></div></article>
        <article><span><Compass size={18} /></span><div><strong>{tripCount}</strong><small>journeys</small></div></article>
        <article><span><GalleryHorizontalEnd size={18} /></span><div><strong>{countryCount}</strong><small>countries</small></div></article>
        <article><span><MapPin size={18} /></span><div><strong>{locatedCount}</strong><small>placed memories</small></div></article>
      </section>

      <div className="memories-toolbar">
        <label className="search-box memories-search"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search captions, trips, cities or places" />{query && <button type="button" onClick={() => setQuery('')} aria-label="Clear memory search"><X size={14} /></button>}</label>
        <label><CalendarDays size={16} /><select value={year} onChange={(event) => setYear(event.target.value)}><option value="ALL">All years</option>{years.map((value) => <option key={value}>{value}</option>)}</select></label>
        <label><Compass size={16} /><select value={tripId} onChange={(event) => setTripId(event.target.value)}><option value="ALL">All journeys</option>{trips.map(([id, title]) => <option key={id} value={id}>{title}</option>)}</select></label>
        <button className={locationOnly ? 'is-active' : ''} type="button" onClick={() => setLocationOnly((current) => !current)}><MapPin size={16} /> Has location</button>
      </div>

      {gallery.isLoading ? (
        <div className="memory-gallery-loading"><span /><span /><span /><span /><span /><span /></div>
      ) : gallery.isError ? (
        <div className="empty-state"><span><Camera size={28} /></span><h2>We could not develop your memories</h2><p>{gallery.error.message}</p><button className="button button--dark" type="button" onClick={() => void gallery.refetch()}>Try again</button></div>
      ) : groups.length ? (
        <div className="memory-year-groups">
          {groups.map(([groupYear, yearMemories]) => (
            <section className="memory-year-section" key={groupYear}>
              <header><div><span>{groupYear}</span><h2>{groupYear === new Date().getFullYear().toString() ? 'This year in frames' : 'Stories from the road'}</h2></div><small>{yearMemories.length} {yearMemories.length === 1 ? 'memory' : 'memories'}</small></header>
              <div className="memory-gallery-grid">
                {yearMemories.map((memory) => (
                  <button className="memory-gallery-card" type="button" key={memory.id} onClick={() => setSelectedId(memory.id)}>
                    <img src={memory.signedUrl} alt={memory.caption || memory.originalFileName} loading="lazy" />
                    {memory.tripStopName && <span className="memory-gallery-card__place"><MapPin size={11} /> {memory.tripStopName}</span>}
                    <span className="memory-gallery-card__overlay"><strong>{memory.caption || memory.tripTitle}</strong><small>{memory.city} · {formatMemoryDate(memoryDate(memory))}</small></span>
                  </button>
                ))}
              </div>
            </section>
          ))}
        </div>
      ) : (
        <div className="memories-empty">
          <span><Camera size={31} /></span>
          <h2>{filtersActive ? 'No memories match these filters' : 'Your gallery is ready for its first frame'}</h2>
          <p>{filtersActive ? 'Try another year, journey or search phrase.' : 'Open a journey and upload a photo. Its date, place and story will appear here automatically.'}</p>
          {filtersActive ? <button className="button button--ghost" type="button" onClick={clearFilters}>Clear filters</button> : <Link className="button button--coral" to="/trips">Choose a journey</Link>}
        </div>
      )}

      {selectedIndex >= 0 && (
        <MemoryLightbox memories={visibleMemories} activeIndex={selectedIndex} onChange={(index) => setSelectedId(visibleMemories[index].id)} onClose={() => setSelectedId(null)} />
      )}
    </div>
  );
}
