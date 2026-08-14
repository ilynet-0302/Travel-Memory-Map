-- The browser uses Supabase Auth and Storage, but application records are served
-- only by the Spring API. RLS plus revoked Data API roles keeps that boundary in
-- place even if the Supabase Data API is accidentally enabled later.
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE trips ENABLE ROW LEVEL SECURITY;
ALTER TABLE trip_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE trip_days ENABLE ROW LEVEL SECURITY;
ALTER TABLE trip_stops ENABLE ROW LEVEL SECURITY;
ALTER TABLE trip_invites ENABLE ROW LEVEL SECURITY;
ALTER TABLE photos ENABLE ROW LEVEL SECURITY;
ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE expense_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE trip_ratings ENABLE ROW LEVEL SECURITY;

DO $data_api_roles$
DECLARE
    application_tables text :=
        'profiles, trips, trip_members, trip_days, trip_stops, trip_invites, '
        'photos, expenses, expense_participants, trip_ratings';
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        EXECUTE 'REVOKE ALL PRIVILEGES ON TABLE ' || application_tables || ' FROM anon';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        EXECUTE 'REVOKE ALL PRIVILEGES ON TABLE ' || application_tables || ' FROM authenticated';
    END IF;
END
$data_api_roles$;
