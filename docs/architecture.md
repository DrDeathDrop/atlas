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
├── facility/        hospitals and shelters
├── zone/            evacuation and affected zones, road closures
├── user/            accounts, roles, authentication
├── audit/           append-only audit log
├── notification/    live updates over WebSocket; in-app and email notifications
├── analytics/       statistics and reports
└── shared/          small: base types, error model, security helpers
```

A module is a group of things that change together and share rules. Inside a
module, code is split into sub-packages by responsibility:

```
resource/
├── inventory/       the resources themselves: teams and vehicles
└── allocation/      finding what is available and assigning it
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

Audit records are written by listeners in the `audit` module that react to
incident and resource events. Neither module knows the audit module exists.
Each record stores who acted, the role they held at the time, the action, the
entity, the previous and new state, and for resource actions the incident
they relate to.

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

## Resources and dispatch

Teams and vehicles are stored in one `resources` table, distinguished by
type. They share every field and rule: a call sign, a status, a location, and
they are assigned and released the same way.

**Finding resources.** "Available resources within 15 km of this incident" is
a PostGIS query. `ST_DWithin` filters by radius using a spatial index, and
`ST_Distance` orders the results nearest first. Both work on the `geography`
type, so distances are real metres.

**Assigning.** A resource can be assigned only when it is `AVAILABLE`, and
never to two incidents at once. Two dispatchers acting at the same moment
would otherwise both pass the availability check. Two things prevent that:

- The assignment reads the resource with a row lock (`SELECT ... FOR UPDATE`).
  A second request for the same resource waits until the first commits, then
  sees that the resource is no longer available.
- A partial unique index allows at most one unreleased assignment per
  resource, so the database refuses a double assignment even if the code were
  wrong.

An integration test runs two assignments of one resource from two threads and
requires exactly one to succeed.

**Rules that span modules.** Only `VERIFIED` and `ACTIVE` incidents can
receive resources. The first assignment moves a `VERIFIED` incident to
`ACTIVE`. Resolving an incident releases its resources. These live in the
incident module, which calls the resource module through the
`ResourceAllocation` interface. The resource module knows nothing about
incidents beyond an id.

## Map data

The map shows three kinds of geometry, each stored in its natural PostGIS
type:

| What | Type | Module |
|---|---|---|
| Incidents, resources, hospitals, shelters | `Point` | `incident`, `resource`, `facility` |
| Evacuation and affected zones | `Polygon` | `zone` |
| Closed roads | `LineString` | `zone` |

The API never exposes database geometry. A zone travels as a list of
latitude/longitude corners; `GeoShapes` turns that into a polygon, closes the
ring, and rejects shapes that cannot be stored meaningfully: fewer than three
different corners, or edges that cross each other. An invalid shape is a 400
with a readable reason, not a database error.

Zones and road closures are never deleted. Lifting a zone or reopening a road
records who did it and when, and the row drops out of the "active" list. What
was closed, and for how long, stays available for later analysis.

A facility has a capacity and an occupancy. The service refuses an occupancy
above the capacity, and a check constraint in the database enforces the same
rule.

## Live updates

Open pages update themselves when something changes elsewhere. The browser
keeps one WebSocket connection, speaking STOMP, and subscribes to a single
topic.

**What is sent.** Only a notice that something changed: a kind (`INCIDENT`,
`RESOURCE`, `FACILITY`, `ZONE`) and an id. The page then reloads what it shows
through the normal REST API. The data is never pushed over the socket, so
there is one place that decides what a user may see, and a missed message
costs nothing more than a stale screen until the next one.

**When it is sent.** Services publish events as before. The broadcaster
listens with `@TransactionalEventListener(AFTER_COMMIT)`: a notice goes out
only once the change is really in the database. Sent earlier, a client could
reload and still read the old state, or be told about a change that was then
rolled back. A failed broadcast is logged and never fails the request. This
is the opposite choice from the audit log, which runs inside the transaction
because an unaudited change must not exist.

**Authentication.** A browser cannot add an `Authorization` header to a
WebSocket handshake, so the handshake itself is open and the access token is
sent in the STOMP `CONNECT` frame. An interceptor validates it with the same
decoder as the REST API and refuses the connection otherwise. Clients may
subscribe to the one topic and may not send anything.

**Reconnecting.** The client reconnects on its own. After a reconnect it
tells every page to reload, because it cannot know what it missed. If the
server refused the connection, the client refreshes the access token before
the next attempt. Bursts of notices are collapsed into one reload.

The broker is Spring's in-memory one, which is enough for a single
application instance. Running several instances would need a shared broker.

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

## Frontend

The Angular application is organised the same way as the backend, by feature:

```
frontend/src/app
├── core/auth/        login state, token refresh, route guards
├── core/live/        the WebSocket connection and the stream of updates
├── layout/           the shell around every page
├── features/
│   ├── auth/         login page
│   ├── incidents/    list, detail, dispatch panel, report form
│   ├── map/          the map page and its drawing panel
│   └── resources/    list, add form
└── shared/           small reusable pieces, including the map component
```

The access token is kept in memory only. On page load the application calls
the refresh endpoint, so a reload does not log the user out. An HTTP
interceptor adds the token to API requests and, on a 401, refreshes it once
and repeats the request; concurrent failures share a single refresh.

The frontend never decides what a user is allowed to do. It asks the server
which status changes are available for an incident and shows those. Hiding a
button is a convenience; every rule is enforced again by the API.

State is held in signals inside components and services. There is no state
management library.

Leaflet is wrapped in a single component, `MapView`. Pages pass it plain
data, markers, shapes and an optional shape being drawn, and receive plain
events back. No page touches Leaflet directly, so page tests replace the map
with a stub and check only what was sent to it. Drawing a zone is the same
idea: the page collects clicked points in a signal and hands them back to the
map to display.

## Technology

| Area | Choice |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1 |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL 17 + PostGIS |
| Migrations | Liquibase |
| Live updates | WebSocket with STOMP, Spring's in-memory broker |
| Frontend | Angular, Angular Material, Leaflet, stompjs |
| Local services | Docker Compose |

## Current state

Implemented: the `user` module (accounts, roles, login, refresh tokens,
sessions), the `incident` module (reporting, lifecycle, status changes,
dispatch), the `resource` module (teams and vehicles, nearby search,
assignment), the `facility` module (hospitals and shelters), the `zone`
module (evacuation and affected zones, road closures), the `audit` module
(append-only log of incident and resource events), live updates in the
`notification` module, Liquibase migrations, Docker setup, CI.

The frontend covers login, the incident list and detail pages, dispatching,
the resource list, forms to report an incident and add a resource, and a map
of incidents, resources, facilities, zones and closed roads, on which zones,
closures and facilities can be drawn. Lists, the incident page and the map
reload themselves when something changes.

Not yet implemented: stored notifications for users, and `analytics`.
