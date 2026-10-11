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
| Messaging | RabbitMQ, WebSocket (STOMP) |
| Observability | Actuator, Micrometer, Prometheus, Grafana |
| Infrastructure | Docker Compose, Testcontainers, GitHub Actions |

The design is described in [docs/architecture.md](docs/architecture.md).

## Running locally

You need JDK 25, Node.js 24 or newer, and Docker.

1. Start the database and the message broker:

   ```
   docker compose up -d
   ```

   PostgreSQL listens on port 5433 and RabbitMQ on 5672. The RabbitMQ
   management page is at http://localhost:15672 (user `atlas`, password
   `atlas`).

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

### Metrics and dashboards

```
docker compose --profile observability up -d
```

This adds Prometheus (http://localhost:9090) and Grafana
(http://localhost:3000). Grafana opens on the Atlas dashboard without a login;
to edit, sign in as `atlas` / `atlas`.

The backend serves health checks and metrics on its own port, 8081, apart from
the API on 8080, so a deployment can keep them internal:

- http://localhost:8081/actuator/health
- http://localhost:8081/actuator/health/liveness
- http://localhost:8081/actuator/health/readiness
- http://localhost:8081/actuator/prometheus

Every response carries an `X-Request-Id` header, and the same id is on every
log line written while that request was handled. With the `prod` profile the
log is written as JSON.

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
| `ATLAS_RABBIT_HOST`, `ATLAS_RABBIT_PORT` | RabbitMQ address |
| `ATLAS_RABBIT_USER`, `ATLAS_RABBIT_PASSWORD` | RabbitMQ login |
| `ATLAS_MANAGEMENT_PORT` | Port for health checks and metrics, 8081 by default |

If `ATLAS_ADMIN_PASSWORD` is not set, a random password is generated and
written to the log once, on the first start.

## Tests

- Backend, from `backend/`: `./mvnw verify`. Docker must be running: the
  integration tests start their own PostgreSQL and RabbitMQ with Testcontainers
  and remove them afterwards, so they never touch the development database.
- Frontend, from `frontend/`: `npm test`.

Both run in CI on every push.

## Repository layout

```
backend/              Spring Boot application
frontend/             Angular application
docs/                 Architecture and decisions
observability/        Prometheus and Grafana configuration
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
- Notifications: stored per user, with an unread count that updates live
- Event system: a transactional outbox publishes events to RabbitMQ;
  consumers are idempotent and failed messages go to a dead-letter queue
- Priority scoring: an explainable 0 to 100 score per open incident, used to
  order the incident list
- Disaster simulator: reproducible scenarios that feed incidents through the
  normal pipeline at a chosen pace
- Analytics: totals, breakdowns, response times, resource load and incident
  hotspots, computed with SQL aggregates and PostGIS
- Observability: liveness and readiness probes, Prometheus metrics including
  outbox lag, a request id on every log line, and a Grafana dashboard

Planned: login rate limiting and caching.
