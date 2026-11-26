# 0006 — In-process Ehcache instead of a distributed cache

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../backend/caching-strategy.md](../../backend/caching-strategy.md)

## Context

Several reads are repeated by every user: seed lookups by code on each save, festivals of a month,
dashboard counters, IP geolocation and email images. Provider quotas (ip-api.com: 45 requests per
minute) make repeated lookups expensive.

## Decision

* Use Ehcache through JCache (`spring.cache.type=jcache`, `ehcache.xml`).
* Give every cache an entry cap and a TTL; Hibernate second-level and query caching are enabled for
  reference data.
* Evict `festivalsByMonth` on festival sync and special-event writes.
* Require cached values to be `Serializable`.

## Consequences

**Positive**

* No extra infrastructure, no network hop, no credentials.
* Provider quota usage drops sharply (IP lookups cached for 24 hours).
* Bounded memory: `CacheConfigurationTest` enforces a cap and TTL on every declared cache.

**Negative**

* Caches are per instance and not shared; a multi-instance deployment warms each copy.
* Cache correctness depends on explicit eviction; a missed eviction serves stale data until the TTL.
* Heap pressure from large values (email images) — capped at 30 entries.

**Follow-up**

* Move to Redis if the service runs several instances or needs shared invalidation.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Redis / Memcached | Extra managed service and cost for a single-instance deployment |
| No cache | ip-api.com quota would be exhausted quickly |
| Database materialised views | Stale data and schema coupling |
