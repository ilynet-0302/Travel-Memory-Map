import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { tripsApi } from '../api/tripsApi';
import type { TripStopInput, UpdateTripInput } from '../types';

export const tripKeys = {
  all: ['trips'] as const,
  detail: (tripId: string) => ['trips', tripId] as const,
};

export function useTrips() {
  return useQuery({ queryKey: tripKeys.all, queryFn: tripsApi.list });
}

export function useTrip(tripId: string) {
  return useQuery({
    queryKey: tripKeys.detail(tripId),
    queryFn: () => tripsApi.getById(tripId),
  });
}

export function useCreateTrip() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: tripsApi.create,
    onSuccess: async (trip) => {
      queryClient.setQueryData(tripKeys.detail(trip.id), trip);
      await queryClient.invalidateQueries({ queryKey: tripKeys.all });
    },
  });
}

export function useUpdateTrip(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: UpdateTripInput) => tripsApi.update(tripId, input),
    onSuccess: async (trip) => {
      queryClient.setQueryData(tripKeys.detail(tripId), trip);
      await queryClient.invalidateQueries({ queryKey: tripKeys.all });
    },
  });
}

function useRemoveTrip(tripId: string, action: 'archive' | 'delete') {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => tripsApi[action](tripId),
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: tripKeys.detail(tripId) });
      await queryClient.invalidateQueries({ queryKey: tripKeys.all });
    },
  });
}

export function useArchiveTrip(tripId: string) {
  return useRemoveTrip(tripId, 'archive');
}

export function useDeleteTrip(tripId: string) {
  return useRemoveTrip(tripId, 'delete');
}

export function useCreateStop(tripId: string, tripStartDate: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: TripStopInput) => tripsApi.createStop(tripId, tripStartDate, input),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) }),
        queryClient.invalidateQueries({ queryKey: tripKeys.all }),
        queryClient.invalidateQueries({ queryKey: ['trip-replay', tripId] }),
      ]);
    },
  });
}

export function useUpdateStop(tripId: string, tripStartDate: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ stopId, input }: { stopId: string; input: TripStopInput }) =>
      tripsApi.updateStop(tripId, stopId, tripStartDate, input),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) }),
        queryClient.invalidateQueries({ queryKey: tripKeys.all }),
        queryClient.invalidateQueries({ queryKey: ['trip-replay', tripId] }),
      ]);
    },
  });
}

export function useDeleteStop(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (stopId: string) => tripsApi.deleteStop(tripId, stopId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) }),
        queryClient.invalidateQueries({ queryKey: tripKeys.all }),
        queryClient.invalidateQueries({ queryKey: ['trip-replay', tripId] }),
      ]);
    },
  });
}
