import { useQuery } from '@tanstack/react-query';
import { comparisonApi } from '../api/comparisonApi';

export const comparisonKeys = {
  detail: (leftTripId: string, rightTripId: string) => ['trip-comparison', leftTripId, rightTripId] as const,
};

export function useTripComparison(leftTripId: string, rightTripId: string) {
  return useQuery({
    queryKey: comparisonKeys.detail(leftTripId, rightTripId),
    queryFn: () => comparisonApi.compare(leftTripId, rightTripId),
    enabled: Boolean(leftTripId && rightTripId && leftTripId !== rightTripId),
  });
}
