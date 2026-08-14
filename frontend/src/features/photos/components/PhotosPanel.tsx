import { Camera, Eye, EyeOff, ImagePlus, MapPin, Sparkles, Trash2, Upload, X } from 'lucide-react';
import { useRef, useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/context/AuthContext';
import type { Trip, TripStop } from '../../trips/types';
import { useDeletePhoto, useSetPhotoPublicVisibility, useTripPhotos, useUploadPhoto } from '../hooks/usePhotos';
import type { TripPhoto } from '../types';

interface PhotosPanelProps {
  trip: Trip;
  stops: TripStop[];
}

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ACCEPTED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

function formatPhotoDate(value: string | null, fallback: string) {
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: value ? '2-digit' : undefined,
    minute: value ? '2-digit' : undefined,
  }).format(new Date(value ?? fallback));
}

function formatFileSize(bytes: number) {
  return bytes >= 1024 * 1024
    ? `${(bytes / 1024 / 1024).toFixed(1)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

export function PhotosPanel({ trip, stops }: PhotosPanelProps) {
  const photos = useTripPhotos(trip.id);
  const uploadPhoto = useUploadPhoto(trip.id);
  const deletePhoto = useDeletePhoto(trip.id);
  const setPublicVisibility = useSetPhotoPublicVisibility(trip.id);
  const { session, demoMode } = useAuth();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [caption, setCaption] = useState('');
  const [tripStopId, setTripStopId] = useState('');
  const [validationError, setValidationError] = useState('');
  const [detectedPhoto, setDetectedPhoto] = useState<TripPhoto | null>(null);
  const canUpload = trip.currentUserRole === 'OWNER' || trip.currentUserRole === 'EDITOR';

  function selectFile(selected: File | undefined) {
    setValidationError('');
    setDetectedPhoto(null);
    if (!selected) return;
    if (!ACCEPTED_TYPES.has(selected.type)) {
      setValidationError('Choose a JPEG, PNG or WebP image.');
      return;
    }
    if (selected.size > MAX_FILE_SIZE) {
      setValidationError('Photos cannot be larger than 10 MB.');
      return;
    }
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setFile(selected);
    setPreviewUrl(URL.createObjectURL(selected));
  }

  function clearSelection() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setFile(null);
    setPreviewUrl(null);
    setCaption('');
    setTripStopId('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) {
      setValidationError('Choose a photo first.');
      return;
    }
    try {
      const uploaded = await uploadPhoto.mutateAsync({
        file,
        caption,
        tripStopId: tripStopId || undefined,
      });
      setDetectedPhoto(uploaded);
      clearSelection();
    } catch {
      return;
    }
  }

  async function confirmDelete(photo: TripPhoto) {
    if (window.confirm(`Delete ${photo.originalFileName}? This cannot be undone.`)) {
      await deletePhoto.mutateAsync(photo.id);
    }
  }

  function canDelete(photo: TripPhoto) {
    return trip.currentUserRole === 'OWNER'
      || (trip.currentUserRole === 'EDITOR'
        && (demoMode || photo.uploadedByUserId === session?.user.id));
  }

  return (
    <section className="photos-layout">
      <header className="photos-heading">
        <div>
          <span className="eyebrow">TRIP MEMORIES</span>
          <h2>Photos from the journey</h2>
          <p>{trip.visibility === 'PUBLIC'
            ? 'Choose up to 12 photos to feature on the anonymous public page.'
            : 'Private images use short-lived access links and follow the same trip roles.'}</p>
        </div>
        <span className="photo-count"><Camera size={16} /> {photos.data?.length ?? 0} photos</span>
      </header>

      {canUpload && (
        <form className="photo-upload" onSubmit={(event) => void submit(event)}>
          <input
            ref={fileInputRef}
            className="photo-upload__input"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => selectFile(event.target.files?.[0])}
          />
          <button
            className={`photo-dropzone${previewUrl ? ' photo-dropzone--selected' : ''}`}
            type="button"
            onClick={() => fileInputRef.current?.click()}
          >
            {previewUrl ? (
              <img src={previewUrl} alt="Selected upload preview" />
            ) : (
              <><span><ImagePlus size={25} /></span><strong>Add a memory</strong><small>JPEG, PNG or WebP · up to 10 MB</small></>
            )}
          </button>
          <div className="photo-upload__fields">
            <label>
              <span>Place (optional)</span>
              <select value={tripStopId} onChange={(event) => setTripStopId(event.target.value)}>
                <option value="">Whole trip</option>
                {stops.map((stop) => <option key={stop.id} value={stop.id}>{stop.name}</option>)}
              </select>
            </label>
            <label>
              <span>Caption (optional)</span>
              <textarea
                value={caption}
                maxLength={1000}
                placeholder="What made this moment special?"
                onChange={(event) => setCaption(event.target.value)}
              />
            </label>
            {(validationError || uploadPhoto.error) && (
              <p className="photo-error" role="alert">{validationError || uploadPhoto.error?.message}</p>
            )}
            <div className="photo-upload__actions">
              {file && <button className="button button--ghost" type="button" onClick={clearSelection}><X size={15} /> Clear</button>}
              <button className="button button--coral" type="submit" disabled={!file || uploadPhoto.isPending}>
                <Upload size={15} /> {uploadPhoto.isPending ? 'Uploading…' : 'Upload photo'}
              </button>
            </div>
          </div>
        </form>
      )}

      {detectedPhoto && (detectedPhoto.takenAt || detectedPhoto.latitude !== null) && (
        <div className="exif-result" role="status">
          <span><Sparkles size={19} /></span>
          <div>
            <strong>Photo details detected</strong>
            {detectedPhoto.latitude !== null && detectedPhoto.longitude !== null && (
              <small><MapPin size={13} /> {detectedPhoto.latitude.toFixed(4)}, {detectedPhoto.longitude.toFixed(4)}</small>
            )}
            {detectedPhoto.takenAt && <small>{formatPhotoDate(detectedPhoto.takenAt, detectedPhoto.createdAt)}</small>}
          </div>
        </div>
      )}

      {setPublicVisibility.error && <p className="photo-error" role="alert">{setPublicVisibility.error.message}</p>}

      {photos.isLoading ? (
        <div className="photos-empty"><span className="detail-skeleton" /></div>
      ) : photos.error ? (
        <p className="photo-error" role="alert">{photos.error.message}</p>
      ) : photos.data?.length ? (
        <div className="photo-grid">
          {photos.data.map((photo) => (
            <article className={`photo-card${photo.publicVisible ? ' photo-card--public' : ''}`} key={photo.id}>
              <img src={photo.signedUrl} alt={photo.caption || photo.originalFileName} loading="lazy" />
              {photo.publicVisible && <span className="photo-card__public-badge"><Eye size={12} /> Public page</span>}
              <div className="photo-card__overlay">
                <div>
                  <strong>{photo.caption || photo.originalFileName}</strong>
                  <small>{formatPhotoDate(photo.takenAt, photo.createdAt)} · {formatFileSize(photo.fileSize)}</small>
                  <small>By {photo.uploadedByDisplayName}</small>
                </div>
                <div className="photo-card__actions">
                  {canUpload && trip.visibility === 'PUBLIC' && (
                    <button
                      className="photo-card__visibility"
                      type="button"
                      title={photo.publicVisible ? 'Remove from public page' : 'Feature on public page'}
                      aria-label={photo.publicVisible ? `Remove ${photo.originalFileName} from public page` : `Feature ${photo.originalFileName} on public page`}
                      disabled={setPublicVisibility.isPending}
                      onClick={() => setPublicVisibility.mutate({ photoId: photo.id, publicVisible: !photo.publicVisible })}
                    >
                      {photo.publicVisible ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  )}
                  {canDelete(photo) && (
                    <button type="button" aria-label={`Delete ${photo.originalFileName}`} disabled={deletePhoto.isPending} onClick={() => void confirmDelete(photo)}>
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className="photos-empty">
          <span><Camera size={27} /></span>
          <h3>No photos yet</h3>
          <p>{canUpload ? 'Add the first memory from this trip.' : 'The travellers have not added photos yet.'}</p>
        </div>
      )}
    </section>
  );
}
