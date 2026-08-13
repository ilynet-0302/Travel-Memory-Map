# Supabase setup

This guide connects the React client and Spring Boot API to one Supabase project. Keep the project in demo mode until every item in the verification section passes.

## 1. Create the project and choose JWT signing

Create a Supabase project, then open **Project Settings > API Keys**. Copy the project URL and the publishable key (`sb_publishable_...`). The publishable key is intended for public clients; never put a secret key or service-role key in the frontend.

The backend validates access tokens from Supabase's JWKS endpoint. In **Authentication > Signing Keys**, use an asymmetric signing key such as ES256 or RS256. The project JWKS endpoint must return at least one public key:

```text
https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
```

## 2. Configure authentication redirects

In **Authentication > URL Configuration**, set the local site URL to:

```text
http://localhost:5173
```

Add these redirect URLs while developing:

```text
http://localhost:5173/**
http://127.0.0.1:5173/**
```

Before deployment, also add the exact production origin and Vite base path, for example:

```text
https://<github-user>.github.io/travel-memory-map/**
```

Invitation links preserve `/join/<token>` through sign-up and email confirmation. Supabase rejects a requested redirect unless it matches the allow list.

## 3. Configure the frontend

Create the ignored local file from the committed template:

```powershell
Copy-Item frontend/.env.example frontend/.env.local
```

Set these values in `frontend/.env.local`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_SUPABASE_URL=https://<project-ref>.supabase.co
VITE_SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
VITE_MAP_STYLE_URL=https://demotiles.maplibre.org/style.json
VITE_DEMO_MODE=false
VITE_BASE_PATH=/travel-memory-map/
```

Only variables prefixed with `VITE_` are exposed to browser code. Do not add database passwords or Supabase secret/service-role keys here.

## 4. Configure the backend and database

Create the ignored backend environment file:

```powershell
Copy-Item .env.example .env
```

For a Supabase Postgres connection, replace the database values in `.env` using the connection information shown by the Supabase **Connect** dialog. A persistent backend should use either the direct connection when IPv6 is available or the Supavisor session-mode connection when it is not. Require SSL in the JDBC URL.

```dotenv
DATABASE_URL=jdbc:postgresql://<database-host>:5432/postgres?sslmode=require
DATABASE_USERNAME=<database-user>
DATABASE_PASSWORD=<database-password>
SUPABASE_AUTH_ISSUER=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWKS_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

The exact host and username differ between direct and pooled connections, so copy both from the dashboard instead of constructing them manually.

## 5. Start the full stack

The helper imports `.env` into the backend process without printing its values:

```powershell
.\scripts\run-backend.ps1
```

In a second terminal:

```powershell
Set-Location frontend
npm install
npm run dev
```

Open `http://localhost:5173`, create an account, confirm the email, and sign in.

## 6. Verify the integration

Check the following before committing or deploying:

1. Sign-up returns to the same page after email confirmation.
2. A signed-in user can create a trip while an anonymous request to `/api/v1/trips` returns `401`.
3. Opening an invite anonymously shows its preview, but accepting it requires authentication.
4. After sign-up from an invite URL, confirmation returns to `/join/<token>` and acceptance succeeds.
5. An editor can manage stops but cannot manage members; a viewer cannot change either.
6. `.env` and `frontend/.env.local` remain untracked and contain no committed secrets.

## Official references

- [Supabase React quickstart](https://supabase.com/docs/guides/getting-started/quickstarts/reactjs)
- [Supabase API keys](https://supabase.com/docs/guides/getting-started/api-keys)
- [Supabase JWTs and JWKS](https://supabase.com/docs/guides/auth/jwts)
- [Supabase signing keys](https://supabase.com/docs/guides/auth/signing-keys)
- [Supabase redirect URLs](https://supabase.com/docs/guides/auth/redirect-urls)
- [Supabase Postgres connection guide](https://supabase.com/docs/guides/database/connecting-to-postgres)
