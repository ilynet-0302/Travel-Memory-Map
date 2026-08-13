CREATE TABLE trips (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES profiles(id),
    title VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    country VARCHAR(100) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    city VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    cover_image_url VARCHAR(500),
    public_slug VARCHAR(140) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_trips_date_range CHECK (end_date >= start_date),
    CONSTRAINT ck_trips_status CHECK (status IN ('UPCOMING', 'ACTIVE', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT ck_trips_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    CONSTRAINT ck_trips_country_code CHECK (char_length(country_code) = 2)
);

CREATE INDEX idx_trips_owner ON trips(owner_id);
CREATE INDEX idx_trips_dates ON trips(start_date, end_date);
CREATE INDEX idx_trips_visibility ON trips(visibility) WHERE status <> 'ARCHIVED';
