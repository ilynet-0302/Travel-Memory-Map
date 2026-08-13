import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { tripKeys } from '../../trips/hooks/useTrips';
import { expensesApi } from '../api/expensesApi';
import type { ExpenseInput } from '../types';

export const expenseKeys = {
  overview: (tripId: string) => ['trip-expenses', tripId] as const,
};

export function useExpenseOverview(tripId: string) {
  return useQuery({ queryKey: expenseKeys.overview(tripId), queryFn: () => expensesApi.overview(tripId) });
}

function useRefreshExpenses(tripId: string) {
  const queryClient = useQueryClient();
  return async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: expenseKeys.overview(tripId) }),
      queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) }),
      queryClient.invalidateQueries({ queryKey: ['profile'] }),
    ]);
  };
}

export function useCreateExpense(tripId: string) {
  const refresh = useRefreshExpenses(tripId);
  return useMutation({
    mutationFn: (input: ExpenseInput) => expensesApi.create(tripId, input),
    onSuccess: refresh,
  });
}

export function useUpdateExpense(tripId: string) {
  const refresh = useRefreshExpenses(tripId);
  return useMutation({
    mutationFn: ({ expenseId, input }: { expenseId: string; input: ExpenseInput }) =>
      expensesApi.update(tripId, expenseId, input),
    onSuccess: refresh,
  });
}

export function useDeleteExpense(tripId: string) {
  const refresh = useRefreshExpenses(tripId);
  return useMutation({
    mutationFn: (expenseId: string) => expensesApi.delete(tripId, expenseId),
    onSuccess: refresh,
  });
}

