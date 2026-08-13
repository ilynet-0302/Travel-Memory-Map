import { describe, expect, it } from 'vitest';
import { expensesApi } from './expensesApi';

describe('demo expenses API', () => {
  it('creates exact shares, builds a settlement and deletes an expense', async () => {
    const tripId = `trip-${crypto.randomUUID()}`;
    const created = await expensesApi.create(tripId, {
      title: 'Dinner',
      amount: 100,
      currency: 'eur',
      category: 'FOOD',
      date: '2026-09-12',
      paidByUserId: 'demo-owner',
      participantUserIds: ['demo-owner', 'demo-maya', 'demo-alex'],
    });

    expect(created.participants.reduce((sum, participant) => sum + participant.shareAmount, 0)).toBe(100);
    const overview = await expensesApi.overview(tripId);
    expect(overview.summaries[0]).toMatchObject({ currency: 'EUR', total: 100 });
    expect(overview.summaries[0].settlements).toHaveLength(2);

    await expensesApi.delete(tripId, created.id);
    await expect(expensesApi.overview(tripId)).resolves.toEqual({ expenses: [], summaries: [] });
  });
});

