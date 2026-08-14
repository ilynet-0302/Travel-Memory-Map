ALTER TABLE trip_stops
    ADD COLUMN arrival_local_datetime TIMESTAMP,
    ADD COLUMN departure_local_datetime TIMESTAMP;

COMMENT ON COLUMN trip_stops.arrival_local_datetime IS
    'Wall-clock date and time selected for the trip, preserved independently of UTC conversion.';
COMMENT ON COLUMN trip_stops.departure_local_datetime IS
    'Optional wall-clock departure selected for the trip, preserved independently of UTC conversion.';
