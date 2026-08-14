import { apiClient } from '../../../services/apiClient';
import type { TripPhoto, UploadPhotoInput } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';
const demoPhotos = new Map<string, TripPhoto[]>();

const wait = (milliseconds: number) =>
  new Promise<void>((resolve) => globalThis.setTimeout(resolve, milliseconds));

export const photosApi = {
  async list(tripId: string): Promise<TripPhoto[]> {
    if (!demoMode) return apiClient<TripPhoto[]>(`/trips/${tripId}/photos`);
    await wait(100);
    return [...(demoPhotos.get(tripId) ?? [])];
  },

  async upload(tripId: string, input: UploadPhotoInput): Promise<TripPhoto> {
    if (!demoMode) {
      const form = new FormData();
      form.append('file', input.file);
      if (input.caption?.trim()) form.append('caption', input.caption.trim());
      if (input.tripStopId) form.append('tripStopId', input.tripStopId);
      return apiClient<TripPhoto>(`/trips/${tripId}/photos`, { method: 'POST', body: form });
    }

    await wait(260);
    const photo: TripPhoto = {
      id: crypto.randomUUID(),
      tripId,
      tripStopId: input.tripStopId ?? null,
      uploadedByUserId: 'demo-user',
      uploadedByDisplayName: 'Demo Traveller',
      storagePath: `demo/${input.file.name}`,
      signedUrl: URL.createObjectURL(input.file),
      originalFileName: input.file.name,
      contentType: input.file.type,
      fileSize: input.file.size,
      takenAt: null,
      latitude: null,
      longitude: null,
      caption: input.caption?.trim() || null,
      publicVisible: false,
      createdAt: new Date().toISOString(),
    };
    demoPhotos.set(tripId, [photo, ...(demoPhotos.get(tripId) ?? [])]);
    return photo;
  },

  async delete(tripId: string, photoId: string): Promise<void> {
    if (!demoMode) {
      await apiClient<void>(`/trips/${tripId}/photos/${photoId}`, { method: 'DELETE' });
      return;
    }
    await wait(160);
    demoPhotos.set(tripId, (demoPhotos.get(tripId) ?? []).filter(({ id }) => id !== photoId));
  },

  async setPublicVisibility(tripId: string, photoId: string, publicVisible: boolean): Promise<TripPhoto> {
    if (!demoMode) {
      return apiClient<TripPhoto>(`/trips/${tripId}/photos/${photoId}/public-visibility`, {
        method: 'PATCH',
        body: JSON.stringify({ publicVisible }),
      });
    }
    await wait(120);
    const photos = demoPhotos.get(tripId) ?? [];
    const photo = photos.find(({ id }) => id === photoId);
    if (!photo) throw new Error('We could not find that photo.');
    const updated = { ...photo, publicVisible };
    demoPhotos.set(tripId, photos.map((current) => current.id === photoId ? updated : current));
    return updated;
  },
};
