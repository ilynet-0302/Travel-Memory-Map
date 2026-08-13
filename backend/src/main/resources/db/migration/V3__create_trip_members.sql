CREATE TABLE trip_members (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id),
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_trip_members_trip_user UNIQUE (trip_id, user_id),
    CONSTRAINT ck_trip_members_role CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER'))
);

CREATE INDEX idx_trip_members_user ON trip_members(user_id);
CREATE UNIQUE INDEX uk_trip_single_owner ON trip_members(trip_id) WHERE role = 'OWNER';
