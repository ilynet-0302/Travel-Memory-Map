ALTER TABLE photos
    ADD COLUMN public_visible BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_photos_public_trip
    ON photos(trip_id, created_at)
    WHERE public_visible = TRUE;

UPDATE trips
SET public_slug = replace(id::text, '-', '')
WHERE visibility = 'PUBLIC'
  AND public_slug IS NULL;

ALTER TABLE trips
    ADD CONSTRAINT ck_trips_public_slug
    CHECK (visibility <> 'PUBLIC' OR public_slug IS NOT NULL);

-- Anonymous Storage access is limited to photos explicitly selected for an
-- active PUBLIC trip. Private trips and unselected photos remain inaccessible.
DO $storage_setup$
BEGIN
    IF to_regclass('storage.objects') IS NOT NULL
       AND to_regprocedure('auth.uid()') IS NOT NULL THEN
        -- Authenticated users keep private-photo access only when they are an
        -- owner/member. PUBLIC visibility alone no longer exposes every photo.
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
                          trip.owner_id = auth.uid()
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
            CREATE OR REPLACE FUNCTION public.can_view_public_trip_photo(requested_object_name text)
            RETURNS boolean
            LANGUAGE sql
            STABLE
            SECURITY DEFINER
            SET search_path = ''
            AS $body$
                SELECT EXISTS (
                    SELECT 1
                    FROM public.photos photo
                    JOIN public.trips trip ON trip.id = photo.trip_id
                    WHERE photo.storage_path = requested_object_name
                      AND photo.public_visible = true
                      AND trip.visibility = 'PUBLIC'
                      AND trip.status <> 'ARCHIVED'
                )
            $body$
        $function$;

        EXECUTE 'REVOKE ALL ON FUNCTION public.can_view_public_trip_photo(text) FROM PUBLIC';
        EXECUTE 'GRANT EXECUTE ON FUNCTION public.can_view_public_trip_photo(text) TO anon, authenticated';

        EXECUTE 'DROP POLICY IF EXISTS trip_photos_public_select ON storage.objects';
        EXECUTE $policy$
            CREATE POLICY trip_photos_public_select
            ON storage.objects
            FOR SELECT
            TO anon, authenticated
            USING (
                bucket_id = 'trip-photos'
                AND public.can_view_public_trip_photo(name)
                AND storage.allow_any_operation(ARRAY[
                    'object.get_authenticated_info',
                    'object.get_authenticated'
                ]::text[])
            )
        $policy$;
    END IF;
END
$storage_setup$;
