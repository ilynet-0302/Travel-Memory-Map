export interface TravelDnaScores {
  explorer: number;
  foodie: number;
  culture: number;
  nightlife: number;
  relaxation: number;
  nature: number;
  adventure: number;
}

export type DnaTrait = keyof TravelDnaScores;

export interface TripDna {
  tripId: string;
  scores: TravelDnaScores;
  dominantTrait: Uppercase<DnaTrait>;
  signals: string[];
}

export interface TravelPersonality {
  type: string;
  description: string;
  revealed: boolean;
  completedTrips: number;
  completedTripsRequired: number;
  scores: TravelDnaScores;
}
