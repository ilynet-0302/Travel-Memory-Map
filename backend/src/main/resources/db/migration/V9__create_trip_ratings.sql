CREATE TABLE trip_ratings (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    score SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_trip_ratings_trip_user UNIQUE (trip_id, user_id),
    CONSTRAINT ck_trip_ratings_score CHECK (score BETWEEN 1 AND 10)
);

CREATE INDEX idx_trip_ratings_trip ON trip_ratings(trip_id);
CREATE INDEX idx_trip_ratings_user ON trip_ratings(user_id);

ALTER TABLE trip_ratings ENABLE ROW LEVEL SECURITY;
