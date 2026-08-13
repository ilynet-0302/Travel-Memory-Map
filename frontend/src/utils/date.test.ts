import { describe, expect, it } from 'vitest';
import { formatDateRange, tripDuration } from './date';

describe('travel dates', () => {
  it('formats dates in the same month as a compact range', () => {
    expect(formatDateRange('2026-09-12', '2026-09-16')).toBe('12–16 Sept 2026');
  });

  it('counts both the arrival and departure day', () => {
    expect(tripDuration('2026-09-12', '2026-09-16')).toBe(5);
  });
});
