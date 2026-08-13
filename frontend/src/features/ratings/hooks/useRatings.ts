import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ratingsApi } from '../api/ratingsApi';

export const ratingKeys = {
  summary: (tripId: string) => ['trip-rating', tripId] as const,
};

export function useTripRating(tripId: string) {
  return useQuery({
    queryKey: ratingKeys.summary(tripId),
    queryFn: () => ratingsApi.summary(tripId),
  });
}

export function useRateTrip(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (score: number) => ratingsApi.rate(tripId, score),
    onSuccess: async (summary) => {
      queryClient.setQueryData(ratingKeys.summary(tripId), summary);
      await queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
  });
}

export function useRemoveTripRating(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => ratingsApi.remove(tripId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ratingKeys.summary(tripId) }),
        queryClient.invalidateQueries({ queryKey: ['profile'] }),
      ]);
    },
  });
}
