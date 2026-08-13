CREATE TABLE trip_invites (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES profiles(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    max_uses INTEGER NOT NULL,
    use_count INTEGER NOT NULL DEFAULT 0,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_trip_invites_role CHECK (role IN ('EDITOR', 'VIEWER')),
    CONSTRAINT ck_trip_invites_max_uses CHECK (max_uses > 0),
    CONSTRAINT ck_trip_invites_use_count CHECK (use_count >= 0 AND use_count <= max_uses),
    CONSTRAINT ck_trip_invites_token_hash CHECK (char_length(token_hash) = 64)
);

CREATE INDEX idx_trip_invites_trip_created ON trip_invites(trip_id, created_at DESC);
CREATE INDEX idx_trip_invites_active ON trip_invites(trip_id, expires_at)
    WHERE revoked_at IS NULL AND use_count < max_uses;
