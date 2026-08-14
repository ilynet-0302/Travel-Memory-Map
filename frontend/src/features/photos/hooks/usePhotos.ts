import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { tripKeys } from '../../trips/hooks/useTrips';
import { photosApi } from '../api/photosApi';
import type { UploadPhotoInput } from '../types';

export const photoKeys = {
  trip: (tripId: string) => ['trip-photos', tripId] as const,
};

export function useTripPhotos(tripId: string) {
  return useQuery({ queryKey: photoKeys.trip(tripId), queryFn: () => photosApi.list(tripId) });
}

export function useUploadPhoto(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: UploadPhotoInput) => photosApi.upload(tripId, input),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: photoKeys.trip(tripId) }),
        queryClient.invalidateQueries({ queryKey: ['trip-replay', tripId] }),
        queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) }),
        queryClient.invalidateQueries({ queryKey: tripKeys.all }),
        queryClient.invalidateQueries({ queryKey: ['profile'] }),
      ]);
    },
  });
}

export function useDeletePhoto(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (photoId: string) => photosApi.delete(tripId, photoId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: photoKeys.trip(tripId) }),
        queryClient.invalidateQueries({ queryKey: ['trip-replay', tripId] }),
        queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) }),
        queryClient.invalidateQueries({ queryKey: ['profile'] }),
      ]);
    },
  });
}

export function useSetPhotoPublicVisibility(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ photoId, publicVisible }: { photoId: string; publicVisible: boolean }) =>
      photosApi.setPublicVisibility(tripId, photoId, publicVisible),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: photoKeys.trip(tripId) }),
        queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) }),
        queryClient.invalidateQueries({ queryKey: ['profile', 'memories'] }),
      ]);
    },
  });
}
