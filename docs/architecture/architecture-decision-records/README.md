# Architecture decision records

Each record captures one decision that shapes the system, the context that forced it, and the
consequences accepted with it. Records are immutable: a later record supersedes an earlier one
rather than editing it.

| ADR | Title | Status |
| --- | --- | --- |
| [0001](0001-modular-monolith.md) | Modular monolith instead of microservices | Accepted |
| [0002](0002-stateless-jwt-with-refresh-tokens.md) | Stateless JWT access tokens with stored refresh tokens | Accepted |
| [0003](0003-single-session-sid-claim.md) | Enforce single-session sign-in with a `sid` claim | Accepted |
| [0004](0004-separate-chat-database.md) | Keep chat in a separate PostgreSQL database | Accepted |
| [0005](0005-quartz-jdbc-job-store.md) | Quartz with a JDBC job store for scheduled work | Accepted |
| [0006](0006-ehcache-in-process-caching.md) | In-process Ehcache instead of a distributed cache | Accepted |
| [0007](0007-gemini-model-fallback.md) | Fallback across Gemini models instead of a fixed model | Accepted |
| [0008](0008-agentic-wish-composer.md) | Agentic wish composer behind a flag with a validator tool | Accepted |
| [0009](0009-supabase-s3-storage.md) | Supabase Storage over the S3 API for all media | Accepted |
| [0010](0010-gzip-media-at-rest.md) | Gzip media at rest | Accepted |
| [0011](0011-hibernate-ddl-auto-update.md) | Hibernate `ddl-auto=update` instead of migrations | Accepted (with known risk) |
| [0012](0012-byte-sniffed-media-validation.md) | Validate uploads by content, not by name | Accepted |
| [0013](0013-gmail-api-email-transport.md) | Gmail API as the primary email transport | Accepted |
| [0014](0014-on-demand-festival-years.md) | Fetch other years' festivals on demand | Accepted |

## Template

```markdown
# NNNN — Title

- **Status:** Proposed | Accepted | Superseded by NNNN | Deprecated
- **Date:** YYYY-MM-DD
- **Deciders:** role or team
- **Related:** requirements, code, other ADRs

## Context
What forces are at play (scale, cost, providers, team size, deadlines).

## Decision
What was decided, stated in the active voice.

## Consequences
Positive, negative, and follow-up work.

## Alternatives considered
What else was evaluated and why it lost.
```
