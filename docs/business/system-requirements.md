# System requirements

This document is the requirements baseline for release `1.0.0`. Detailed statements live in
[functional-requirements.md](functional-requirements.md) and
[non-functional-requirements.md](non-functional-requirements.md); this page summarises them and
records the system-level interfaces and constraints.

## 1. System purpose

A single Spring Boot service that stores family dates, generates personalised greetings with an AI
model, delivers them by email on a schedule, and provides calendar, circle, chat, notification and
dashboard capabilities through a JSON API and a STOMP WebSocket.

## 2. Actors

| Actor | Type | Description |
| --- | --- | --- |
| Administrator | Human, `ROLE_ADMIN` | Manages users, events, festivals, mappings, content, notifications; reads dashboards; triggers jobs; impersonates users |
| Family member | Human, `ROLE_USER` | Manages their own profile and settings; views calendar; manages circles; chats; reads notifications |
| Unauthenticated visitor | Human | Can sign in, request OTP, reset password, read published notifications, read `/health`, read OpenAPI |
| Quartz scheduler | System | Runs durable jobs from the JDBC job store |
| Spring scheduler | System | Runs the Instagram and delivery-ops jobs |
| Gmail API | External | Sends email using OAuth credentials |
| Gemini API | External | Generates wish text and powers the agents |
| Pollinations | External | Generates greeting images |
| Calendarific | External | Supplies festival dates |
| ip-api.com | External | Resolves IP geolocation |
| Supabase Storage | External | Stores media objects |
| PostgreSQL (primary) | Infrastructure | Business data |
| PostgreSQL (chat) | Infrastructure | Conversations, messages, reactions |

## 3. Functional requirements summary

| Group | IDs | Summary |
| --- | --- | --- |
| Authentication | FR-AUTH-01…18 | Password, OTP, SSO, direct login, reset, MFA gate, lockout, single session, rate limits |
| Users | FR-USER-01…10 | Admin CRUD, directory, self-service settings, pictures, deactivation |
| Events | FR-EVT-01…07 | Per-user events with seeded types, media, recurrence |
| Festivals | FR-FEST-01…08 | Calendarific sync, on-demand years, mappings, bulk, status filters |
| Wishes and email | FR-WISH-01…14 | Generation, fallback, image selection, links, template, logging, retry, agents |
| Calendar | FR-CAL-01…06 | Month aggregation, circle sharing, year scoping |
| Circles | FR-CIR-01…10 | Requests, responses, reminders, calendar/chat unlocking |
| Chat | FR-CHAT-01…09 | Messaging, attachments, reactions, presence, realtime, unread digest |
| Notifications | FR-NOTIF-01…05 | Authoring, publish window, email, public read |
| Themes | FR-THEME-01…03 | Seeded themes, per-user preference |
| Media | FR-MEDIA-01…05 | Byte-sniffed validation, size limits, gzip, signed URLs |
| Seed data | FR-SEED-01…04 | Reference data and wish-image administration |
| Dashboards | FR-DASH-01…07 | Counters, charts, login locations, failed-login analytics |
| Operations | FR-OPS-01…05 | Health, OpenAPI, scheduler control, agent status |
| Cross-cutting | FR-X-01…04 | IST timestamps, paged shape, error shape, non-disclosure |

## 4. Non-functional requirements summary

| Attribute | Target |
| --- | --- |
| Performance | Interactive endpoints respond in well under a second at expected volume; expensive work (AI, email, image generation) is asynchronous; list endpoints are paged |
| Availability | 99% monthly for the API; wishes may be retried; scheduler jobs are durable and clustered |
| Scalability | Horizontal scaling possible through the clustered Quartz job store and stateless JWT sessions; caches and rate limiter are per instance |
| Security | JWT bearer auth, BCrypt hashes, method-level authorisation, per-IP rate limiting, byte-sniffed uploads, secrets only from environment |
| Usability | Consistent paged payloads, filter-values endpoints for UI dropdowns, clear error messages, Swagger UI |
| Maintainability | Layered packages, interface/implementation split, 223 unit tests, Google Java Format |
| Portability | Container image; configuration only through properties and environment variables |
| Observability | `/health`, structured log lines, email log, scheduler statistics |

## 5. External interfaces

| Interface | Direction | Protocol / format | Authentication |
| --- | --- | --- | --- |
| Client application | Inbound | HTTPS, JSON, multipart | Bearer JWT |
| Realtime chat | Inbound | STOMP over WebSocket/SockJS at `/ws-chat` | Endpoint is open; message payloads carry sender identity |
| Gmail API | Outbound | HTTPS JSON, base64url MIME | OAuth 2.0 refresh token |
| SMTP | Outbound | SMTP 587 STARTTLS | Username/password |
| Brevo | Outbound | HTTPS JSON | API key |
| Gemini | Outbound | HTTPS JSON (`generateContent`, `models`) | `x-goog-api-key` |
| Pollinations | Outbound | HTTPS (image bytes, balance JSON) | Optional bearer |
| Calendarific | Outbound | HTTPS JSON | `api_key` query parameter |
| ip-api.com | Outbound | HTTP JSON | None (rate limited) |
| Supabase Storage | Outbound | S3 API (AWS SDK v2) | Access key / secret |
| PostgreSQL primary | Outbound | JDBC | Username/password |
| PostgreSQL chat | Outbound | JDBC | Username/password |
| Instagram webhook | Inbound | HTTPS GET/POST | Verify token on GET |

## 6. Data requirements

* Two PostgreSQL databases: business data (Hibernate-managed, `ddl-auto=update`) and chat data
  (schema created by `ChatSchemaInitializer` on startup).
* Media in object storage; only object keys (and optionally public URLs) are stored in the database.
* Email bodies and images are retained in `email_logs` for the status views.
* Quartz tables (`QRTZ_*`) are created automatically (`spring.quartz.jdbc.initialize-schema=always`).

## 7. Constraints

| # | Constraint |
| --- | --- |
| C1 | Java 17 toolchain; Spring Boot 3.3.3; Gradle build |
| C2 | PostgreSQL only (Hibernate dialect is fixed to PostgreSQL) |
| C3 | Single container deployment on Render; `prod` profile baked into the image entrypoint |
| C4 | No schema migration tool; Hibernate updates the schema |
| C5 | Third-party quotas apply (Gmail, Gemini, Pollinations, Calendarific, ip-api.com) |
| C6 | All scheduling in Asia/Kolkata |
| C7 | Uploads capped at 50 MB by the servlet container |

## 8. Assumptions

See [business-requirements.md](business-requirements.md) §6. In addition: the deployment has
outbound internet access; a JDK 17 is discoverable by Gradle; the UI sends ISO dates
(`yyyy-MM-dd`).

## 9. Acceptance and verification

Requirements are accepted when the criteria in [acceptance-criteria.md](acceptance-criteria.md) pass
and the automated suite (`./gradlew test`) is green. Manual and exploratory checks are described in
[../testing/test-plan.md](../testing/test-plan.md).

## 10. Traceability

| Requirement group | Implementation | Tests |
| --- | --- | --- |
| FR-AUTH | `AuthServiceImpl`, `JwtService`, `JwtAuthFilter`, `RateLimiter` | `AuthServiceImplTest`, `JwtServiceTest`, `JwtAuthFilterTest`, `RateLimiterTest` |
| FR-USER | `UserServiceImpl`, `UserController` | `UserServiceImplTest`, `UserDirectoryTest` |
| FR-EVT | `EventServiceImpl` | `EventServiceImplTest` |
| FR-FEST | `FestivalServiceImpl`, `FestivalWishMappingServiceImpl` | `FestivalWishMappingServiceImplTest` |
| FR-WISH | `AiServiceImpl`, `AgenticAiService`, `GmailEmailServiceImpl`, `EmailServiceImpl` | `AiServiceImplTest`, `GmailEmailServiceImplTest`, `AgentRunnerTest`, `GeminiClientTest` |
| FR-CAL | `CalendarServiceImpl` | `CalendarServiceImplTest` |
| FR-CIR | `CircleServiceImpl`, `CircleRequestNotifier` | `CircleServiceImplTest`, `CircleRequestNotifierTest` |
| FR-CHAT | `ChatServiceImpl` | `ChatServiceImplCircleGateTest` |
| FR-NOTIF | `NotificationServiceImpl` | `NotificationServiceImplTest` |
| FR-THEME | `ThemeServiceImpl` | `ThemeServiceImplTest` |
| FR-MEDIA | `MediaValidation`, `MediaContentTypes`, `GzipUtil`, `SupabaseStorageService` | `MediaValidationTest`, `MediaContentTypesTest`, `GzipUtilTest`, `WishImageServiceImplTest` |
| FR-DASH | `DashboardServiceImpl` | `DashboardServiceImplTest` |
| FR-OPS | `HealthController`, `SchedulerManagementService` | — (manual) |
