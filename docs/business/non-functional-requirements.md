# Non-functional requirements

Each requirement is stated with the mechanism that satisfies it in this codebase, so the document
can be verified by inspection or by test.

## 1. Performance and capacity

| ID | Requirement | Implementation / evidence |
| --- | --- | --- |
| NFR-P-01 | Interactive read endpoints shall complete within 500 ms at p95 for the expected family-scale load (tens of users, thousands of rows). | Indexed queries, paged results, Ehcache second-level and query cache for seed data, `festivalsByMonth` cache, `dashboardSummary` cache (60 s). |
| NFR-P-02 | Any work that calls an external provider (AI text, AI image, email send) shall not block the HTTP response where the flow allows it. | `@Async` on `GmailEmailService.sendEmailWithAttachments`; async executor core 5 / max 10 / queue 100; wish generation happens in scheduled jobs. |
| NFR-P-03 | AI text generation shall not fail the whole request when one model is unavailable. | `GeminiClient` model fallback: up to `GEMINI_FALLBACK_MAX_RETRIES` (5) extra models, 60 s cooldown per failed model. |
| NFR-P-04 | Image generation failure shall not prevent email delivery. | `AiServiceImpl.callGeminiImage` returns `null` on error; callers send without an image. |
| NFR-P-05 | Database connections shall be pooled and bounded. | Hikari: max pool 10 (`DB_POOL_SIZE`), min idle 5, idle timeout 30 s, max lifetime 60 s, connection timeout 20 s, leak detection 20 s; chat pool max 10 (`SUPABASE_DB_POOL_SIZE`). |
| NFR-P-06 | No endpoint shall return an unbounded collection. | All lists use `PagedResponse`; login-location points capped at 1000; failed-login map points capped at 300; chat pages default 20–50. |
| NFR-P-07 | Caches shall be bounded in size and time. | `ehcache.xml` declares an entry cap and TTL for every cache; `CacheConfigurationTest` enforces both and checks every `CacheNames` constant is declared. |
| NFR-P-08 | Uploads shall be limited before application validation. | `spring.servlet.multipart.max-file-size=50MB`, `max-request-size=50MB`; per-kind limits in `MediaValidation` (5/10/20/50 MB). |
| NFR-P-09 | The service shall start and serve `/health` on a small container. | Single Spring Boot process, no local cache server, no local file storage. |
| NFR-P-10 | Email retries shall be bounded to avoid loops. | `retryCount < 3` for both `EmailService.retryFailed` and the delivery-ops agent. |

## 2. Availability and reliability

| ID | Requirement | Implementation / evidence |
| --- | --- | --- |
| NFR-A-01 | The API shall be monitored and alerted when down. | UptimeRobot HTTP monitor on `/health` every 5 minutes, alert on DOWN and UP. |
| NFR-A-02 | Scheduled work shall survive a restart and shall not double-run across instances. | Quartz JDBC job store, `isClustered=true`, `instanceId=AUTO`, `initialize-schema=always`, `storeDurably` jobs. |
| NFR-A-03 | A failed wish for one recipient shall not stop the others. | Per-recipient try/catch in `WishesScheduler`, `BirthdayMailScheduler`, `FestivalScheduler`, `GoodMorningScheduler`, `GoodNightScheduler`. |
| NFR-A-04 | A mail failure shall not block a circle request or an acceptance. | `CircleServiceImpl.notifyNewRequest` / `notifyAccepted` swallow and log mail errors. |
| NFR-A-05 | Notification problems shall not break chat or calendar reads. | Realtime publishing is best-effort; calendar reads do not depend on mail. |
| NFR-A-06 | Duplicate wishes shall be prevented after a partial failure. | `users.last_birthday_wish_sent`, `festival_wish_mappings.last_wish_sent_on` + `is_error`, `email_logs` records. |
| NFR-A-07 | A provider outage shall produce a defined error, not a hang. | `AiUnavailableException` → `503`; `RestTemplate` defaults; geolocation failures returned as `status=fail` and not cached. |
| NFR-A-08 | Refresh tokens shall not accumulate forever. | `RefreshTokenCleanupScheduler` deletes expired tokens daily at 00:00 IST. |

## 3. Security

| ID | Requirement | Implementation / evidence |
| --- | --- | --- |
| NFR-S-01 | All non-public endpoints shall require a valid bearer token. | `SecurityConfig` permits only `/api/auth/**`, docs, `/health`, `/actuator/**`, `/google/**`, `/oauth2callback`, `/gmail/**`, `/instagram/**`, `/ws-chat/**`, `/api/notifications/**`; everything else is `authenticated()`. |
| NFR-S-02 | Administrative operations shall be restricted to `ROLE_ADMIN`. | `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` on admin controllers and methods. |
| NFR-S-03 | Passwords shall be stored only as BCrypt hashes. | `BCryptPasswordEncoder`; `UserServiceImpl.create` encodes the initial password. |
| NFR-S-04 | Credentials shall never be hard-coded. | Every secret is a property with an environment placeholder (`JWT_SECRET`, `GEMINI_API_KEY`, `GMAIL_*`, `SUPABASE_*`, …). |
| NFR-S-05 | Authentication endpoints shall resist brute force. | In-memory sliding-window limiter, 10 requests / 60 s per IP and path, on 10 auth paths; account lock after 5 bad passwords. |
| NFR-S-06 | Single-use tokens shall expire and be consumed once. | OTP 5 min, reset 30 min, direct login 60 min; `used` flag set on consumption. |
| NFR-S-07 | Uploads shall be validated by content. | `MediaContentTypes` byte sniffing + `MediaValidation` agreement and size checks. |
| NFR-S-08 | Stored links shall not enable HTML injection. | `WishesUrls` restricts scheme/host/length; `WishEmailExtras.escape` escapes all user text. |
| NFR-S-09 | Sensitive emails shall not be readable by other users. | `SENSITIVE_TYPES = OTP, FORGOT_PASSWORD, CIRCLE_REQUEST` blocked for non-admins in `GmailEmailServiceImpl`. |
| NFR-S-10 | A superseded session shall be rejected promptly. | `sid` claim compared with `users.active_session_id` in `JwtAuthFilter`; `401 CONCURRENT_LOGIN:` body. |
| NFR-S-11 | Transport shall be encrypted in production. | Render terminates TLS for the public host; all provider calls use HTTPS (ip-api.com is HTTP and carries no credentials). |

## 4. Scalability

| ID | Requirement | Implementation / evidence |
| --- | --- | --- |
| NFR-SC-01 | The service shall run more than one instance without duplicate scheduled sends. | Clustered Quartz; `SchedulerTrackingService` is per instance and is not used for coordination. |
| NFR-SC-02 | Sessions shall not require server-side state. | Stateless JWT; refresh tokens in the database. |
| NFR-SC-03 | Chat storage shall scale independently of business data. | Separate database and datasource (`app.supabase.db.*`), plain SQL. |
| NFR-SC-04 | Media shall not bloat the database. | Objects in Supabase Storage; only keys/URLs stored. |
| NFR-SC-05 | Growth in reference data shall not degrade lookups. | Second-level cache for seeds, `READ_WRITE` on `ThemeSeed`, query cache for by-code lookups. |
| NFR-SC-06 | The known scaling limits shall be documented. | Rate limiter and caches are per instance; see [../operations/known-issues.md](../operations/known-issues.md). |

## 5. Usability

| ID | Requirement | Implementation / evidence |
| --- | --- | --- |
| NFR-U-01 | Clients shall be able to build filter dropdowns without hard-coding enums. | `/filter-values` endpoints for users, events, emails, mappings, seeds, wish images. |
| NFR-U-02 | List responses shall have one consistent shape. | `PagedResponse` everywhere. |
| NFR-U-03 | Errors shall be machine-readable and human-readable. | `{timestamp, status, message}` with meaningful messages. |
| NFR-U-04 | The API shall be self-documenting. | springdoc OpenAPI + Swagger UI, operations sorted by method, tags alphabetically. |
| NFR-U-05 | Emails shall render in common clients. | Inline CSS, table-based button, `cid:` inline images, plain-text link fallback, signature image. |
| NFR-U-06 | The UI shall be themeable per user. | `/api/themes`, `/api/themes/me`, `user_theme_preferences`. |
| NFR-U-07 | Dates shall be unambiguous. | All timestamps IST; API dates ISO `yyyy-MM-dd`. |

## 6. Maintainability and quality

| ID | Requirement | Implementation / evidence |
| --- | --- | --- |
| NFR-M-01 | Code shall follow one formatting standard. | Spotless with google-java-format 1.23.0, unused-import removal, trailing-whitespace trim. |
| NFR-M-02 | Business logic shall be unit-tested. | 33 test classes, 223 `@Test` methods, JUnit 5 + Mockito. |
| NFR-M-03 | Layers shall be separated. | `controller` → `service` (interface) → `service.impl` → `repository`; DTO records at the boundary; entities never returned raw. |
| NFR-M-04 | Configuration shall be externalised. | `application.properties` with `${ENV:default}` for every tunable. |
| NFR-M-05 | Repository-specific knowledge shall be captured. | `AGENTS.md` plus this documentation set. |
| NFR-M-06 | The build shall be reproducible. | Gradle dependency management, Spring Boot BOM, pinned AWS SDK BOM and JJWT version. |

## 7. Portability and deployment

| ID | Requirement | Implementation / evidence |
| --- | --- | --- |
| NFR-D-01 | The service shall run as a container. | Multi-stage `Dockerfile` (gradle:8.14.3-jdk17-alpine build, eclipse-temurin:17-jre-alpine runtime). |
| NFR-D-02 | The runtime shall need no build tools. | JRE-only final image, `app.jar`, `EXPOSE 8080`. |
| NFR-D-03 | Configuration shall be supplied at runtime. | `--env-file` / Render environment variables; `prod` profile in the entrypoint. |
| NFR-D-04 | The service shall be reachable at a stable URL. | Render service `eventmanagement-backend-ka9x.onrender.com`. |

## 8. Compliance and privacy

| ID | Requirement | Implementation / evidence |
| --- | --- | --- |
| NFR-C-01 | Personal data shall be limited to what the product needs. | Name, email, gender, birthday, relationship, optional picture and wishes URL. |
| NFR-C-02 | Access to personal data shall be least-privilege. | Directory endpoint returns a reduced projection; events are owner/admin only; chat is circle-gated. |
| NFR-C-03 | Failed-login data shall be retained only as long as needed. | `violated_users` is not automatically purged today — see [../security/privacy-and-data-retention.md](../security/privacy-and-data-retention.md). |
| NFR-C-04 | Data shall be deletable on request. | Users are soft-deleted (`deleted=true`); hard deletion is a manual database operation. |

## 9. Verification

| NFR area | How verified |
| --- | --- |
| Performance | Manual load checks ([../testing/performance-testing.md](../testing/performance-testing.md)) |
| Availability | UptimeRobot, Quartz misfire behaviour |
| Security | [../testing/security-testing.md](../testing/security-testing.md), `RateLimiterTest`, `JwtAuthFilterTest`, `MediaValidationTest` |
| Maintainability | `./gradlew test`, `spotlessCheck` (with known pre-existing violations) |
| Portability | `docker build` + container smoke test |
