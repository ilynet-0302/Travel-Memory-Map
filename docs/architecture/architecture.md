# Architecture

## System boundary

```mermaid
flowchart LR
    Browser[React + TypeScript] -->|REST + bearer token| API[Spring Boot API]
    Browser -->|sign in| Auth[Supabase Auth]
    API -->|verify JWKS| Auth
    API --> DB[(PostgreSQL)]
    Browser -. Phase 3 upload .-> Storage[Supabase Storage]
    API -. signed access / metadata .-> Storage
```

The Spring Boot API is the authoritative layer for permissions, memberships, invitations, expenses, settlements and derived travel intelligence. Hiding a frontend control is never treated as authorization.

## Backend organization

The backend is organized by feature (`trip`, `membership`, `invitation`, `user`, and later `expense` and `photo`), with thin controllers and explicit services. Cross-feature policy is kept in narrowly named services such as `TripPermissionService`.

```mermaid
flowchart TD
    Request --> Controller
    Controller --> Service
    Service --> Permission[TripPermissionService]
    Service --> Repository
    Repository --> PostgreSQL
```

Entities are not exposed directly through REST. Request and response DTOs define the contract, and a global exception handler produces stable error codes.

## Frontend organization

The frontend groups code around product features. React Router owns navigation, TanStack Query owns server state, the API client attaches the current Supabase access token, and forms validate locally with Zod while the backend repeats authoritative validation.

Demo mode is an adapter behind the same trip and collaboration API boundaries; components do not contain arbitrary fetch calls.

## Phase boundaries

The current slice covers the trip lifecycle (create, edit, archive and delete), stop lifecycle (create, edit and delete), timeline and replay, plus private collaboration. Owners control trip settings and lifecycle; owners and editors manage itinerary content. Owners also manage roles and issue limited invitations, while new members preview and accept an invitation without exposing the private trip. The next slice connects a real Supabase environment and strengthens HTTP-level authentication and authorization tests before photo storage, EXIF suggestions and shared expenses are added.
