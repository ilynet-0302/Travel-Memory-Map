import { zodResolver } from '@hookform/resolvers/zod';
import { CircleDollarSign, X } from 'lucide-react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import type { TripMember } from '../../collaboration/types';
import type { Trip } from '../../trips/types';
import { useCreateExpense, useUpdateExpense } from '../hooks/useExpenses';
import { expenseCategories, type ExpenseInput, type TripExpense } from '../types';

const expenseSchema = z.object({
  title: z.string().trim().min(2, 'Expense title is required.').max(160),
  amount: z.string().refine((value) => /^\d+(\.\d{1,2})?$/.test(value) && Number(value) > 0, 'Enter a positive amount with up to 2 decimals.'),
  currency: z.string().trim().regex(/^[A-Za-z]{3}$/, 'Use a three-letter currency code.'),
  category: z.enum(expenseCategories),
  date: z.string().min(1, 'Expense date is required.'),
  paidByUserId: z.string().min(1, 'Choose who paid.'),
  participantUserIds: z.array(z.string()).min(1, 'Choose at least one participant.'),
});

type ExpenseForm = z.infer<typeof expenseSchema>;

interface ExpenseDialogProps {
  open: boolean;
  trip: Trip;
  members: TripMember[];
  expense?: TripExpense;
  onClose: () => void;
}

export function ExpenseDialog({ open, trip, members, expense, onClose }: ExpenseDialogProps) {
  const createExpense = useCreateExpense(trip.id);
  const updateExpense = useUpdateExpense(trip.id);
  const mutation = expense ? updateExpense : createExpense;
  const { register, handleSubmit, reset, formState: { errors } } = useForm<ExpenseForm>({
    resolver: zodResolver(expenseSchema),
    defaultValues: { participantUserIds: [] },
  });

  useEffect(() => {
    if (!open) return;
    reset(expense ? {
      title: expense.title,
      amount: String(expense.amount),
      currency: expense.currency,
      category: expense.category,
      date: expense.date,
      paidByUserId: expense.paidByUserId,
      participantUserIds: expense.participants.map(({ userId }) => userId),
    } : {
      title: '',
      amount: '',
      currency: 'EUR',
      category: 'FOOD',
      date: trip.startDate,
      paidByUserId: members[0]?.userId ?? '',
      participantUserIds: members.map(({ userId }) => userId),
    });
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    document.body.classList.add('is-modal-open');
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.classList.remove('is-modal-open');
    };
  }, [expense, members, onClose, open, reset, trip.startDate]);

  if (!open) return null;

  const submit = handleSubmit(async (values) => {
    const input: ExpenseInput = {
      ...values,
      title: values.title.trim(),
      amount: Number(values.amount),
      currency: values.currency.trim().toUpperCase(),
    };
    try {
      if (expense) await updateExpense.mutateAsync({ expenseId: expense.id, input });
      else await createExpense.mutateAsync(input);
      onClose();
    } catch {
      return;
    }
  });

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card expense-dialog" role="dialog" aria-modal="true" aria-labelledby="expense-dialog-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-card__header">
          <div><span className="eyebrow">SHARED COST</span><h2 id="expense-dialog-title">{expense ? 'Edit expense' : 'Add an expense'}</h2></div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close dialog"><X size={20} /></button>
        </header>
        <form className="trip-form" onSubmit={submit}>
          <label className="field field--full"><span>Title</span><input autoFocus placeholder="Dinner near the Colosseum" {...register('title')} />{errors.title && <small className="field__error">{errors.title.message}</small>}</label>
          <label className="field"><span>Amount</span><span className="input-with-icon"><CircleDollarSign size={17} /><input inputMode="decimal" placeholder="120.00" {...register('amount')} /></span>{errors.amount && <small className="field__error">{errors.amount.message}</small>}</label>
          <label className="field"><span>Currency</span><input maxLength={3} placeholder="EUR" {...register('currency')} />{errors.currency && <small className="field__error">{errors.currency.message}</small>}</label>
          <label className="field"><span>Category</span><select {...register('category')}>{expenseCategories.map((category) => <option key={category} value={category}>{category.toLowerCase()}</option>)}</select></label>
          <label className="field"><span>Date</span><input type="date" min={trip.startDate} max={trip.endDate} {...register('date')} />{errors.date && <small className="field__error">{errors.date.message}</small>}</label>
          <label className="field field--full"><span>Paid by</span><select {...register('paidByUserId')}>{members.map((member) => <option key={member.userId} value={member.userId}>{member.displayName}</option>)}</select>{errors.paidByUserId && <small className="field__error">{errors.paidByUserId.message}</small>}</label>
          <fieldset className="expense-participants field--full">
            <legend>Split equally between</legend>
            <div>
              {members.map((member) => (
                <label key={member.userId}>
                  <input type="checkbox" value={member.userId} {...register('participantUserIds')} />
                  <span>{member.displayName}<small>{member.role.toLowerCase()}</small></span>
                </label>
              ))}
            </div>
            {errors.participantUserIds && <small className="field__error">{errors.participantUserIds.message}</small>}
          </fieldset>
          {mutation.error && <p className="form-error field--full">{mutation.error.message}</p>}
          <footer className="modal-card__footer field--full">
            <button className="button button--ghost" type="button" onClick={onClose}>Cancel</button>
            <button className="button button--coral" type="submit" disabled={mutation.isPending || !members.length}>{mutation.isPending ? 'Saving…' : expense ? 'Save expense' : 'Add expense'}</button>
          </footer>
        </form>
      </section>
    </div>
  );
}

