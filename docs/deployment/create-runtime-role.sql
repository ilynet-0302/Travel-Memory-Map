-- Run this once in Supabase SQL Editor after all Flyway migrations have run.
-- Replace the placeholder with a password-manager-generated password and do not
-- save the real password in this repository.

CREATE ROLE travel_memory_app
    WITH LOGIN
    PASSWORD 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    NOBYPASSRLS;

GRANT CONNECT ON DATABASE postgres TO travel_memory_app;
GRANT USAGE ON SCHEMA public TO travel_memory_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    profiles,
    trips,
    trip_members,
    trip_days,
    trip_stops,
    trip_invites,
    photos,
    expenses,
    expense_participants,
    trip_ratings
TO travel_memory_app;

-- The Spring API performs authorization before database access. These policies
-- let only its restricted runtime role reach application rows; browser-facing
-- anon/authenticated roles remain blocked by V12. Policies and grants for any
-- future table must be added explicitly instead of being inherited by default.
CREATE POLICY travel_memory_backend_access ON profiles
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
CREATE POLICY travel_memory_backend_access ON trips
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
CREATE POLICY travel_memory_backend_access ON trip_members
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
CREATE POLICY travel_memory_backend_access ON trip_days
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
CREATE POLICY travel_memory_backend_access ON trip_stops
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
CREATE POLICY travel_memory_backend_access ON trip_invites
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
CREATE POLICY travel_memory_backend_access ON photos
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
CREATE POLICY travel_memory_backend_access ON expenses
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
CREATE POLICY travel_memory_backend_access ON expense_participants
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
CREATE POLICY travel_memory_backend_access ON trip_ratings
    FOR ALL TO travel_memory_app USING (true) WITH CHECK (true);
