# High-level design

## 1. Purpose

Show, at a glance, which services exist, what they talk to, and how data flows between them. Detail
lives in [system-design.md](system-design.md) and [low-level-design.md](low-level-design.md).

## 2. Services and their collaborators

| Service | Reads from | Writes to | Calls out |
| --- | --- | --- | --- |
| `AuthService` | `users`, `refresh_tokens`, `otp_codes`, `password_reset_tokens`, `login_tokens`, `login_location_events`, `violated_users` | same + chat unread count | Gmail (OTP, reset, alerts), Google (ID token verification) |
| `UserService` | `users`, `relationship_seeds`, `events`, `festival_wish_mappings` | `users`, `user_wish_settings`, `user_theme_preferences` | Storage (pictures), AI (wish preview) |
| `EventService` | `events`, `event_type_seeds`, `users` | `events` | Storage (media) |
| `FestivalService` | `seed_special_events` | `seed_special_events` | Calendarific |
| `FestivalWishMappingService` | `festival_wish_mappings`, `seed_special_events`, `users` | `festival_wish_mappings` | Storage (image) |
| `CalendarService` | festivals, events, users, circles | — | — |
| `CircleService` | `circles`, `users` | `circles` | Gmail (request/reminder/accept emails), `AuthService` (direct-login token) |
| `ChatService` | chat DB, `users`, `circles` | chat DB, `users` (presence) | Storage (attachments), STOMP broker |
| `NotificationService` | `notifications`, `users` | `notifications` | Gmail, STOMP broker |
| `DashboardService` | `email_logs`, `ig_message_logs`, `users`, `events`, `login_location_events`, `violated_users` | — | ip-api.com |
| `AiService` | `wish_images` | — | Gemini, Pollinations |
| `WishImageService` | `wish_images`, `users` | `wish_images` | Storage |
| `ThemeService` | `theme_seeds`, `user_theme_preferences` | `user_theme_preferences` | — |
| `EmailService` / `GmailEmailService` | `email_logs`, `users`, storage | `email_logs` | Gmail API / SMTP / Brevo |
| Schedulers | events, users, mappings, circles, email logs, chat DB | date stamps, `email_logs`, `ig_message_logs` | AI, storage, Gmail |

## 3. Integration overview

```
                      ┌──────────────┐
      UI ────────────►│  REST API    │◄──────────── UptimeRobot (GET /health)
      UI ◄────────────┤  + STOMP     │◄──────────── Instagram webhook (GET/POST)
                      └──────┬───────┘
                             │
        ┌────────────────────┼─────────────────────┬──────────────────┐
        ▼                    ▼                     ▼                  ▼
  ┌───────────┐       ┌────────────┐        ┌────────────┐     ┌────────────┐
  │  Business │       │    Chat    │        │  Delivery  │     │     AI     │
  │  services │       │  services  │        │  services  │     │  services  │
  └─────┬─────┘       └──────┬─────┘        └─────┬──────┘     └─────┬──────┘
        │                    │                    │                  │
        ▼                    ▼                    ▼                  ▼
  Primary Postgres     Chat Postgres        Gmail API           Gemini
  (JPA + Quartz)       (plain SQL)          SMTP / Brevo        Pollinations
        │                    │                    │              Calendarific
        └──────────┬─────────┘                    │              ip-api.com
                   ▼                              │
            Supabase Storage ◄────────────────────┘ (email images, signature)
```

## 4. Data flow in one paragraph

An administrator creates a user and their events. A Quartz job wakes at 06:10 IST, finds today's
events, asks the AI service for text and an image, renders the email, and hands it to the delivery
service, which sends it through Gmail and records the result in `email_logs`. The same pattern
applies to birthdays (06:30), good morning (06:00), good night (22:00) and festival wishes (07:30).
Users sign in, see their month on the calendar (their own entries plus accepted circle members'), and
can chat with circle members; messages go to the chat database and are pushed over STOMP. All
outbound media goes to Supabase Storage; all email attempts are visible through the status APIs and
dashboards.

## 5. Cross-cutting concerns

| Concern | Where it is handled |
| --- | --- |
| Authentication | `JwtAuthFilter` + `SecurityConfig` |
| Authorisation | URL rules, `@PreAuthorize`, service-level ownership checks |
| Rate limiting | `RateLimitFilter` + `RateLimiter` |
| Validation | Bean Validation on DTOs, `MediaValidation` for files, `WishesUrls` for links |
| Error shape | `GlobalExceptionHandler` |
| Time | `Asia/Kolkata` everywhere (`scheduler.time-zone`, `AuditableEntity`) |
| Caching | Ehcache (`ehcache.xml`) + Spring `@Cacheable`/`@CacheEvict` |
| Async | `@Async` executor, Quartz thread pool (5 threads) |
| Secrets | Environment variables injected as properties |
| Observability | Logs, `/health`, `/api/schedulers`, `email_logs`, dashboards |

## 6. Deployment view (summary)

One container on Render behind TLS, talking to Neon (primary DB), Supabase (chat DB + storage) and
the external providers. Configuration comes entirely from environment variables; the image runs the
`prod` profile. See [deployment-architecture.md](deployment-architecture.md).

## 7. Design principles applied

1. **Explicit boundaries** — controllers never see entities; services never see HTTP types.
2. **Fail soft where the user cannot help** — image generation, geolocation, circle notification
   emails and realtime pushes degrade instead of failing the request.
3. **Fail loud where the operator must act** — AI exhaustion returns `503`; email failures are
   logged and retried; scheduler failures are recorded.
4. **Idempotent by date** — daily sends are guarded by date stamps.
5. **Least data on the wire** — directory projection, `hasImage` instead of base64, paged lists.
6. **Configuration over code** — every tunable has a property with an environment override.
