# Travel Memory Map

Travel Memory Map is a collaborative travel journal that turns a trip into an interactive story: route, timeline, memories, expenses and a replay on the map.

This repository is being built as a production-shaped portfolio project. It separates frontend experience from authoritative backend permissions and keeps private collaboration distinct from public sharing.

## Current product slice

The first two vertical slices are implemented:

- responsive dashboard with realistic travel summaries;
- trip search and filters;
- create-trip flow with React Hook Form and Zod;
- editable authenticated travel profile with backend-derived trip statistics;
- owner trip editing, archiving and permanent deletion;
- owner/editor stop creation, editing and deletion;
- trip detail with MapLibre route and markers;
- timeline generated from trip stops;
- interactive Trip Replay controls (play, pause, previous, next, 1×/2×/4×);
- TanStack Query API boundary and optional demo mode;
- member list, owner-controlled EDITOR / VIEWER role management and leave flow;
- secure invitation creation, anonymous preview, authenticated acceptance and revocation;
- invite expiry, maximum-use enforcement, duplicate-member protection and row locking;
- Spring Boot API for full trip and trip-stop lifecycle, members and invitations;
- Supabase JWT verification through Spring Security Resource Server;
- centralized OWNER / EDITOR / VIEWER permission checks;
- PostgreSQL schema managed by Flyway;
- invitation, permission, profile, statistics and HTTP security tests plus a Docker-aware Testcontainers integration test.

The UI starts in demo mode so it is immediately explorable before Supabase credentials and the API are configured. Demo mode supports the same trip and stop lifecycle as the current backend slice.

## What makes it different

### Trip Replay

Stops are played in chronological order while the map camera moves through the journey. The route, timeline and current memory stay synchronized.

### Private collaborative trips

Private does not mean single-user. A private trip is visible only to its owner and explicit members. Owners can issue secure, expiring and revocable invite links without turning the trip public. Only the SHA-256 token hash is persisted; the raw link is shown once.

### Travel intelligence

Later phases derive trip statistics, deterministic Trip DNA, travel personality, comparisons and a world scratch map from the same domain data.

## Architecture

```text
React + TypeScript + Vite
          │
          │ HTTPS / REST + Supabase JWT
          ▼
Spring Boot API
          │
          ├── authorization and business rules
          ├── PostgreSQL / Flyway
          └── Supabase Storage (Phase 3)
```

The frontend never sends a `userId` as proof of identity. The backend obtains the current user from the validated JWT `sub` claim and applies trip permissions before accessing protected data.

More detail is available in [architecture.md](docs/architecture/architecture.md), [database-schema.md](docs/database/database-schema.md), [authentication-flow.md](docs/architecture/authentication-flow.md), [trip-invitation-flow.md](docs/architecture/trip-invitation-flow.md), and the [Supabase setup guide](docs/supabase-setup.md).

## Technology stack

Frontend: React 19, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, MapLibre GL JS and CSS Modules-style feature organization.

Backend: Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation, Flyway, MapStruct, JUnit, Mockito and Testcontainers.

Data and identity: PostgreSQL, Supabase Auth and (in Phase 3) Supabase Storage.

## Run locally

Prerequisites: Node 22+ and Java 21. Docker is needed only when using the local PostgreSQL container.

For the quickest UI preview, keep `VITE_DEMO_MODE=true`, then run `npm install` and `npm run dev` in `frontend`.

For the complete authenticated stack, follow [docs/supabase-setup.md](docs/supabase-setup.md). It covers Supabase Auth redirects, the publishable frontend key, JWT verification, database SSL configuration and the local startup commands. The Windows backend helper `scripts/run-backend.ps1` safely imports the ignored root `.env` file before starting Spring Boot.

## Tests

```text
frontend: npm run lint && npm test && npm run build
backend:  mvnw.cmd test
```

The PostgreSQL repository test runs with Testcontainers when Docker is available and is skipped cleanly otherwise.

## Deployment

The frontend workflow builds Vite and publishes the output to GitHub Pages. The backend is packaged as a standalone Spring Boot service and expects a PostgreSQL/Supabase database plus the Supabase JWKS URL.

## Roadmap

- Phase 2: complete â€” members, secure invitations, expiry/revocation/use limits and role management.
- Phase 3: photos, EXIF suggestions, Supabase Storage and shared expense settlement.
- Phase 4: complete Trip Replay with synchronized photos and route animation.
- Phase 5–6: Travel DNA, statistics, world map, public trips, search and On This Day.
