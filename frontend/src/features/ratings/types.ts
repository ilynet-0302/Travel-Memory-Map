export interface TripRatingSummary {
  averageScore: number | null;
  ratingCount: number;
  currentUserScore: number | null;
  canRate: boolean;
}
