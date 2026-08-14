import { useQuery } from '@tanstack/react-query';
import { memoryGalleryApi } from '../api/memoryGalleryApi';

export const memoryGalleryKey = ['profile', 'memories'] as const;

export function useMemoryGallery() {
  return useQuery({ queryKey: memoryGalleryKey, queryFn: memoryGalleryApi.get });
}
