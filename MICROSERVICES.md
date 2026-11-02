# Microservices foundation

The local Compose stack runs:

- **API Gateway** on `http://localhost:8080`; keep the existing `/api/**` base path.
- **Service Discovery** (Eureka) on `http://localhost:8761`.
- **Auth Service** with a separate PostgreSQL database.
- **Auction monolith** with its existing domain database.

The gateway sends `/api/auth/**` to Auth Service and all other `/api/**` requests
to the auction monolith. Both applications register with Eureka. The monolith
looks up account details through Auth Service's private
`/api/internal/users/by-email/{email}` endpoint; the gateway does not expose that
route. Configure the same `JWT_SECRET` and `INTERNAL_API_TOKEN` for both
applications.

## Move existing accounts before switching traffic

Back up the existing database first. Account IDs are preserved so existing
tournament `created_by_id` values continue to identify their owners. Auth Service
creates its tables on startup; import the existing users and verification OTP
rows before enabling gateway traffic:

```sh
docker compose up -d auth-postgres service-discovery auth-service
docker compose stop auth-service

# Use PostgreSQL connection URIs (postgresql://...), not the JDBC URL.
pg_dump --data-only --no-owner --no-acl \
  --table=public.users \
  --table=public.email_verification_tokens \
  "$LEGACY_DATABASE_URL" |
  psql "$AUTH_DATABASE_URL"

docker compose up -d
```

For the default local Compose database, `AUTH_DATABASE_URL` is
`postgresql://postgres:postgres@localhost:5433/authdb`. Set
`LEGACY_DATABASE_URL` to the connection URI for the existing auction database.
If account data is already managed elsewhere, import it into the Auth Service
schema with IDs preserved before routing requests.

The monolith's Flyway migration removes the tournament-to-`users` foreign key;
the existing `created_by_id` values remain in place and new tournaments store
the Auth Service user ID. The old `users` table is intentionally not deleted
automatically, so it remains available for rollback and can be retired after
the migration has been verified and backed up.

## Configuration and deployment

Copy `.env.example` to `.env` and set database URLs, `JWT_SECRET`,
`INTERNAL_API_TOKEN`, CORS origins, and Resend credentials as appropriate. For
production, deploy the gateway, discovery server, Auth Service, Auth PostgreSQL,
and auction monolith on a private network. Expose only the gateway publicly;
do not expose the Auth Service's `/internal/**` endpoint or Eureka outside the
trusted network. Point existing clients at the gateway URL without changing
their `/api` paths.
