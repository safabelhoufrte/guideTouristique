# Authentication integration: review branch

This branch preserves the complete working local integration, including Keycloak, Angular login
and private workspaces, JWT protection in Circuit and the local Guide/Reservation/Avis implementations.
Those implementations are integration candidates for their owners to review, not replacements already
approved for main2. No real local passwords, generated accounts file, tokens, database volumes or build
outputs are included. Keycloak realm credentials use environment placeholders.

Do not merge this branch wholesale into main2 while teammates are finishing their code.

## Agree before integration

1. Keycloak realm `guide-touristique`, public web client `guide-touristique-web`, authorization-code
   flow with PKCE S256. API tokens need issuer, audience `guide-touristique-api`, subject and roles.
2. Roles `USER`, `GUIDE`, `ADMIN`; public signup grants USER only. Guide profiles link to the Keycloak
   subject. Never infer authorization from a frontend route or an arbitrary client-supplied subject.
3. Circuit: reads public; create/update/archive require ADMIN. Circuit authentication is one unit:
   dependencies, SecurityConfig, application properties, seed script and integration tests.
4. Guide/Safa: profile ownership and guide assignment endpoints. Reservation/Mariem: tourist ownership,
   guide visibility, status transitions and review eligibility. Avis/Hajar: ownership, one review per
   eligible completed visit, published-only public reads and ADMIN moderation.
5. Review service-specific schema migrations against each teammate's real schema and existing data.
   Do not reset their volumes. API contracts and auth need both compatibility and authorization checks.

## Local launch

Follow README and `.env.example`; run `python scripts/setup-local-auth.py` locally, then Docker Compose.
For an existing realm apply the theme with `python scripts/apply-auth-theme.py`. Keep SMTP credentials
outside Git and confirm email delivery/verification separately. Keycloak start-dev is local development only.

## Integration strategy

Start teammates' changes from updated main2; add JWT validation and protected endpoint changes in
small owner-reviewed commits. Use the frontend/auth/theme pieces from this branch as the agreed
integration layer after the APIs meet the contracts. Keep public shared photos/landing fixes from main2.
Run frontend tests/build, per-service mvn verify and real-role HTTP checks before merging enforcement.
