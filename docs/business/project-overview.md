# Project overview

## 1. Purpose

**Golden Greetings** is a private family-relationship platform. It keeps a family's important dates
and sends each person a personalised greeting by email on the right day — a birthday, an
anniversary, a festival, a good-morning or good-night note — without anyone having to remember to
write it.

The repository `family-wishes` (service name `family-wishes`, version `1.0.0`, group
`com.familywishes`) is the **backend**: it owns the data, the business rules, the AI generation, the
scheduling and the email delivery. A separate Angular single-page application, hosted on Firebase,
is the user interface.

## 2. Business context

Families coordinate greetings through chat groups and memory. That breaks down in three ways:

1. **Dates get missed.** Birthdays and anniversaries are numerous and easy to forget.
2. **Messages get repetitive.** The same forwarded image and one-liner is sent to everybody.
3. **Coordination is manual.** Someone has to remind the rest of the family.

Golden Greetings addresses all three: dates are stored once, messages are generated per person from
their name, relationship, occasion and tone, and delivery is automated on a schedule with a record
of what was sent.

## 3. Objectives

| Objective | Measure of success |
| --- | --- |
| Never miss a family date | Every active user with a birthday wish enabled receives a wish on the day; every active event mapping receives its wish; a delivery record exists for each attempt |
| Make messages feel personal | Messages contain the recipient's real name and relationship, never placeholders; recent wishes are not repeated (agentic composer reads recent history) |
| Remove manual coordination | Zero-touch scheduling via Quartz; reminders for unanswered circle requests |
| Keep the family private | Data visible only to the owner, their accepted circle and administrators |
| Stay operable by a small team | Admin dashboards, scheduler status/trigger API, email status with retry, health endpoint |
| Keep running cost low | Managed PostgreSQL (Neon), managed storage (Supabase), single small container (Render), free-tier AI and geolocation providers |

## 4. Scope at a glance

**In scope (this repository)**

* Identity: registration is admin-driven; sign-in by password, OTP, Google SSO, or emailed
  one-click link; password reset; MFA flag; single-session option; admin impersonation.
* People: users, relationships, genders, birthdays, profile pictures, wish preferences, UI theme.
* Dates: events (anniversaries, engagements, other seeded types), festivals from Calendarific,
  calendar aggregation.
* Greetings: AI text + image generation, uploaded media fallback, custom wishes links, signature,
  email templating, delivery logging, retry, per-type status views.
* Social: circles (trusted contacts), chat (1:1 with attachments, voice notes, replies, reactions),
  presence, unread-mail digest, notifications.
* Insight: mail and Instagram dashboards, charts, login-location map, failed-login analytics with IP
  geolocation.
* Operations: scheduler management, health, OpenAPI, seed-data administration.

**Out of scope**

* The Angular UI (separate repository/deployment).
* Native mobile applications.
* Public self-registration and public social features.
* Payments, subscriptions, billing.
* Multi-tenant isolation (one deployment serves one family group).

## 5. High-level functionality

```
Admin ──► users, events, festival mappings, wish images, seed data, notifications, dashboards
User  ──► own profile, wish settings, calendar, circles, chat, theme
Scheduler ──► good morning · birthdays · events · festival wishes · good night
           └─► AI text (Gemini) + image (uploaded or Pollinations) ──► Gmail/SMTP ──► email log
```

| Capability | Summary |
| --- | --- |
| Authentication | JWT access token (15 min) + refresh token (7 days); refresh tokens stored and revocable; `sid` claim enables single-session enforcement; OTP (6 digits, 5 min); password reset (30 min); Google ID-token SSO; single-use direct-login tokens for emailed links |
| Users | Admin CRUD with role, gender, relationship, birthday, wish switches, MFA and concurrent-login flags; self-service profile picture, wish settings, theme |
| Events | Admin-managed per-user events with a seeded type, optional custom wishes page URL, optional uploaded greeting media |
| Festivals | Calendarific holidays for India synced monthly, filled on demand for other years, mapped to users for festival wishes |
| AI | Gemini text generation with automatic model fallback; Pollinations image generation; optional agentic composer that validates its own output |
| Email | Gmail API (OAuth refresh token) as the primary transport, SMTP and Brevo as alternatives; every send logged with type, status, retry count and image |
| Circles | Request/accept/reject/remove, daily reminders, one-click sign-in links, calendar sharing, chat gating |
| Chat | Conversations, attachments (any file up to 50 MB), voice metadata, replies, reactions, edits, deletes, presence heartbeat, STOMP realtime, nightly unread digest |
| Notifications | Admin announcements, publish/unpublish, optional email, scheduled windows, realtime push |
| Dashboards | Mail counters and charts, Instagram message counters, OTP/reset stats, login locations, failed-login ("violated users") analysis |

## 6. Users and roles

| Role | Description |
| --- | --- |
| `ROLE_ADMIN` | Manages users, events, festivals, mappings, wish images, notifications, dashboards; can impersonate a user; can trigger schedulers |
| `ROLE_USER` | Manages their own profile and settings, sees their calendar and circle, chats with circle members, reads published notifications |
| Scheduler / system | Internal actors: Quartz jobs and Spring `@Scheduled` tasks that send wishes and reminders |
| External providers | Gmail, Gemini, Pollinations, Calendarific, ip-api.com, Supabase Storage |

## 7. Technology summary

| Concern | Choice |
| --- | --- |
| Framework | Spring Boot 3.3.3 (Web, Security, Data JPA, Validation, Mail, Thymeleaf, Quartz, Cache, WebSocket) |
| Language | Java 17 |
| Persistence | PostgreSQL via Hibernate; `ddl-auto=update`; Ehcache second-level and query cache |
| Chat persistence | Separate PostgreSQL accessed through `NamedParameterJdbcTemplate` |
| Scheduling | Quartz JDBC job store (clustered) + Spring `@Scheduled` |
| Auth | JJWT 0.12.6 (HS256), BCrypt |
| AI | Google Gemini `generateContent`, model catalog for fallback; Pollinations for images |
| Storage | Supabase Storage over the S3 API (AWS SDK v2) |
| Docs | springdoc-openapi 2.6.0 (Swagger UI) |
| Build / deploy | Gradle, multi-stage Docker image, Render |

## 8. Related documents

* Requirements: [business-requirements.md](business-requirements.md),
  [functional-requirements.md](functional-requirements.md)
* Design: [../architecture/system-architecture.md](../architecture/system-architecture.md)
* API: [../backend/api-specification.md](../backend/api-specification.md)
* Operations: [../operations/operations-runbook.md](../operations/operations-runbook.md)
