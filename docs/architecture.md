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

The audit table is append-only, enforced by the database: the application
connects as a user that may `INSERT` and `SELECT` on it but not `UPDATE` or
`DELETE`. This needs two database users, an owner that runs migrations and a
restricted one used at runtime.

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

Implemented: backend skeleton, database container, Liquibase with the PostGIS
migration.

Not yet implemented: every module above, the two database users, the
frontend, CI.
