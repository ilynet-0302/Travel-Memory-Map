# Database schema

## Current model

```mermaid
erDiagram
    PROFILES ||--o{ TRIPS : owns
    PROFILES ||--o{ TRIP_MEMBERS : joins
    TRIPS ||--|{ TRIP_MEMBERS : authorizes
    TRIPS ||--o{ TRIP_DAYS : groups
    TRIPS ||--o{ TRIP_STOPS : contains
    PROFILES ||--o{ TRIP_STOPS : creates
    PROFILES ||--o{ TRIP_INVITES : creates
    TRIPS ||--o{ TRIP_INVITES : issues
    TRIPS ||--o{ PHOTOS : contains
    TRIP_STOPS ||--o{ PHOTOS : groups
    PROFILES ||--o{ PHOTOS : uploads
    TRIPS ||--o{ EXPENSES : tracks
    PROFILES ||--o{ EXPENSES : pays
    EXPENSES ||--|{ EXPENSE_PARTICIPANTS : splits
    PROFILES ||--o{ EXPENSE_PARTICIPANTS : owes
    TRIPS ||--o{ TRIP_RATINGS : receives
    PROFILES ||--o{ TRIP_RATINGS : submits

    PROFILES {
      uuid id PK
      varchar email UK
      varchar display_name
      varchar avatar_url
    }
    TRIPS {
      uuid id PK
      uuid owner_id FK
      varchar title
      date start_date
      date end_date
      varchar visibility
      varchar status
      varchar public_slug UK
      bigint version
    }
    TRIP_MEMBERS {
      uuid id PK
      uuid trip_id FK
      uuid user_id FK
      varchar role
    }
    TRIP_DAYS {
      uuid id PK
      uuid trip_id FK
      int day_number
      date trip_date
    }
    TRIP_STOPS {
      uuid id PK
      uuid trip_id FK
      numeric latitude
      numeric longitude
      timestamptz arrival_time
      timestamptz departure_time
      timestamp arrival_local_datetime
      timestamp departure_local_datetime
      varchar category
      int position
    }
    TRIP_INVITES {
      uuid id PK
      uuid trip_id FK
      uuid created_by_user_id FK
      varchar token_hash UK
      varchar role
      timestamptz expires_at
      int max_uses
      int use_count
      timestamptz revoked_at
      bigint version
    }
    PHOTOS {
      uuid id PK
      uuid trip_id FK
      uuid trip_stop_id FK
      uuid uploaded_by_user_id FK
      varchar storage_path UK
      varchar content_type
      bigint file_size
      timestamptz taken_at
      numeric latitude
      numeric longitude
      varchar caption
      boolean public_visible
    }
    EXPENSES {
      uuid id PK
      uuid trip_id FK
      uuid paid_by_user_id FK
      uuid created_by_user_id FK
      numeric amount
      varchar currency
      varchar category
      date expense_date
    }
    EXPENSE_PARTICIPANTS {
      uuid id PK
      uuid expense_id FK
      uuid user_id FK
      numeric share_amount
    }
    TRIP_RATINGS {
      uuid id PK
      uuid trip_id FK
      uuid user_id FK
      smallint overall_score
      smallint food
      smallint nightlife
      smallint culture
      smallint nature
      smallint walkability
      smallint value_for_money
      smallint crowds
      smallint relaxation
      varchar would_return
    }
```

Important invariants are enforced twice: friendly validation in Java and hard constraints in PostgreSQL. Examples include `end_date >= start_date`, a single membership per `(trip_id, user_id)`, one owner membership per trip, coordinate ranges, ordered stop positions, invitation roles limited to `EDITOR` / `VIEWER`, positive `max_uses`, `0 <= use_count <= max_uses`, positive expense amounts, ISO-style currency codes, unique expense participants and one rating per `(trip_id, user_id)`.

Invitation rows store a unique SHA-256 hash rather than the raw URL token. A pessimistic row lock during acceptance keeps concurrent requests from crossing the usage limit. Photo rows store metadata only; image bytes live in the private `trip-photos` Storage bucket and are served through short-lived signed URLs.

Application records are accessed through the Spring API. RLS is enabled on all application tables and direct `anon` / `authenticated` table privileges are revoked as defense in depth against accidental Data API exposure. Storage uses narrowly scoped policies and `SECURITY DEFINER` helper functions for member and explicitly public photo access.

The schema is append-only from Flyway's perspective: new versions add or evolve structures without rewriting migrations that may already have run in another environment.
