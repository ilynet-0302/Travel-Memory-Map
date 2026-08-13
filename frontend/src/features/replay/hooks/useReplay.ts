import { useQuery } from '@tanstack/react-query';
import { replayApi } from '../api/replayApi';

export const replayKeys = {
  trip: (tripId: string) => ['trip-replay', tripId] as const,
};

export function useTripReplay(tripId: string, enabled = true) {
  return useQuery({
    queryKey: replayKeys.trip(tripId),
    queryFn: () => replayApi.get(tripId),
    enabled: enabled && Boolean(tripId),
  });
}

