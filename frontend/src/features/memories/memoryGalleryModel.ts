import type { MemoryGalleryFilters, MemoryPhoto } from './types';

export function memoryDate(memory: MemoryPhoto) {
  return memory.takenAt ?? memory.createdAt;
}

export function filterMemories(memories: MemoryPhoto[], filters: MemoryGalleryFilters) {
  const query = filters.query.trim().toLowerCase();
  return memories.filter((memory) => {
    const searchable = [
      memory.caption,
      memory.originalFileName,
      memory.tripTitle,
      memory.country,
      memory.city,
      memory.tripStopName,
      memory.uploadedByDisplayName,
    ].filter(Boolean).join(' ').toLowerCase();
    return (!query || searchable.includes(query))
      && (filters.year === 'ALL' || memoryDate(memory).slice(0, 4) === filters.year)
      && (filters.tripId === 'ALL' || memory.tripId === filters.tripId)
      && (!filters.locationOnly || memory.latitude !== null || memory.tripStopId !== null);
  });
}

export function groupMemoriesByYear(memories: MemoryPhoto[]) {
  const groups = new Map<string, MemoryPhoto[]>();
  memories.forEach((memory) => {
    const year = memoryDate(memory).slice(0, 4);
    groups.set(year, [...(groups.get(year) ?? []), memory]);
  });
  return [...groups.entries()].sort(([left], [right]) => right.localeCompare(left));
}
