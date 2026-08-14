import { useQuery } from '@tanstack/react-query';
import { onThisDayApi } from '../api/onThisDayApi';

export const onThisDayKey = ['profile', 'on-this-day'] as const;

export function useOnThisDay() {
  return useQuery({ queryKey: onThisDayKey, queryFn: onThisDayApi.get });
}
