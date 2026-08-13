import { ApiError, apiClient } from '../../../services/apiClient';
import { demoTrips } from '../../trips/data/demoTrips';
import { expenseCategories, type ExpenseCurrencySummary, type ExpenseInput, type ExpenseOverview, type TripExpense } from '../types';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';
const demoExpenses = new Map<string, TripExpense[]>();
const demoNames: Record<string, string> = {
  'demo-owner': 'Iliya Petrov',
  'demo-maya': 'Maya Stoyanova',
  'demo-alex': 'Alex Marinov',
};

const wait = (milliseconds = 140) =>
  new Promise<void>((resolve) => globalThis.setTimeout(resolve, milliseconds));

function nameFor(userId: string) {
  return demoNames[userId] ?? 'Demo Traveller';
}

function money(value: number) {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

function split(amount: number, participantUserIds: string[]) {
  const totalCents = Math.round(amount * 100);
  const baseCents = Math.floor(totalCents / participantUserIds.length);
  const remainder = totalCents - baseCents * participantUserIds.length;
  return [...participantUserIds].sort().map((userId, index) => ({
    userId,
    displayName: nameFor(userId),
    shareAmount: (baseCents + (index < remainder ? 1 : 0)) / 100,
  }));
}

function settlement(expenses: TripExpense[], currency: string) {
  const balances = new Map<string, number>();
  expenses.forEach((expense) => {
    balances.set(expense.paidByUserId, (balances.get(expense.paidByUserId) ?? 0) + Math.round(expense.amount * 100));
    expense.participants.forEach((participant) => {
      balances.set(participant.userId, (balances.get(participant.userId) ?? 0) - Math.round(participant.shareAmount * 100));
    });
  });
  const debtors = [...balances.entries()].filter(([, cents]) => cents < 0).sort((a, b) => a[1] - b[1]);
  const creditors = [...balances.entries()].filter(([, cents]) => cents > 0).sort((a, b) => b[1] - a[1]);
  const transfers = [];
  let debtorIndex = 0;
  let creditorIndex = 0;
  while (debtorIndex < debtors.length && creditorIndex < creditors.length) {
    const debtor = debtors[debtorIndex];
    const creditor = creditors[creditorIndex];
    const cents = Math.min(-debtor[1], creditor[1]);
    transfers.push({
      fromUserId: debtor[0],
      fromDisplayName: nameFor(debtor[0]),
      toUserId: creditor[0],
      toDisplayName: nameFor(creditor[0]),
      amount: cents / 100,
      currency,
    });
    debtor[1] += cents;
    creditor[1] -= cents;
    if (debtor[1] === 0) debtorIndex += 1;
    if (creditor[1] === 0) creditorIndex += 1;
  }
  return transfers;
}

function demoOverview(tripId: string): ExpenseOverview {
  const expenses = [...(demoExpenses.get(tripId) ?? [])];
  const trip = demoTrips.find(({ id }) => id === tripId);
  const tripDays = trip
    ? Math.max(1, Math.round((new Date(trip.endDate).getTime() - new Date(trip.startDate).getTime()) / 86_400_000) + 1)
    : 1;
  const summaries = [...new Set(expenses.map(({ currency }) => currency))].sort().map((currency): ExpenseCurrencySummary => {
    const currencyExpenses = expenses.filter((expense) => expense.currency === currency);
    const total = money(currencyExpenses.reduce((sum, expense) => sum + expense.amount, 0));
    const memberIds = new Set(currencyExpenses.flatMap((expense) => expense.participants.map(({ userId }) => userId)));
    const categoryTotals = Object.fromEntries(expenseCategories.map((category) => [
      category,
      money(currencyExpenses.filter((expense) => expense.category === category).reduce((sum, expense) => sum + expense.amount, 0)),
    ])) as Record<(typeof expenseCategories)[number], number>;
    const dayTotals = new Map<string, number>();
    currencyExpenses.forEach((expense) => dayTotals.set(expense.date, money((dayTotals.get(expense.date) ?? 0) + expense.amount)));
    const mostExpensiveDay = [...dayTotals.entries()].sort((a, b) => b[1] - a[1])[0];
    const largestExpense = [...currencyExpenses].sort((a, b) => b.amount - a.amount)[0];
    return {
      currency,
      total,
      costPerDay: money(total / tripDays),
      costPerPerson: money(total / Math.max(1, memberIds.size)),
      mostExpensiveDay: { date: mostExpensiveDay[0], amount: mostExpensiveDay[1] },
      largestExpense,
      categoryTotals,
      settlements: settlement(currencyExpenses, currency),
    };
  });
  return { expenses, summaries };
}

export const expensesApi = {
  async overview(tripId: string): Promise<ExpenseOverview> {
    if (!demoMode) return apiClient<ExpenseOverview>(`/trips/${tripId}/expenses`);
    await wait();
    return demoOverview(tripId);
  },

  async create(tripId: string, input: ExpenseInput): Promise<TripExpense> {
    if (!demoMode) {
      return apiClient<TripExpense>(`/trips/${tripId}/expenses`, {
        method: 'POST',
        body: JSON.stringify(input),
      });
    }
    await wait(220);
    if (!input.participantUserIds.length) throw new ApiError('Choose at least one participant.', 400, 'INVALID_EXPENSE');
    const now = new Date().toISOString();
    const expense: TripExpense = {
      id: crypto.randomUUID(),
      tripId,
      ...input,
      amount: money(input.amount),
      currency: input.currency.toUpperCase(),
      paidByDisplayName: nameFor(input.paidByUserId),
      createdByUserId: 'demo-owner',
      participants: split(input.amount, input.participantUserIds),
      createdAt: now,
      updatedAt: now,
    };
    demoExpenses.set(tripId, [expense, ...(demoExpenses.get(tripId) ?? [])]);
    return expense;
  },

  async update(tripId: string, expenseId: string, input: ExpenseInput): Promise<TripExpense> {
    if (!demoMode) {
      return apiClient<TripExpense>(`/trips/${tripId}/expenses/${expenseId}`, {
        method: 'PUT',
        body: JSON.stringify(input),
      });
    }
    await wait(180);
    const existing = (demoExpenses.get(tripId) ?? []).find(({ id }) => id === expenseId);
    if (!existing) throw new ApiError('This expense does not exist.', 404, 'EXPENSE_NOT_FOUND');
    const updated: TripExpense = {
      ...existing,
      ...input,
      amount: money(input.amount),
      currency: input.currency.toUpperCase(),
      paidByDisplayName: nameFor(input.paidByUserId),
      participants: split(input.amount, input.participantUserIds),
      updatedAt: new Date().toISOString(),
    };
    demoExpenses.set(tripId, (demoExpenses.get(tripId) ?? []).map((expense) => expense.id === expenseId ? updated : expense));
    return updated;
  },

  async delete(tripId: string, expenseId: string): Promise<void> {
    if (!demoMode) {
      await apiClient<void>(`/trips/${tripId}/expenses/${expenseId}`, { method: 'DELETE' });
      return;
    }
    await wait();
    demoExpenses.set(tripId, (demoExpenses.get(tripId) ?? []).filter(({ id }) => id !== expenseId));
  },
};

