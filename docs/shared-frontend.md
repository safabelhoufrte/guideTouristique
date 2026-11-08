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
`db/init.sql` creates one shared business database, `tourisme_db`, with four service roles and
schemas (`guide`, `circuit`, `reservation`, `avis`). Each service connects with its own role,
uses its schema for JDBC/Hibernate queries and keeps its Flyway history there. Business tables
remain owned by service migrations. No business seed rows or image blobs are added here.

### Existing PostgreSQL volumes

Initialization scripts do not rerun against existing data. Do not delete an existing volume
or use `docker compose down -v`. Follow the backup, migration, verification and rollback steps
in [shared-database.md](shared-database.md) before changing an existing installation.
This configuration does not transfer existing data automatically.
The default host port is 15433; containers connect to `postgres:5432/tourisme_db`.

### Branch ownership

- `main2`: shared public frontend and paired first-time database configuration.
- `circuit-service`: Circuit backend, migrations, tests, UML and Circuit page updates.
- `auth-integration`: local authenticated integration for team review, not merged into main2.

Never merge the entire Circuit branch just to obtain shared frontend files: that would also import
backend-specific history. Integrate agreed service changes explicitly after review.
