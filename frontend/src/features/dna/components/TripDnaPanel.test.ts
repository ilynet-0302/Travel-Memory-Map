import { describe, expect, it } from 'vitest';
import { orderedDnaScores } from '../utils';

describe('Trip DNA score ordering', () => {
  it('orders traits from the strongest to the weakest with stable ties', () => {
    expect(orderedDnaScores({
      explorer: 80,
      foodie: 65,
      culture: 80,
      nightlife: 20,
      relaxation: 40,
      nature: 55,
      adventure: 70,
    }).map(([trait]) => trait)).toEqual([
      'culture', 'explorer', 'adventure', 'foodie', 'nature', 'relaxation', 'nightlife',
    ]);
  });
});
