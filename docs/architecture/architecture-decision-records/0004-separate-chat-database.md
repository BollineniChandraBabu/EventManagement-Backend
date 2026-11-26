# 0004 — Keep chat in a separate PostgreSQL database

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../backend/database-design.md](../../backend/database-design.md)

## Context

Chat writes far more rows than the rest of the product and has different access patterns (recent
messages per conversation, unread counters, `DISTINCT ON` for the last message). The business data
lives on Neon; a second PostgreSQL instance was already available on Supabase.

## Decision

* Store conversations, messages and reactions in a second PostgreSQL database, configured through
  `app.supabase.db.*` and exposed as `NamedParameterJdbcTemplate`.
* Create and evolve the schema in `ChatSchemaInitializer` with idempotent DDL
  (`CREATE TABLE IF NOT EXISTS`, `ALTER TABLE … ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT
  EXISTS`).
* Access it with hand-written SQL in `ChatMessageRepository`, not JPA.
* Accept that chat is outside the business transaction boundary.

## Consequences

**Positive**

* Chat load and failures are isolated from wishes, users and festivals.
* SQL gives exact control of upserts, `DISTINCT ON`, counters and bulk deletes.
* The chat schema can change without touching the JPA model.

**Negative**

* No joins between chat and users: participant names and pictures are resolved with a second query.
* No distributed transaction: a message may be stored while a related business change rolls back (and
  the reverse), which is acceptable because they are independent.
* Two connection pools, two sets of credentials, two backup regimes.
* Schema changes are manual and imperative.

**Follow-up**

* Consider Flyway for the chat schema if it grows.
* Keep `ChatSchemaInitializer` idempotent; never put destructive DDL there.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Same database, JPA entities | Couples chat volume to business backups and migrations |
| Document store (MongoDB) | Another provider and model to operate |
| Third-party chat service | Loses control of message data and the circle gate |
