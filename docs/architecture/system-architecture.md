# System architecture

## 1. Architectural style

A **modular monolith**: one deployable Spring Boot jar, internally split into layers
(`controller` → `service` → `repository`) and feature packages (`auth`, `user`, `event`, `festival`,
`chat`, `circle`, `notification`, `dashboard`, `ai`, `agent`, `media`). There is no service mesh, no
message broker and no separate worker process; scheduling and asynchronous work happen inside the
same JVM.

That choice fits the load (one family group, tens of users, a handful of jobs per day) and keeps the
operating cost to one container plus two managed databases.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            Clients (separate repo)                           │
│   Angular SPA (Firebase Hosting)  ·  Email clients  ·  Monitoring probes     │
└───────────────┬───────────────────────────────┬──────────────────────────────┘
                │ HTTPS / JSON / multipart      │ STOMP over WebSocket (/ws-chat)
                ▼                               ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      Spring Boot 3.3 application (Java 17)                   │
│                                                                              │
│  Filters: RateLimitFilter → JwtAuthFilter → Spring Security chain            │
│  Controllers: /api/auth /api/users /api/events /api/festivals /api/calendar  │
│               /api/circles /api/chat /api/notifications /api/emails          │
│               /api/dashboard /api/themes /api/admin /api/agent /health       │
│  Services: AuthService UserService EventService FestivalService              │
│            CalendarService CircleService ChatService NotificationService     │
│            DashboardService AiService ThemeService WishImageService          │
│  Repositories: Spring Data JPA (primary) · NamedParameterJdbcTemplate (chat) │
│  Jobs: Quartz (JDBC, clustered) · Spring @Scheduled · @Async executor        │
│  Cache: Ehcache/JCache (second-level + query cache + Spring @Cacheable)      │
└───┬───────────────┬───────────────┬───────────────┬──────────────┬───────────┘
    │               │               │               │              │
    ▼               ▼               ▼               ▼              ▼
┌────────┐  ┌─────────────┐  ┌───────────┐  ┌────────────┐  ┌──────────────┐
│Primary │  │Chat Postgres│  │ Supabase  │  │ Gmail API  │  │ Gemini       │
│Postgres│  │ (Supabase)  │  │ Storage   │  │ SMTP/Brevo │  │ Pollinations │
│(Neon)  │  │             │  │ (S3 API)  │  │            │  │ Calendarific │
└────────┘  └─────────────┘  └───────────┘  └────────────┘  │ ip-api.com   │
                                                            └──────────────┘
```

## 2. Major components

| Component | Responsibility | Key classes |
| --- | --- | --- |
| HTTP edge | Routing, validation, DTO mapping, status codes | 24 controllers in `com.familywishes.controller` plus `ChatSocketController` (25 `@RestController` classes) |
| Security edge | Bearer-token authentication, per-IP rate limiting, single-session policing | `SecurityConfig`, `JwtAuthFilter`, `RateLimitFilter`, `RateLimiter`, `JwtService`, `CustomUserDetailsService`, `AppUserDetails` |
| Identity | Password/OTP/SSO/direct login, reset, lockout, impersonation | `AuthServiceImpl`, `AuthController` |
| People | User CRUD, directory, preferences, pictures | `UserServiceImpl` |
| Dates | Events and their seeded types | `EventServiceImpl` |
| Festivals | Calendarific sync, year fill, festival mappings | `FestivalServiceImpl`, `FestivalWishMappingServiceImpl` |
| Calendar | Aggregation of festivals, birthdays, events, circle sharing | `CalendarServiceImpl` |
| Social graph | Circle requests and membership | `CircleServiceImpl`, `CircleRequestNotifier` |
| Chat | Conversations, messages, reactions, presence | `ChatServiceImpl`, `ChatMessageRepository`, `ChatRealtimePublisher`, `ChatSocketController`, `ChatSchemaInitializer` |
| Content | Wish images, seed tables, themes, notifications | `WishImageServiceImpl`, `*SeedServiceImpl`, `ThemeServiceImpl`, `NotificationServiceImpl` |
| AI | Wish text, images, model fallback, agents | `AiServiceImpl`, `GeminiClient`, `GeminiModelCatalog`, `AgentRunner`, `WishComposerAgent`, `DeliveryOpsAgent`, `AdminCopilotAgent`, `AgenticAiService` |
| Delivery | Email composition and sending, logging, retry | `GmailEmailServiceImpl`, `EmailServiceImpl`, `BrevoEmailServiceImpl`, `EmailTemplateBuilder`, `EmailImageService` |
| Storage | Object upload/download, signed URLs, gzip | `SupabaseStorageService`, `GzipUtil`, `MediaContentTypes`, `MediaValidation` |
| Scheduling | Daily wishes, reminders, cleanups, sync | `WishesSchedulerConfig` + 9 Quartz jobs + 4 Spring `@Scheduled` tasks |
| Insight | Dashboards and charts | `DashboardServiceImpl`, `IpInfoService`, `ViolatedUserAuditService` |
| Operations | Health, OpenAPI, scheduler control | `HealthController`, `SchedulerManagementService`, `SchedulerTrackingService`, `OpenApiConfig` |

## 3. Layering rules

| Layer | May depend on | Must not do |
| --- | --- | --- |
| `controller` | service interfaces, DTOs | contain business rules, touch repositories |
| `service` (interface) | DTOs, entities in signatures only where unavoidable | expose JPA specifics |
| `service.impl` | repositories, entities, other services, clients | return entities to controllers |
| `repository` | entities | contain business rules |
| `entity` | converters, enums | depend on services or DTOs |
| `security`, `config` | everything needed to wire the app | contain domain rules |
| `util` | JDK only (plus small libraries) | hold state |

Cross-cutting conventions: DTOs are Java records; entities extend `AuditableEntity` (created/updated
timestamps in IST) and optionally `ActivatableEntity` (`active` flag); all lists return
`PagedResponse`.

## 4. Runtime topology

| Node | Role |
| --- | --- |
| Render web service | Runs the container (`prod` profile, port 8080) |
| Neon PostgreSQL | Primary database: users, events, festivals, mappings, email logs, notifications, security events, Quartz tables |
| Supabase PostgreSQL | Chat database: conversations, messages, reactions |
| Supabase Storage | Media bucket `golden-greetings` (S3-compatible endpoint, region `ap-southeast-1`) |
| Gmail API | Email transport (OAuth 2.0 refresh token) |
| Gemini | Text generation and agents |
| Pollinations | Greeting image generation |
| Calendarific | Festival data |
| ip-api.com | IP geolocation |
| UptimeRobot | External availability monitoring of `/health` |

## 5. Request lifecycle

1. TLS terminates at Render; the request reaches Tomcat on port 8080.
2. `RateLimitFilter` checks the sliding window for ten auth paths; over-limit returns `429` directly.
3. `JwtAuthFilter` parses `Authorization: Bearer`, verifies the signature and expiry, loads the user,
   and rejects a stale `sid` when single-session sign-in is on.
4. Spring Security authorises the URL; `@PreAuthorize` authorises the method.
5. The controller validates the payload (`jakarta.validation`), calls a service, and returns a DTO.
6. `GlobalExceptionHandler` maps exceptions to `{timestamp, status, message}`.
7. Responses larger than 1 KB in the configured MIME types are gzipped by the container.

## 6. State and consistency

* **Business data** is transactional through JPA (`@Transactional` on service methods).
* **Chat data** is plain SQL against a second datasource; it is not part of the business
  transaction, so a chat failure never rolls back a business change.
* **Sessions** are stateless (JWT). The only server-side session state is the refresh-token row and
  `users.active_session_id`.
* **Idempotency** is achieved with date stamps (`last_birthday_wish_sent`, `last_wish_sent_on`) and
  unique constraints (`circles` pair, festival mapping pair, refresh/reset/login tokens, chat
  conversation pair).
* **Caches** are read-through and bounded; correctness relies on eviction when data changes
  (festival sync and special-event writes clear `festivalsByMonth`).

## 7. Failure isolation

| Failure | Behaviour |
| --- | --- |
| Gemini unavailable | `GeminiClient` tries other models; if none answers, `503` and the scheduler logs the failure per recipient |
| Pollinations unavailable | Email still sent without an image |
| Gmail unavailable | `email_logs.status=FAILED`, retried up to 3 times, delivery-ops agent may alert |
| Calendarific unavailable | Sync logged, existing festival rows kept |
| ip-api.com rate limited | Lookup returns `status=fail`, not cached, dashboard still renders |
| Supabase Storage unavailable | Upload returns `null`/`400` for user-facing uploads; emails fall back to no image or a URL signature |
| Chat database unavailable | Chat endpoints fail; the rest of the API is unaffected |
| Scheduler crash mid-run | Quartz marks the job failed; `SchedulerTrackingService` records the error; per-recipient guards limit the blast radius |

## 8. Architectural constraints and trade-offs

| Decision | Consequence |
| --- | --- |
| One deployable unit | Simple operations, but a change to any module redeploys everything |
| Two datasources | Chat scales and fails independently, but there are no cross-database joins or transactions |
| Hibernate `ddl-auto=update` | Fast iteration, no migration history |
| In-process cache and rate limiting | No extra infrastructure, but not shared across instances |
| JWT without server-side access-token store | Cheap verification; revocation relies on the short TTL and the `sid` claim |
| Email through Gmail API | No SMTP reputation setup, but bound to Google's OAuth and quotas |

## 9. Related documents

* [system-design.md](system-design.md) · [high-level-design.md](high-level-design.md) ·
  [low-level-design.md](low-level-design.md)
* [component-diagram.md](component-diagram.md) · [deployment-architecture.md](deployment-architecture.md)
* [data-flow.md](data-flow.md) · [sequence-diagrams.md](sequence-diagrams.md)
* [../backend/backend-design.md](../backend/backend-design.md)
