# 0011 — Hibernate `ddl-auto=update` instead of migrations

- **Status:** Accepted (with known risk)
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../deployment/database-migration.md](../../deployment/database-migration.md), [../../operations/known-issues.md](../../operations/known-issues.md)

## Context

The project moved quickly with a single developer. Every entity change had to reach two databases
(local and production) without a migration pipeline, and most changes were additive (new columns,
new tables).

## Decision

* Set `spring.jpa.hibernate.ddl-auto=update` in both `application.properties` and
  `application-prod.properties`.
* Create the chat schema imperatively in `ChatSchemaInitializer` with idempotent DDL.
* Let Quartz create its own tables (`initialize-schema=always`).
* Back up before every release and review the schema diff manually.

## Consequences

**Positive**

* New columns and tables appear automatically; no migration files to maintain.
* Local development matches production without extra steps.

**Negative**

* No migration history and no down-migrations.
* `update` never drops or narrows columns, so removals require manual SQL.
* A rename becomes add + orphaned column.
* A bad entity change can lock or alter a production table during startup.
* Rolling back code may leave the schema ahead of it.

**Follow-up**

* Introduce Flyway: baseline the current schema, then add versioned migrations (see
  [../../deployment/database-migration.md](../../deployment/database-migration.md)).
* Until then, never ship a destructive column change in the same release as a code change.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Flyway from day one | Slower early iteration; the schema changed daily |
| Liquibase | Same benefit as Flyway, more XML/YAML to maintain |
| `ddl-auto=validate` + manual SQL | Requires a migration process that did not exist |
