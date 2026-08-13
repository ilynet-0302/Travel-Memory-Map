import { apiClient } from '../../../services/apiClient';
import type { TripDna } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';

const demoDna: Omit<TripDna, 'tripId'> = {
  scores: {
    explorer: 82,
    foodie: 71,
    culture: 64,
    nightlife: 20,
    relaxation: 34,
    nature: 41,
    adventure: 57,
  },
  dominantTrait: 'EXPLORER',
  signals: [
    '8 saved places across 5 days',
    'landmark is the strongest place signal',
    'food appears most often in expenses',
  ],
};

export const dnaApi = {
  async get(tripId: string): Promise<TripDna> {
    if (!demoMode) return apiClient<TripDna>(`/trips/${tripId}/dna`);
    return { tripId, ...demoDna };
  },
};
