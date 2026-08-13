import { useQuery } from '@tanstack/react-query';
import { worldMapApi } from '../api/worldMapApi';

export const worldMapKeys = {
  current: ['profile', 'world-map'] as const,
};

export function useWorldMap() {
  return useQuery({ queryKey: worldMapKeys.current, queryFn: worldMapApi.get });
}
