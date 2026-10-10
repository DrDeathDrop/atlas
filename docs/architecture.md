# Atlas architecture

Atlas is a **modular monolith**: one Spring Boot application and one PostgreSQL
database, split internally into modules with clear boundaries.

Microservices were ruled out. At this size they add operational cost and no
benefit. A module can be extracted later if there is a real reason to.

## Modules

Code is organised by domain, not by technical layer.

```
io.github.drdeathdrop.atlas
├── incident/        incidents, categories, lifecycle, priority
├── resource/        teams, personnel, vehicles, equipment, allocation
├── user/            accounts, roles, authentication
├── audit/           append-only audit log
├── notification/    in-app, email and WebSocket notifications
├── analytics/       statistics and reports
└── shared/          small: base types, error model, security helpers
```

A module is a group of things that change together and share rules. Inside a
module, each entity gets its own sub-package:

```
resource/
├── vehicle/         Vehicle, VehicleService, VehicleDTO, VehicleRepository
├── team/
├── equipment/
└── allocation/      "find and assign what is available"
```

Inside a module the flow is controller -> service -> repository.

## Module boundaries

1. **The top package of a module is its public part.** It holds the interface
   other modules call, the records they receive, and the events the module
   publishes. Everything in sub-packages is internal.
2. **Entities and repositories never leave their module.** Other modules get
   read-only records (for example `VehicleSummary`), never the JPA entity.
3. **Modules refer to each other by ID.** An assignment stores `incidentId`,
   not a `@ManyToOne Incident`.
4. **No dependency cycles.** `incident` may call `resource`; `resource` does
   not call back into `incident`. Events are used where the reverse direction
   is needed.

These rules are kept by convention and code review. Spring Modulith was
considered for automatic verification and not adopted.

## How modules communicate

| Situation | Mechanism | Example |
|---|---|---|
| The caller needs an answer, or the action must succeed | Direct call to the other module's public interface | `incident` asks `resource` for available ambulances, then deploys one |
| Something happened and others may react | Event | `IncidentStatusChanged` |

Events are published with Spring's `ApplicationEventPublisher` and stay
in-process for now. RabbitMQ is introduced in Phase 8.

When a listener runs depends on what it does:

- **Audit** runs inside the same transaction as the change
  (`@EventListener`). The change and its audit record are saved together or
  not at all.
- **Notifications, dashboard pushes and analytics** run after the transaction
  commits (`@TransactionalEventListener`), so nobody is notified about a
  change that was rolled back.

## Database

- **PostgreSQL 17 with PostGIS**, run locally through `docker-compose.yml`.
  PostGIS provides point and polygon column types, distance and containment
  queries, and spatial indexes. It backs resource allocation ("available
  ambulances within 15 km"), map zones and the proximity part of the priority
  score.
- **Liquibase owns the schema.** Hibernate runs with `ddl-auto: validate` and
  never creates or alters tables.
- Connection settings come from `ATLAS_DB_URL`, `ATLAS_DB_USER` and
  `ATLAS_DB_PASSWORD`, with local defaults.

### Identifiers

Primary keys are UUIDs. They are used in URLs, events and references between
modules. Entities that people talk about also get a readable reference for
display, such as `INC-2026-0184`.

### Audit log

The audit table is append-only, enforced by the database. There are two
database users: `atlas` owns the schema and is used only by Liquibase, and
`atlas_app` is what the application runs as. `atlas_app` may `INSERT` and
`SELECT` on `audit_log` but not `UPDATE`, `DELETE` or `TRUNCATE`; an
integration test confirms PostgreSQL refuses both.

Audit records are written by a listener in the `audit` module that reacts to
incident events. The incident module does not know the audit module exists.
Each record stores who acted, the role they held at the time, the action, the
entity, and the previous and new state.

## Incident lifecycle

An incident moves through seven states. Only the moves in this table are
allowed; everything else is refused.

| From | Allowed next states |
|---|---|
| `REPORTED` | `VERIFIED`, `REJECTED` |
| `VERIFIED` | `ACTIVE` |
| `ACTIVE` | `CONTAINED` |
| `CONTAINED` | `RESOLVED`, `ACTIVE` |
| `RESOLVED` | `ARCHIVED`, `ACTIVE` |
| `REJECTED` | `ARCHIVED` |
| `ARCHIVED` | none |

`REJECTED` exists for false reports, so they are not counted as resolved.
The two backwards moves cover an incident that flares up again or was closed
too early. `ARCHIVED` is final.

Roles limit who may make a move. Administrators and dispatchers may make any
allowed move. A field operator may only mark an active incident as contained.
Analysts and viewers may not change status.

The rules live in one class, `IncidentLifecycle`, which has no dependency on
Spring or the database. A move is validated before anything is modified, so a
refused move changes nothing and produces no event.

## Authentication

Accounts are created by administrators; there is no public sign-up. Each user
has exactly one role: `ADMIN`, `DISPATCHER`, `FIELD_OPERATOR`, `ANALYST` or
`VIEWER`. The first administrator is created at startup when none exists.

Logging in with email and password returns two tokens:

- An **access token**: a JWT signed with HS256, valid for 15 minutes, carrying
  the user's id and role. It is sent in the `Authorization` header and verified
  on every request by Spring Security's resource server support. The server
  keeps no session.
- A **refresh token**: a random value, valid for 7 days, delivered in an
  `HttpOnly`, `SameSite=Strict` cookie scoped to `/api/auth`. Only its SHA-256
  hash is stored. Each row is one logged-in device, which is what session
  listing and revocation are built on.

Refresh tokens rotate: using one replaces it. Presenting a token that was
already used is treated as theft and ends every session of that user.

The current user is always taken from the verified token, never from a URL or
request body. Role rules are declared on methods with `@PreAuthorize`.

Passwords are hashed with BCrypt. The signing key comes from
`ATLAS_JWT_SECRET`; when it is unset a random key is generated at startup,
which is acceptable only for local development.

Known limitation: disabling a user does not invalidate an access token that
was already issued. It stops working when it expires, at most 15 minutes
later, and cannot be refreshed.

## Technology

| Area | Choice |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1 |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL 17 + PostGIS |
| Migrations | Liquibase |
| Frontend | Angular, Angular Material, Leaflet |
| Local services | Docker Compose |

## Current state

Implemented: the `user` module (accounts, roles, login, refresh tokens,
sessions), the `incident` module (reporting, lifecycle, status changes), the
`audit` module (append-only log of incident events), Liquibase migrations,
Docker setup, CI.

Not yet implemented: `resource`, `notification` and `analytics`, and the
frontend beyond its skeleton.
