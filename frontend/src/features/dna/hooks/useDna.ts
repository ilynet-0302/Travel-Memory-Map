import { useQuery } from '@tanstack/react-query';
import { dnaApi } from '../api/dnaApi';

export const dnaKeys = {
  trip: (tripId: string) => ['trip-dna', tripId] as const,
};

export function useTripDna(tripId: string) {
  return useQuery({ queryKey: dnaKeys.trip(tripId), queryFn: () => dnaApi.get(tripId) });
}
