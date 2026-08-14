-- Run this once in Supabase SQL Editor after all Flyway migrations have run.
-- Replace the placeholder with a password-manager-generated password and do not
-- save the real password in this repository.

CREATE ROLE travel_memory_app
    WITH LOGIN
    PASSWORD 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION
    BYPASSRLS;

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

-- UUIDs currently avoid application sequences, but these grants keep future
-- identity columns usable without granting schema ownership or DDL privileges.
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO travel_memory_app;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO travel_memory_app;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO travel_memory_app;
