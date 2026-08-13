ALTER TABLE trip_ratings RENAME COLUMN score TO overall_score;
ALTER TABLE trip_ratings RENAME CONSTRAINT ck_trip_ratings_score TO ck_trip_ratings_overall_score;

ALTER TABLE trip_ratings
    ADD COLUMN food SMALLINT,
    ADD COLUMN nightlife SMALLINT,
    ADD COLUMN culture SMALLINT,
    ADD COLUMN nature SMALLINT,
    ADD COLUMN walkability SMALLINT,
    ADD COLUMN value_for_money SMALLINT,
    ADD COLUMN crowds SMALLINT,
    ADD COLUMN relaxation SMALLINT,
    ADD COLUMN would_return VARCHAR(10);

UPDATE trip_ratings
SET food = overall_score,
    nightlife = overall_score,
    culture = overall_score,
    nature = overall_score,
    walkability = overall_score,
    value_for_money = overall_score,
    crowds = overall_score,
    relaxation = overall_score,
    would_return = 'MAYBE';

ALTER TABLE trip_ratings
    ALTER COLUMN food SET NOT NULL,
    ALTER COLUMN nightlife SET NOT NULL,
    ALTER COLUMN culture SET NOT NULL,
    ALTER COLUMN nature SET NOT NULL,
    ALTER COLUMN walkability SET NOT NULL,
    ALTER COLUMN value_for_money SET NOT NULL,
    ALTER COLUMN crowds SET NOT NULL,
    ALTER COLUMN relaxation SET NOT NULL,
    ALTER COLUMN would_return SET NOT NULL,
    ADD CONSTRAINT ck_trip_ratings_food CHECK (food BETWEEN 1 AND 10),
    ADD CONSTRAINT ck_trip_ratings_nightlife CHECK (nightlife BETWEEN 1 AND 10),
    ADD CONSTRAINT ck_trip_ratings_culture CHECK (culture BETWEEN 1 AND 10),
    ADD CONSTRAINT ck_trip_ratings_nature CHECK (nature BETWEEN 1 AND 10),
    ADD CONSTRAINT ck_trip_ratings_walkability CHECK (walkability BETWEEN 1 AND 10),
    ADD CONSTRAINT ck_trip_ratings_value_for_money CHECK (value_for_money BETWEEN 1 AND 10),
    ADD CONSTRAINT ck_trip_ratings_crowds CHECK (crowds BETWEEN 1 AND 10),
    ADD CONSTRAINT ck_trip_ratings_relaxation CHECK (relaxation BETWEEN 1 AND 10),
    ADD CONSTRAINT ck_trip_ratings_would_return CHECK (would_return IN ('YES', 'MAYBE', 'NO'));
