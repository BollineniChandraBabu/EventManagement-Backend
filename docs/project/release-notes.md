# Release notes

## 1.0.0 — current

The first documented release of the Golden Greetings backend.

### Highlights

**Identity**
- Password sign-in with lockout after five failures and an email to the account owner.
- One-time-code sign-in, Google SSO, and one-click direct login from circle-request emails.
- Password reset by emailed link, and password change while signed in.
- Optional MFA gate and optional single-session sign-in (`CONCURRENT_LOGIN` handling).
- Admin impersonation with switch-back.

**People and preferences**
- Admin-managed members with relationships, birthdays and wish preferences.
- Directory search with a reduced projection.
- Profile pictures (direct or presigned upload) and 11 UI themes.

**Dates**
- Per-user events with seeded types and yearly/one-time recurrence.
- Festival dates from Calendarific, synced monthly and fetched on demand for other years.
- A month calendar that merges festivals, birthdays, events and accepted circle members' dates.

**Wishes and delivery**
- AI-written greetings with model fallback, and generated or uploaded images.
- Optional personal wishes links and per-mapping notes.
- Gmail delivery with logging, retry (up to 3), per-type views and admin-only sensitive types.
- A shared email template with signature.
- Duplicate protection for birthday and festival wishes.

**Circles and chat**
- Circle requests with one-click acceptance, daily reminders and acceptance emails.
- Circle-gated chat with attachments, replies, reactions, edits, deletes, presence and unread counts.
- Nightly unread-message email digest.
- Realtime updates over STOMP.

**Announcements and insight**
- Admin announcements with scheduling, automatic unpublish, optional email fan-out and realtime push.
- Dashboards for mail and Instagram activity, OTP/reset statistics, a login-location map and
  failed-sign-in analytics with geolocation.

**Operations**
- Nine Quartz jobs plus four Spring scheduled tasks, with a status and trigger API.
- Health endpoint, OpenAPI documentation and Swagger UI.
- Docker image with a `prod` profile, deployed on Render.

**Agents**
- Wish composer with a validator, delivery-ops triage and a read-only admin copilot.

### Known issues

See [../operations/known-issues.md](../operations/known-issues.md). The most important:

1. `violated_users` stores attempted passwords in clear text.
2. Actuator endpoints and diagnostic OAuth endpoints are reachable without authentication.
3. The STOMP endpoint does not authenticate the sender.
4. Scheduler trigger has no admin check.
5. `GET /api/users/{id}` returns full profiles to any authenticated member.

### Upgrade notes

- No migrations are provided; the schema is created/updated by Hibernate at startup.
- All configuration is environment variables; see
  [../backend/configuration-reference.md](../backend/configuration-reference.md).

### Verification

- `./gradlew test` — 223 tests across 33 test classes.
- Smoke checks: `/health`, sign-in, calendar, one test email.

## Unreleased

### Planned

- Remove clear-text passwords from `violated_users`.
- Restrict actuator and diagnostic endpoints.
- Authenticate the STOMP handshake.
- Add a readiness endpoint and metrics.
- Add CI with tests and scanning.
- Baseline Flyway migrations.

### Fixed

- Nothing yet.

## Template for the next release

```markdown
## X.Y.Z — YYYY-MM-DD

### Highlights
### Added
### Changed
### Fixed
### Security
### Known issues
### Upgrade notes
### Verification
```
