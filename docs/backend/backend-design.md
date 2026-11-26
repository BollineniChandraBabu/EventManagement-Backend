# Backend design

## 1. Package structure and responsibilities

| Package | Contents | Responsibility |
| --- | --- | --- |
| `com.familywishes` | `FamilyWishesApplication` | Boot, scheduling, async, caching switches |
| `config` | `SecurityConfig`, `AppConfig`, `GmailConfig`, `WebSocketConfig`, `OpenApiConfig`, `PrimaryDataSourceConfig`, `CacheNames`, `WishTemplateSeedDataInitializer` | Wiring only |
| `security` | `JwtService`, `JwtAuthFilter`, `RateLimitFilter`, `RateLimiter`, `AppUserDetails`, `CustomUserDetailsService` | Authentication, token handling, rate limiting |
| `controller` | 24 REST controllers (25 `@RestController` classes with the chat socket) | HTTP mapping, validation, authorisation annotations |
| `service` | 21 interfaces | Contracts |
| `service.impl` | 27 implementations and helpers | Business rules, orchestration, external calls |
| `repository` | 22 Spring Data interfaces | Persistence |
| `entity` | 22 entities + 9 enums + converter | JPA model |
| `dto` | 17 files, mostly records | API contract |
| `exception` | 5 exceptions + `GlobalExceptionHandler` | Error contract |
| `scheduler` | 9 Quartz jobs + 4 Spring scheduled tasks + config + listener | Time-based work |
| `chat` | datasource config, SQL repository, schema initializer, STOMP controller, DTOs | Chat subsystem |
| `ai` | `GeminiClient`, `GeminiModelCatalog`, `GeminiEndpoint` | Text provider abstraction |
| `agent` | `AgentRunner`, tools, three agents | Tool-calling features |
| `util` | 8 utilities | Pure helpers |

## 2. Request pipeline

```
Tomcat
  └─ RateLimitFilter            (429 on auth paths over budget)
      └─ JwtAuthFilter          (authenticate; 401 on stale sid)
          └─ Spring Security    (URL authorisation)
              └─ DispatcherServlet
                  ├─ HandlerInterceptor (none)
                  ├─ @Valid on @RequestBody  → 400 on violation
                  ├─ @PreAuthorize            → 403 on denial
                  ├─ Controller → Service (@Transactional where needed)
                  └─ GlobalExceptionHandler   → {timestamp,status,message}
```

## 3. Conventions

| Concern | Convention |
| --- | --- |
| Controller method | One HTTP verb per method; returns DTO or `ResponseEntity`; no `try/catch` |
| Validation | Bean Validation on request records; file checks in `MediaValidation`; link checks in `WishesUrls` |
| Authorisation | Admin endpoints annotated `@PreAuthorize("hasRole('ADMIN')")`; ownership checked in services |
| Transactions | `@Transactional` on service methods that write; `readOnly = true` for reads where practical |
| Current user | `SecurityContextHolder` inside services (helper `currentUser()`), or `Authentication` passed from the controller |
| Errors | Throw `NotFoundException` / `BadRequestException` / `RateLimitExceededException` / `ConcurrentLoginException` / `AiUnavailableException` |
| Time | `LocalDateTime.now(ZoneId.of(schedulerTimeZone))`; entities set timestamps in `AuditableEntity` |
| Pagination | `PagedResponse<T>`; default page 0, size 10 (20 for chat/users, 30/50 for messages) |
| Sorting | `sortBy` + `sortDir`, resolved against a whitelist where the field is user-controlled |
| Logging | `@Slf4j`; `log.info` for lifecycle, `log.warn` for degraded behaviour, `log.error` with the exception |

## 4. Service responsibilities in detail

| Service | Owns | Notable rules |
| --- | --- | --- |
| `AuthServiceImpl` | Sign-in variants, tokens, lockout, session bookkeeping | MFA gate, resend caps, single-session, impersonation |
| `UserServiceImpl` | Users, settings, pictures, directory | Initial password `Test`; wishes URL validation; reduced directory projection |
| `EventServiceImpl` | Events | Active seeded type required; owner-or-admin read; image cleanup |
| `FestivalServiceImpl` | Festival rows | Name+year upsert; deactivation; on-demand year fill; cache eviction |
| `FestivalWishMappingServiceImpl` | Mappings | Upsert by pair; bulk; status filters; summary |
| `CalendarServiceImpl` | Aggregation | Circle merge; year scoping; `fromCircle` |
| `CircleServiceImpl` | Circles | Direction handling; addressee-only response; reminders |
| `ChatServiceImpl` | Chat | Circle gate; attachment validation; presence; unread |
| `NotificationServiceImpl` | Notifications | Single published; future-only schedule; email fan-out |
| `DashboardServiceImpl` | Metrics | Role scoping; date validation; caps; geolocation cache |
| `AiServiceImpl` | Wish text and image | Prompt construction; JSON parsing; graceful image failure |
| `WishImageServiceImpl` | Uploaded images | Matching order; event normalisation |
| `ThemeServiceImpl` | Themes | Validation against seeded codes |
| `GmailEmailServiceImpl` | Email | MIME build, logging, sensitive-type filtering, status queries |
| `EmailServiceImpl` | SMTP | Template, inline image, failure alert |
| `SupabaseStorageService` | Objects | Key generation, gzip, signed URLs |

## 5. Cross-cutting implementation notes

**Async.** `@EnableAsync` with a `ThreadPoolTaskExecutor` (core 5, max 10, queue 100). Email sending is
`@Async`, so the HTTP response or scheduler loop does not wait for Gmail. Exceptions inside an async
method are logged by the method itself, not propagated.

**Transactions and external calls.** Wish sending happens outside a transaction in schedulers; the
email log is saved in its own call. `AuthServiceImpl.login` uses `Propagation.REQUIRES_NEW` so the
failure bookkeeping survives the thrown exception.

**Open session in view.** Disabled (`spring.jpa.open-in-view=false`), so entities must be fully loaded
inside the transaction; DTO mapping happens there.

**Hibernate batching.** `connection.handling_mode=DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION`
keeps connections shorter; `lob.non_contextual_creation=true` avoids LOB issues with PostgreSQL.

**Cache.** `spring.cache.type=jcache` with `ehcache.xml`; Hibernate second-level and query caches use
the same provider. `CacheNames` must stay in sync with the XML (`CacheConfigurationTest`).

**JSON.** Jackson defaults; dates serialised as ISO strings; records supported natively.

**Multipart.** 50 MB cap; controllers accept `@RequestPart`/`@RequestParam` mixes for form + file.

## 6. Extension points

| Need | Where to add |
| --- | --- |
| New endpoint | Controller method + service interface method + implementation + test |
| New reference data | Entity + repository + seed controller + `WishTemplateSeedDataInitializer` entry |
| New scheduled job | `Job` class + `JobDetail`/`Trigger` beans in `WishesSchedulerConfig` |
| New external provider | Client class in the owning package, property in `application.properties`, documentation in [external-api-integrations.md](external-api-integrations.md) |
| New email kind | Add to `EmailType`, decide sensitive/non-sensitive, classify keywords |
| New agent | `AgentSpec` + tools + `AgentTool` implementations, flag in properties, endpoint in `AgentController` |
| New cache | `CacheNames` constant + `<cache>` in `ehcache.xml` + test |

## 7. Known deviations and technical debt

1. `EmailServiceImpl` and `GmailEmailServiceImpl` overlap; only Gmail is used by schedulers.
2. `EmailType` inference by keywords is fragile; callers should pass the type.
3. `System.out.println` remains in a few schedulers (`GoodMorningScheduler`, `GoodNightScheduler`,
   `InstagramController`, `NotificationServiceImpl`) alongside proper logging.
4. `SchedulerTrackingService` is in-memory only.
5. `GET /api/users/{id}` is not restricted to admin/self.
6. `violated_users` stores the attempted password.
7. Two scheduling mechanisms and two email transports increase surface area.

## 8. Related documents

* [api-specification.md](api-specification.md) · [business-logic.md](business-logic.md)
* [authentication-and-authorization.md](authentication-and-authorization.md) ·
  [exception-handling.md](exception-handling.md)
* [../architecture/low-level-design.md](../architecture/low-level-design.md)
