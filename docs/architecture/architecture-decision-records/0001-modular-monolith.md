# 0001 — Modular monolith instead of microservices

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../system-architecture.md](../system-architecture.md), [../../backend/backend-design.md](../../backend/backend-design.md)

## Context

The product serves one family group: tens of users, a handful of scheduled jobs per day, and a
single administrator. The team is small. The features (identity, dates, wishes, chat, circles,
dashboards) share the same entities heavily: a calendar read touches festivals, events, users and
circles; a wish send touches users, events, AI and email.

Operating several services would multiply deployment, monitoring and secret-management work without
solving a real scaling problem.

## Decision

Build one Spring Boot application (one jar, one container) organised internally by layer and feature
package. Keep module boundaries enforced by convention (controllers → services → repositories) and
by documentation rather than by process boundaries.

## Consequences

**Positive**

* One deployment, one log stream, one configuration surface.
* Cross-feature reads are in-process method calls, not network hops.
* Transactions span all business tables naturally.
* Tests are plain JUnit/Mockito unit tests with no containers to orchestrate.

**Negative**

* A change in any module requires redeploying everything.
* A crash takes down chat, wishes and dashboards together.
* Scaling is coarse-grained (whole application, not one hot module).
* Module boundaries can erode silently without review discipline.

**Follow-up**

* Keep the layering rules in [../system-architecture.md](../system-architecture.md) under review.
* Revisit if a module needs independent scaling (for example AI generation) or a different runtime.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Microservices per domain | Operational overhead and distributed transactions outweigh the benefit at this scale |
| Serverless functions | Cold starts, no durable scheduler, awkward JPA lifecycle |
| Two services (API + worker) | The worker would share nearly all domain code; the split buys little |
