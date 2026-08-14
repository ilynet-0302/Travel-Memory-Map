import { describe, expect, it } from 'vitest';
import { tripDayNumber } from './tripDays';

describe('tripDayNumber', () => {
  it('uses the selected trip-local calendar date', () => {
    expect(tripDayNumber('2026-08-18', '2026-08-18')).toBe(1);
    expect(tripDayNumber('2026-08-19', '2026-08-18')).toBe(2);
    expect(tripDayNumber('2026-08-31', '2026-08-18')).toBe(14);
  });
});
