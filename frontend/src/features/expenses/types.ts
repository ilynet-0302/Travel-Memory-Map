export const expenseCategories = [
  'FLIGHT',
  'ACCOMMODATION',
  'FOOD',
  'TRANSPORT',
  'ACTIVITY',
  'SHOPPING',
  'OTHER',
] as const;

export type ExpenseCategory = (typeof expenseCategories)[number];

export interface ExpenseParticipant {
  userId: string;
  displayName: string;
  shareAmount: number;
}

export interface TripExpense {
  id: string;
  tripId: string;
  title: string;
  amount: number;
  currency: string;
  category: ExpenseCategory;
  date: string;
  paidByUserId: string;
  paidByDisplayName: string;
  createdByUserId: string;
  participants: ExpenseParticipant[];
  createdAt: string;
  updatedAt: string;
}

export interface ExpenseSettlement {
  fromUserId: string;
  fromDisplayName: string;
  toUserId: string;
  toDisplayName: string;
  amount: number;
  currency: string;
}

export interface ExpenseCurrencySummary {
  currency: string;
  total: number;
  costPerDay: number;
  costPerPerson: number;
  mostExpensiveDay: { date: string; amount: number };
  largestExpense: TripExpense;
  categoryTotals: Record<ExpenseCategory, number>;
  settlements: ExpenseSettlement[];
}

export interface ExpenseOverview {
  expenses: TripExpense[];
  summaries: ExpenseCurrencySummary[];
}

export interface ExpenseInput {
  title: string;
  amount: number;
  currency: string;
  category: ExpenseCategory;
  date: string;
  paidByUserId: string;
  participantUserIds: string[];
}

