# Caching strategy

## 1. Mechanism

* Provider: **Ehcache 3** through JCache (`spring.cache.type=jcache`,
  `spring.cache.jcache.config=classpath:ehcache.xml`).
* Used for two purposes:
  1. Hibernate **second-level cache** (`hibernate.cache.use_second_level_cache=true`) and **query
     cache** (`use_query_cache=true`) via `JCacheRegionFactory`.
  2. Spring `@Cacheable` / `@CacheEvict` on selected service methods.
* Every cache is declared with an entry cap and, except for the update-timestamps region, a TTL.
  `CacheConfigurationTest` fails if a `CacheNames` constant has no declaration, or if a cache lacks a
  cap or TTL.

## 2. Declared caches (`ehcache.xml`)

| Cache | Holds | TTL | Max entries | Cleared when |
| --- | --- | --- | --- | --- |
| `defaultCache` | Anything not otherwise declared (bounded template) | 30 min | 1 000 | TTL |
| `seedData` | Hibernate region for `ThemeSeed` and other reference entities | 1 h | 500 | Hibernate evicts on row change |
| `default-query-results-region` | Results of cacheable queries (seed lookups by code) | 10 min | 1 000 | TTL and Hibernate invalidation |
| `default-update-timestamps-region` | When each table last changed | none | 5 000 | Never (must not expire) |
| `emailImages` | An email's image bytes + content type, keyed by log id | 15 min | 30 | TTL, or the email row is deleted |
| `ipInfo` | `ViolatedUserIpInfo` per IP | 24 h | 2 000 | TTL |
| `festivalsByMonth` | Festivals of a month (`all` when month is null) | 6 h | 24 | Festival sync or special-event write |
| `dashboardSummary` | The four dashboard counters | 60 s | 200 | TTL |

## 3. Usage points

| Code | Annotation | Key | Why |
| --- | --- | --- | --- |
| `FestivalServiceImpl.listByMonth(Integer)` | `@Cacheable(FESTIVALS_BY_MONTH)` | `#month ?: 'all'` | Every calendar request needs the month's festivals |
| `FestivalServiceImpl.syncCalendarificFestivals()` | `@CacheEvict(FESTIVALS_BY_MONTH, allEntries)` | — | Data changed |
| Special-event seed writes | Evict `festivalsByMonth` | — | Admin edits change festival rows |
| `DashboardServiceImpl.getDashboard(String, boolean)` | `@Cacheable(DASHBOARD_SUMMARY)` | caller + role | Expensive counters, tolerant of 60 s staleness |
| `IpInfoService.lookup(String)` | `@Cacheable(IP_INFO, unless = status != 'success')` | IP address | ip-api.com allows 45 requests/minute |
| `EmailImageService.load(Long, String)` | `@Cacheable(EMAIL_IMAGES)` | email log id | Images are large; loaded on demand |
| `ThemeSeed` entity | `@Cache(usage = READ_WRITE, region = SEED_DATA)` | Hibernate | Read on every theme request |

## 4. Cache rules and invariants

1. **Bounded.** No cache may be unbounded; the template enforces 1 000 entries/30 min for anything
   undeclared.
2. **Serializable.** Values in caches that may be configured to store by value implement
   `Serializable` (`FestivalResponse`, `DashboardResponse`, `EmailImage`, `ViolatedUserIpInfo`).
3. **Evict on write.** Any write that changes festivals evicts `festivalsByMonth`; Hibernate handles
   the seed regions itself.
4. **Do not cache failures.** `IpInfoService` caches only `status=success` results, so a rate-limit
   response is not sticky.
5. **Short TTL for counters.** `dashboardSummary` is intentionally 60 s; a stale counter is harmless.
6. **Images keyed by log id.** Deleting an email should also evict its image (the delete path removes
   the storage object; the cache entry expires within 15 minutes).
7. **Per instance.** Caches are not shared; a second instance has its own copy and its own cold start.

## 5. Cache and consistency

| Scenario | Behaviour |
| --- | --- |
| Admin edits a festival row | The service evicts `festivalsByMonth`; the calendar is correct on the next read |
| Calendarific sync runs | All festival cache entries evicted |
| Seed row updated | Hibernate `READ_WRITE` invalidates the entity; the query region invalidates by timestamp |
| Email deleted | Storage object removed; cached image lives at most 15 minutes |
| IP address changes location | Stale for up to 24 hours (acceptable) |
| Dashboard counters | Up to 60 s stale |
| Second instance started | Independent cold cache; no invalidation is propagated |

## 6. Risks

| Risk | Mitigation |
| --- | --- |
| A missed eviction serves stale festivals for up to 6 hours | Every special-event write evicts; add a test when a new write path appears |
| Large images in heap | `emailImages` capped at 30 entries |
| `default-update-timestamps-region` expiring would serve stale query results | Declared without TTL |
| Cache provider misconfiguration fails at first use | `CacheConfigurationTest` checks every name |
| Multi-instance inconsistency | Documented limitation; move to a shared cache if instances grow |

## 7. Tuning guidance

* Increase `festivalsByMonth` TTL only if festival edits are rare; decrease if admins edit often.
* Keep `ipInfo` at 24 h to stay inside the ip-api.com quota.
* Reduce `dashboardSummary` to 30 s if operators complain about stale counters.
* If `emailImages` causes heap pressure, lower the entry cap before lowering the TTL (TTL protects
  correctness, the cap protects memory).

## 8. Disabling caches

Set `spring.cache.type=none` (or remove the JCache config) to bypass Spring caching; Hibernate's
second-level cache is controlled separately by
`spring.jpa.properties.hibernate.cache.use_second_level_cache=false`. Both are configuration-only
changes, but expect higher provider usage and slower calendar/dashboard reads.
