import { useQuery } from '@tanstack/react-query';
import { publicTripApi } from '../api/publicTripApi';

export function usePublicTrip(publicSlug: string) {
  return useQuery({
    queryKey: ['public-trip', publicSlug],
    queryFn: () => publicTripApi.get(publicSlug),
    enabled: Boolean(publicSlug),
  });
}
