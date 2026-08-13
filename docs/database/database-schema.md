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
    }
```

Important invariants are enforced twice: friendly validation in Java and hard constraints in PostgreSQL. Examples include `end_date >= start_date`, a single membership per `(trip_id, user_id)`, one owner membership per trip, coordinate ranges, ordered stop positions, invitation roles limited to `EDITOR` / `VIEWER`, positive `max_uses`, and `0 <= use_count <= max_uses`.

Invitation rows store a unique SHA-256 hash rather than the raw URL token. A pessimistic row lock during acceptance keeps concurrent requests from crossing the usage limit. Photo rows store metadata only; image bytes live in the private `trip-photos` Storage bucket and are served through short-lived signed URLs. Future tables (`expenses`, `expense_participants`, `trip_ratings`) are added by new Flyway migrations rather than modifying applied migrations.
