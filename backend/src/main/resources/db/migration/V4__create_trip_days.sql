CREATE TABLE trip_days (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    day_number INTEGER NOT NULL,
    trip_date DATE NOT NULL,
    title VARCHAR(160),
    notes VARCHAR(2000),
    CONSTRAINT uk_trip_days_number UNIQUE (trip_id, day_number),
    CONSTRAINT uk_trip_days_date UNIQUE (trip_id, trip_date),
    CONSTRAINT ck_trip_days_number CHECK (day_number > 0)
);
