# Shared public frontend

This main2 release contains the public Angular frontend: Moroccan landing page, circuit catalogue
and details with inspiration photos, guide directory, published reviews and the existing reservation form.
It does not enable Keycloak, JWT interception, role guards or private workspaces. Authentication is
being reviewed separately on `auth-integration`; this public development release is not a
production security boundary. Protected operations must be authorized by the owning backend.

## Run and verify

Use Node 24. In `frontend`, run `npm ci`, `npm test -- --watch=false`, `npm run build`, then `npm start`.
The dev proxy targets Guide 8081, Circuit 8082, Reservation 8083 and Avis 8084. Set `GUIDE_API_URL`,
`CIRCUIT_API_URL`, `RESERVATION_API_URL` or `AVIS_API_URL` when teammates use different ports.

Alternatively run `docker compose up --build -d frontend postgres` after preparing `.env`.
The frontend listens on http://localhost:4200 and uses Docker service DNS names for APIs.
Connect teammates' API containers to the same Compose network to serve data there. Missing APIs
show loading/error/retry states; they do not stop the frontend from starting.

main2 still contains the original service skeletons. It does not contain the Circuit implementation
from `circuit-service`, nor replace Guide/Reservation/Avis code with the local integration versions.
Running the whole Compose stack requires the corresponding service implementations to be integrated.

Public API contracts remain in `docs/api-contracts.md`. The existing reservation form is an integration
client, not a replacement for Mariem's backend. The public reviews page consumes only `/api/avis`.
Photo sources and fictional-preview behaviour are documented in `docs/landing-photos.md`.

## First-time database setup

Copy `.env.example` to `.env` and replace all password placeholders. Keep `.env` out of Git.
`db/init.sql` creates the four databases and their separate non-superuser login roles. Compose passes
the matching passwords into PostgreSQL. Business tables are owned by each service's migrations;
no business seed rows or image blobs are added to PostgreSQL initialization.

### Existing PostgreSQL volumes

Initialization scripts do not rerun against existing data. Do not delete or recreate an existing
volume and do not run `docker compose down -v` to adopt these changes. Existing installations need
an administrator to create missing service roles, set the chosen passwords, assign database ownership
and update each service's connection settings after taking a backup. Plan that migration with each
database owner; the fresh-init script is not an upgrade script. Keep the previous Compose/env setup
for that database until its migration is complete. The default host port is now 15433 to avoid clashes;
the internal database endpoint remains `postgres:5432`.

### Branch ownership

- `main2`: shared public frontend and paired first-time database configuration.
- `circuit-service`: Circuit backend, migrations, tests, UML and Circuit page updates.
- `auth-integration`: local authenticated integration for team review, not merged into main2.

Never merge the entire Circuit branch just to obtain shared frontend files: that would also import
backend-specific history. Integrate agreed service changes explicitly after review.
