CREATE TABLE photos (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    trip_stop_id UUID REFERENCES trip_stops(id) ON DELETE SET NULL,
    uploaded_by_user_id UUID NOT NULL REFERENCES profiles(id),
    storage_path VARCHAR(600) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    taken_at TIMESTAMPTZ,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    caption VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_photos_file_size CHECK (file_size > 0 AND file_size <= 10485760),
    CONSTRAINT ck_photos_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    )
);

CREATE INDEX idx_photos_trip_created ON photos(trip_id, created_at);
CREATE INDEX idx_photos_trip_stop ON photos(trip_stop_id) WHERE trip_stop_id IS NOT NULL;
CREATE INDEX idx_photos_uploader ON photos(uploaded_by_user_id);

ALTER TABLE photos ENABLE ROW LEVEL SECURITY;

-- Supabase projects expose the storage/auth schemas. Plain PostgreSQL used by
-- integration tests does not, so Storage setup is deliberately conditional.
DO $storage_setup$
BEGIN
    IF to_regclass('storage.buckets') IS NOT NULL
       AND to_regclass('storage.objects') IS NOT NULL
       AND to_regprocedure('auth.uid()') IS NOT NULL THEN
        INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
        VALUES (
            'trip-photos',
            'trip-photos',
            false,
            10485760,
            ARRAY['image/jpeg', 'image/png', 'image/webp']::text[]
        )
        ON CONFLICT (id) DO UPDATE
        SET public = false,
            file_size_limit = EXCLUDED.file_size_limit,
            allowed_mime_types = EXCLUDED.allowed_mime_types;

        EXECUTE $function$
            CREATE OR REPLACE FUNCTION public.can_view_trip_storage(requested_trip_id uuid)
            RETURNS boolean
            LANGUAGE sql
            STABLE
            SECURITY DEFINER
            SET search_path = ''
            AS $body$
                SELECT EXISTS (
                    SELECT 1
                    FROM public.trips trip
                    WHERE trip.id = requested_trip_id
                      AND (
                          trip.visibility = 'PUBLIC'
                          OR trip.owner_id = auth.uid()
                          OR EXISTS (
                              SELECT 1
                              FROM public.trip_members member
                              WHERE member.trip_id = trip.id
                                AND member.user_id = auth.uid()
                          )
                      )
                )
            $body$
        $function$;

        EXECUTE $function$
            CREATE OR REPLACE FUNCTION public.can_manage_trip_storage(requested_trip_id uuid)
            RETURNS boolean
            LANGUAGE sql
            STABLE
            SECURITY DEFINER
            SET search_path = ''
            AS $body$
                SELECT EXISTS (
                    SELECT 1
                    FROM public.trip_members member
                    WHERE member.trip_id = requested_trip_id
                      AND member.user_id = auth.uid()
                      AND member.role IN ('OWNER', 'EDITOR')
                )
            $body$
        $function$;

        EXECUTE 'REVOKE ALL ON FUNCTION public.can_view_trip_storage(uuid) FROM PUBLIC';
        EXECUTE 'REVOKE ALL ON FUNCTION public.can_manage_trip_storage(uuid) FROM PUBLIC';
        EXECUTE 'GRANT EXECUTE ON FUNCTION public.can_view_trip_storage(uuid) TO authenticated';
        EXECUTE 'GRANT EXECUTE ON FUNCTION public.can_manage_trip_storage(uuid) TO authenticated';

        EXECUTE 'DROP POLICY IF EXISTS trip_photos_select ON storage.objects';
        EXECUTE 'DROP POLICY IF EXISTS trip_photos_insert ON storage.objects';
        EXECUTE 'DROP POLICY IF EXISTS trip_photos_delete ON storage.objects';

        EXECUTE $policy$
            CREATE POLICY trip_photos_select
            ON storage.objects
            FOR SELECT
            TO authenticated
            USING (
                bucket_id = 'trip-photos'
                AND public.can_view_trip_storage(((storage.foldername(name))[1])::uuid)
            )
        $policy$;

        EXECUTE $policy$
            CREATE POLICY trip_photos_insert
            ON storage.objects
            FOR INSERT
            TO authenticated
            WITH CHECK (
                bucket_id = 'trip-photos'
                AND public.can_manage_trip_storage(((storage.foldername(name))[1])::uuid)
            )
        $policy$;

        EXECUTE $policy$
            CREATE POLICY trip_photos_delete
            ON storage.objects
            FOR DELETE
            TO authenticated
            USING (
                bucket_id = 'trip-photos'
                AND public.can_manage_trip_storage(((storage.foldername(name))[1])::uuid)
            )
        $policy$;
    END IF;
END
$storage_setup$;
