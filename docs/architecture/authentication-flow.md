# Authentication flow

```mermaid
sequenceDiagram
    actor User
    participant UI as React app
    participant Auth as Supabase Auth
    participant API as Spring Boot API
    participant JWKS as Supabase JWKS

    User->>UI: Sign in
    UI->>Auth: Credentials / provider flow
    Auth-->>UI: Session + access token
    UI->>API: Request with Bearer token
    API->>JWKS: Resolve cached public signing key
    JWKS-->>API: Public key
    API->>API: Verify signature, issuer, expiry
    API->>API: Read user UUID from JWT sub
    API-->>UI: Authorized response
```

The API never accepts a frontend-provided `userId` as identity. `AuthenticatedUserProvider` reads the subject from the verified token, while `UserProfileService` synchronizes non-authoritative display fields such as email and name.

Supabase asymmetric signing keys and the project JWKS endpoint are preferred. Secrets and service-role keys are never exposed through `VITE_` environment variables.
