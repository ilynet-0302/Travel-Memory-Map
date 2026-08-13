import { ArrowRight, CalendarDays, CircleDollarSign, Pencil, Plus, ReceiptText, Trash2, Users } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useAuth } from '../../auth/context/AuthContext';
import { useTripMembers } from '../../collaboration/hooks/useCollaboration';
import type { Trip } from '../../trips/types';
import { ExpenseDialog } from './ExpenseDialog';
import { useDeleteExpense, useExpenseOverview } from '../hooks/useExpenses';
import { expenseCategories, type ExpenseCategory, type TripExpense } from '../types';

interface ExpensesPanelProps {
  trip: Trip;
}

const categoryLabels: Record<ExpenseCategory, string> = {
  FLIGHT: 'Flights',
  ACCOMMODATION: 'Accommodation',
  FOOD: 'Food',
  TRANSPORT: 'Transport',
  ACTIVITY: 'Activities',
  SHOPPING: 'Shopping',
  OTHER: 'Other',
};

function formatMoney(value: number, currency: string) {
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency, maximumFractionDigits: 2 }).format(value);
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
    .format(new Date(`${value}T00:00:00`));
}

export function ExpensesPanel({ trip }: ExpensesPanelProps) {
  const overview = useExpenseOverview(trip.id);
  const members = useTripMembers(trip.id);
  const deleteExpense = useDeleteExpense(trip.id);
  const { session, demoMode } = useAuth();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedExpense, setSelectedExpense] = useState<TripExpense>();
  const [selectedCurrency, setSelectedCurrency] = useState('');
  const summaries = useMemo(() => overview.data?.summaries ?? [], [overview.data?.summaries]);
  const summary = summaries.find(({ currency }) => currency === selectedCurrency) ?? summaries[0];
  const shownExpenses = useMemo(
    () => (overview.data?.expenses ?? []).filter((expense) => !summary || expense.currency === summary.currency),
    [overview.data?.expenses, summary],
  );
  const canAdd = trip.currentUserRole === 'OWNER' || trip.currentUserRole === 'EDITOR';
  const currentUserId = demoMode ? 'demo-owner' : session?.user.id;

  const openCreate = () => {
    setSelectedExpense(undefined);
    setDialogOpen(true);
  };

  const openEdit = (expense: TripExpense) => {
    setSelectedExpense(expense);
    setDialogOpen(true);
  };

  const canModify = (expense: TripExpense) =>
    trip.currentUserRole === 'OWNER'
    || (trip.currentUserRole === 'EDITOR' && expense.createdByUserId === currentUserId);

  const confirmDelete = async (expense: TripExpense) => {
    if (!window.confirm(`Delete “${expense.title}”? This will recalculate the settlement.`)) return;
    try {
      await deleteExpense.mutateAsync(expense.id);
    } catch {
      return;
    }
  };

  if (overview.isLoading || members.isLoading) return <div className="expenses-skeleton detail-skeleton" />;
  if (overview.error || members.error) return <p className="photo-error" role="alert">{overview.error?.message ?? members.error?.message}</p>;

  const largestCategoryValue = summary
    ? Math.max(1, ...Object.values(summary.categoryTotals))
    : 1;

  return (
    <section className="expenses-layout">
      <header className="expenses-heading">
        <div><span className="eyebrow">TRIP WALLET</span><h2>Shared expenses</h2><p>Track costs together and settle up with the fewest transfers.</p></div>
        {canAdd && <button className="button button--coral" type="button" disabled={!members.data?.length} onClick={openCreate}><Plus size={16} /> Add expense</button>}
      </header>

      {summaries.length > 1 && (
        <div className="currency-tabs" aria-label="Expense currencies">
          {summaries.map(({ currency }) => <button key={currency} type="button" className={currency === summary?.currency ? 'is-active' : ''} onClick={() => setSelectedCurrency(currency)}>{currency}</button>)}
        </div>
      )}

      {summary ? (
        <>
          <div className="expense-summary-grid">
            <article className="expense-summary-card expense-summary-card--total"><span><CircleDollarSign size={19} /></span><small>TOTAL SPENT</small><strong>{formatMoney(summary.total, summary.currency)}</strong></article>
            <article className="expense-summary-card"><span><CalendarDays size={18} /></span><small>PER DAY</small><strong>{formatMoney(summary.costPerDay, summary.currency)}</strong></article>
            <article className="expense-summary-card"><span><Users size={18} /></span><small>PER PERSON</small><strong>{formatMoney(summary.costPerPerson, summary.currency)}</strong></article>
            <article className="expense-summary-card"><span><ReceiptText size={18} /></span><small>LARGEST EXPENSE</small><strong>{formatMoney(summary.largestExpense.amount, summary.currency)}</strong><em>{summary.largestExpense.title}</em></article>
          </div>

          <div className="expenses-dashboard-grid">
            <section className="expense-breakdown-card">
              <div className="expense-section-title"><div><span className="eyebrow">BREAKDOWN</span><h3>Where the money went</h3></div><small>Most expensive day<br /><strong>{formatDate(summary.mostExpensiveDay.date)} · {formatMoney(summary.mostExpensiveDay.amount, summary.currency)}</strong></small></div>
              <div className="expense-category-list">
                {expenseCategories.filter((category) => summary.categoryTotals[category] > 0).map((category) => (
                  <div key={category}>
                    <span>{categoryLabels[category]}</span>
                    <span className="expense-category-bar"><i style={{ width: `${(summary.categoryTotals[category] / largestCategoryValue) * 100}%` }} /></span>
                    <strong>{formatMoney(summary.categoryTotals[category], summary.currency)}</strong>
                  </div>
                ))}
              </div>
            </section>

            <aside className="settlement-card">
              <span className="eyebrow">SETTLE UP</span>
              <h3>Who owes whom</h3>
              {summary.settlements.length ? (
                <div className="settlement-list">
                  {summary.settlements.map((transfer) => (
                    <article key={`${transfer.fromUserId}-${transfer.toUserId}`}>
                      <span className="settlement-avatar">{transfer.fromDisplayName.charAt(0)}</span>
                      <div><strong>{transfer.fromDisplayName}</strong><small>pays {transfer.toDisplayName}</small></div>
                      <ArrowRight size={15} />
                      <b>{formatMoney(transfer.amount, transfer.currency)}</b>
                    </article>
                  ))}
                </div>
              ) : <div className="settlement-empty"><CircleDollarSign size={25} /><strong>All settled</strong><small>No transfers are needed.</small></div>}
            </aside>
          </div>

          <section className="expense-list-card">
            <div className="expense-section-title"><div><span className="eyebrow">LEDGER</span><h3>Every expense</h3></div><span>{shownExpenses.length} entries</span></div>
            <div className="expense-list">
              {shownExpenses.map((expense) => (
                <article key={expense.id}>
                  <span className={`expense-category-icon expense-category-icon--${expense.category.toLowerCase()}`}><ReceiptText size={17} /></span>
                  <div className="expense-list__copy"><strong>{expense.title}</strong><small>{categoryLabels[expense.category]} · {formatDate(expense.date)}</small></div>
                  <div className="expense-list__people"><small>Paid by {expense.paidByDisplayName}</small><span>Split between {expense.participants.map(({ displayName }) => displayName).join(', ')}</span></div>
                  <strong className="expense-list__amount">{formatMoney(expense.amount, expense.currency)}</strong>
                  {canModify(expense) && <div className="expense-list__actions"><button type="button" aria-label={`Edit ${expense.title}`} onClick={() => openEdit(expense)}><Pencil size={14} /></button><button type="button" aria-label={`Delete ${expense.title}`} disabled={deleteExpense.isPending} onClick={() => void confirmDelete(expense)}><Trash2 size={14} /></button></div>}
                </article>
              ))}
            </div>
          </section>
        </>
      ) : (
        <div className="expenses-empty">
          <span><CircleDollarSign size={28} /></span><h3>No expenses yet</h3><p>Add the first shared cost and the settlement will calculate automatically.</p>
          {canAdd && <button className="button button--coral" type="button" disabled={!members.data?.length} onClick={openCreate}>Add first expense</button>}
        </div>
      )}

      {(deleteExpense.error) && <p className="photo-error" role="alert">{deleteExpense.error.message}</p>}
      <ExpenseDialog open={dialogOpen} trip={trip} members={members.data ?? []} expense={selectedExpense} onClose={() => setDialogOpen(false)} />
    </section>
  );
}
