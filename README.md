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

Planned, in order: authentication and roles, incident lifecycle, resource
management, operations dashboard, map, real-time updates, event system,
priority scoring, disaster simulator, analytics, observability.
