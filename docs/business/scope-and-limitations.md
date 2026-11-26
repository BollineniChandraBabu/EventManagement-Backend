# Scope and limitations

## 1. In scope

| Area | Included |
| --- | --- |
| Identity | Admin-provisioned accounts; password, OTP, Google SSO, emailed direct login; password reset; MFA gate; single-session option; admin impersonation; lockout and rate limiting |
| People | Users, relationships, genders, birthdays, wish preferences, profile pictures, per-user theme |
| Dates | Per-user events with seeded types and recurrence; Calendarific festivals per year; aggregated calendar |
| Greetings | Gemini text with model fallback; Pollinations images; uploaded wish images; wishes links; personal notes; signature; templated HTML email |
| Delivery | Gmail API (primary), SMTP and Brevo services present; logging, status APIs, retry, per-type views; delivery-ops agent |
| Social | Circles with requests, reminders and one-click sign-in; 1:1 chat with attachments, replies, reactions, edits, deletes, presence; unread digest |
| Communication | Admin notifications with scheduling and optional email; realtime push |
| Insight | Mail and Instagram dashboards and charts; OTP/reset statistics; login-location map; failed-login analytics with geolocation |
| Administration | Seed data CRUD (event types, relationships, template types, special events), wish-image CRUD, festival mappings, scheduler status/trigger |
| Platform | Health endpoint, OpenAPI/Swagger, Docker image, `prod` profile, Render deployment |

## 2. Out of scope

| Area | Reason / note |
| --- | --- |
| Angular UI | Separate repository and Firebase deployment; this repo exposes only the API |
| Public registration | Accounts are created by an administrator |
| Multi-tenancy | One deployment serves one family group; there is no tenant column |
| Payments / billing | Not part of the product |
| Native mobile apps | The web UI is responsive; no native client is provided |
| SMS / WhatsApp / push notifications | Only email and WebSocket push exist |
| Calendar export / sync (ICS, Google Calendar) | Not implemented |
| File virus scanning | Media is type-checked but not malware-scanned |
| Localisation | Emails are generated in English (`language=EN`); no i18n resource bundles |
| Analytics warehouse / BI export | Only the built-in dashboard endpoints |
| Hard delete of personal data through the API | Users are soft-deleted; see §4 |
| Automated DB migrations | Schema is applied by Hibernate `ddl-auto=update` |

## 3. Known limitations

| # | Limitation | Impact | Workaround |
| --- | --- | --- | --- |
| L1 | Rate limiting is in-memory and per instance | With several instances, each allows its own budget | Keep one instance, or move to a shared store |
| L2 | Caches (`seedData`, `festivalsByMonth`, `dashboardSummary`, `ipInfo`, `emailImages`) are local Ehcache | Values are not shared between instances; each instance warms independently | Acceptable at family scale |
| L3 | `spring.jpa.hibernate.ddl-auto=update` | No versioned migrations, no down-migrations, risky column changes | Review schema diffs before deploy; back up first |
| L4 | Hibernate `update` cannot drop or narrow columns | Removed fields linger in the database | Drop manually with a reviewed script |
| L5 | Quartz schema is initialised on every start (`initialize-schema=always`) | Harmless but not friendly to strict DBAs | Set to `never` once the schema exists |
| L6 | `violated_users` stores the attempted password in clear text | Serious privacy exposure if the table is read | Restrict access; see [../security/threat-model.md](../security/threat-model.md) T-07 |
| L7 | The STOMP endpoint does not authenticate the sender | A crafted frame can claim another `senderId` | Restrict `/ws-chat` at the network layer; see T-05 |
| L8 | `/actuator/**` is permitted and `management.endpoints.web.exposure.include=*` | Actuator endpoints are reachable without a token | Restrict or disable in production |
| L9 | `/api/notifications/**` is in the permit-all list | Relies on `@PreAuthorize` for admin methods; a new unannotated method would be public | Keep method security on every new endpoint |
| L10 | `/google/**`, `/gmail/**` and `/oauth2callback` are public | OAuth helper endpoints can be called by anyone; `/gmail/send` accepts an access token as a parameter | Remove or protect before wider exposure |
| L11 | The Instagram webhook prints payloads to stdout and does not persist them | No audit trail; verbose logs | Add structured logging if Instagram is used |
| L12 | Email type is inferred from keywords when not passed explicitly | Misclassification is possible for unusual subjects | Pass `EmailType` from the caller (the wish and circle flows do) |
| L13 | Non-recurring events still appear in month queries | A past one-off event may show in that month | UI filters on year/date |
| L14 | `GET /api/users/{id}` is available to any authenticated user | Any member can read another member's full profile including role and settings | Restrict to admin/self |
| L15 | The calendar returns all festivals of the month without paging | Larger month payloads | Bounded by month size |
| L16 | Non-agentic AI output is only shape-validated (JSON with two keys) | A malformed but parseable message is possible | Enable `AGENT_WISH_ENABLED` for validator-backed output |
| L17 | Image generation is best-effort | Some emails arrive without an image | Upload wish images for important occasions |
| L18 | The Gmail OAuth refresh token can be revoked | Email sending stops with `invalid_grant` | Re-authorise and update `GMAIL_REFRESH_TOKEN` |
| L19 | Render free tier sleeps on inactivity | The first request after idle is slow | Use a paid instance or a keep-alive monitor |
| L20 | Emails are not checked for deliverability | Bounces are only visible if the API reports a failure | Monitor `email_logs` failures |
| L21 | No user-facing deletion of chat history | Messages persist until deleted individually or by an operator | Manual database cleanup |
| L22 | Most cron times are fixed in `WishesSchedulerConfig` | Changing a time needs a code change and redeploy | Only the unread-mail and reminder crons are configurable |

## 4. Data retention limitations

* Soft delete (`users.deleted=true`) hides a user but keeps their rows, email logs and chat messages.
* `violated_users` and `login_location_events` grow without automatic pruning.
* `email_logs` keeps bodies and image keys indefinitely unless deleted through the admin API.
* Refresh tokens are pruned daily; OTP, reset and login tokens are not pruned automatically.

## 5. Compatibility

| Item | Supported |
| --- | --- |
| Java | 17 (build toolchain); runtime image is `eclipse-temurin:17-jre-alpine` |
| PostgreSQL | Any version supported by the PostgreSQL JDBC driver in the Spring Boot 3.3.3 BOM |
| Browsers | Any modern browser (API only); email clients that render inline CSS and `cid:` images |
| Client contract | JSON over HTTPS with `Authorization: Bearer <token>`; multipart for uploads; STOMP for chat |

## 6. Deferred work

1. Replace `ddl-auto=update` with Flyway or Liquibase.
2. Remove the clear-text password from `violated_users`.
3. Authenticate the STOMP handshake and derive the sender from the principal.
4. Restrict `/actuator`, `/gmail/**`, `/google/**` and the notification permit-all rule.
5. Shared rate-limit and cache store for multi-instance deployments.
6. Retention jobs for security and email tables.
7. Delivery webhooks or bounce processing for email.
