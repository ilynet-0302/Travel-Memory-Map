import type { DnaTrait, TravelDnaScores } from './types';

export function orderedDnaScores(scores: TravelDnaScores) {
  return (Object.entries(scores) as [DnaTrait, number][])
    .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]));
}
