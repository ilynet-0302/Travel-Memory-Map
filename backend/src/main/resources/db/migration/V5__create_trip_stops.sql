CREATE TABLE trip_stops (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES profiles(id),
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    arrival_time TIMESTAMPTZ NOT NULL,
    departure_time TIMESTAMPTZ,
    category VARCHAR(30) NOT NULL,
    rating INTEGER,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_trip_stops_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_trip_stops_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_trip_stops_times CHECK (departure_time IS NULL OR departure_time >= arrival_time),
    CONSTRAINT ck_trip_stops_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 10),
    CONSTRAINT ck_trip_stops_position CHECK (position >= 0),
    CONSTRAINT ck_trip_stops_category CHECK (category IN (
        'LANDMARK', 'RESTAURANT', 'HOTEL', 'AIRPORT', 'BEACH', 'MUSEUM',
        'BAR', 'SHOP', 'NATURE', 'TRANSPORT', 'OTHER'
    ))
);

CREATE INDEX idx_trip_stops_trip_position ON trip_stops(trip_id, position);
CREATE INDEX idx_trip_stops_arrival ON trip_stops(trip_id, arrival_time);
