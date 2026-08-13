CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    category VARCHAR(30) NOT NULL,
    expense_date DATE NOT NULL,
    paid_by_user_id UUID NOT NULL REFERENCES profiles(id),
    created_by_user_id UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_expenses_amount CHECK (amount > 0),
    CONSTRAINT ck_expenses_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_expenses_category CHECK (
        category IN ('FLIGHT', 'ACCOMMODATION', 'FOOD', 'TRANSPORT', 'ACTIVITY', 'SHOPPING', 'OTHER')
    )
);

CREATE TABLE expense_participants (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id),
    share_amount NUMERIC(14, 2) NOT NULL,
    CONSTRAINT uk_expense_participants_expense_user UNIQUE (expense_id, user_id),
    CONSTRAINT ck_expense_participants_share CHECK (share_amount >= 0)
);

CREATE INDEX idx_expenses_trip_date ON expenses(trip_id, expense_date DESC, created_at DESC);
CREATE INDEX idx_expenses_paid_by ON expenses(paid_by_user_id);
CREATE INDEX idx_expenses_created_by ON expenses(created_by_user_id);
CREATE INDEX idx_expense_participants_user ON expense_participants(user_id);

ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE expense_participants ENABLE ROW LEVEL SECURITY;

