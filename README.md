# Atlas

Atlas is an emergency and crisis management platform for coordinating
responses to floods, major accidents, infrastructure failures and similar
incidents. It works with simulated data and is not connected to real
emergency services.

The project is in early development. See [Status](#status) for what exists
today.

## Technology

| Area | Stack |
|---|---|
| Backend | Java 25, Spring Boot 4.1, Spring Data JPA |
| Frontend | Angular 22 |
| Database | PostgreSQL 17 with PostGIS, Liquibase migrations |
| Infrastructure | Docker Compose, GitHub Actions |

The design is described in [docs/architecture.md](docs/architecture.md).

## Running locally

You need JDK 25, Node.js 24 or newer, and Docker.

1. Start the database:

   ```
   docker compose up -d
   ```

   It listens on port 5433.

2. Start the backend, from `backend/`:

   ```
   ./mvnw spring-boot:run
   ```

   On Windows use `mvnw.cmd`. The API runs on http://localhost:8080.

3. Start the frontend, from `frontend/`:

   ```
   npm install
   npm start
   ```

   The app runs on http://localhost:4200 and forwards `/api` requests to the
   backend.

### Everything in containers

```
docker compose --profile full up -d --build
```

This builds and starts the backend and frontend as well. The app is then at
http://localhost:4200.

## Configuration

All settings have defaults that work locally. Outside local development, set:

| Variable | Purpose |
|---|---|
| `ATLAS_JWT_SECRET` | Signing key for access tokens, at least 32 characters |
| `ATLAS_ADMIN_EMAIL`, `ATLAS_ADMIN_PASSWORD` | The first administrator, created when none exists |
| `ATLAS_DEMO_DATA` | Set to `true` to add sample teams and vehicles around Plovdiv at startup |
| `ATLAS_DB_URL` | Database address |
| `ATLAS_DB_USER`, `ATLAS_DB_PASSWORD` | The database owner, used only to run migrations |
| `ATLAS_DB_APP_PASSWORD` | Password of `atlas_app`, the restricted user the application runs as |

If `ATLAS_ADMIN_PASSWORD` is not set, a random password is generated and
written to the log once, on the first start.

## Tests

- Backend, from `backend/`: `./mvnw verify`. The database must be running.
- Frontend, from `frontend/`: `npm test`.

Both run in CI on every push.

## Repository layout

```
backend/              Spring Boot application
frontend/             Angular application
docs/                 Architecture and decisions
docker-compose.yml    Local services
```

## Status

Done:

- Backend and frontend skeletons
- Database with PostGIS and Liquibase migrations
- Docker setup and CI pipeline
- Authentication: role-based access, JWT access tokens, rotating refresh
  tokens in an HttpOnly cookie, session management
- Incidents: reporting with a PostGIS location, a lifecycle state machine
  with role rules
- Audit log: written in the same transaction as each change, append-only at
  the database level
- Resources: teams and vehicles, a PostGIS search for what is available near
  an incident, and assignment that is safe under concurrent requests
- Operations dashboard: an Angular application to log in, browse and report
  incidents, change their status, dispatch nearby resources and read the
  audit history
- Map: incidents, resources, hospitals and shelters as markers; evacuation
  and affected zones as polygons and closed roads as lines, drawn on the map
  and stored as PostGIS geometry

- Live updates: changes are announced over WebSocket (STOMP) after the
  transaction commits, and open pages reload themselves
- Notifications: stored per user after commit, with an unread count that
  updates live

Planned, in order: event system,
priority scoring, disaster simulator, analytics, observability.
