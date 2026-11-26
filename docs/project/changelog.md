# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/) style, with semantic versions. Dates are
release dates.

> This file starts with the current state. Earlier history is in the git log; the repository was
> cloned shallow for documentation, so commit-level history before this entry is not reproduced here.

## [Unreleased]

### Added

- Documentation set under `docs/` (business, architecture, backend, testing, deployment, security,
  operations, project).

### Changed

- Nothing yet.

### Fixed

- Nothing yet.

### Security

- Planned: remove clear-text attempted passwords from `violated_users`.

## [1.0.0]

### Added

**Authentication**
- Password sign-in, OTP sign-in, Google SSO and emailed direct login.
- Refresh tokens with revocation; single-session option using the `sid` claim.
- Password reset by email and password change while signed in.
- Optional MFA gate; lockout after five failures with an owner email.
- Admin impersonation and switch-back.

**People**
- User CRUD, directory search with a reduced projection, wish settings, profile pictures, themes.

**Dates**
- Events with seeded types and recurrence; calendar aggregation of festivals, birthdays and events.
- Circle-member entries on the calendar with `fromCircle`.

**Festivals**
- Calendarific monthly sync, on-demand fetch for other years, per-year seeds.
- Festival wish mappings with bulk creation, personal notes and failure tracking.

**Wishes**
- Gemini text generation with model fallback and an optional agentic composer with a validator.
- Pollinations image generation, uploaded wish images with user/global priority, wishes links.
- Shared HTML email template with signature.

**Delivery**
- Gmail API transport with logging, retry (max 3), status APIs and sensitive-type protection.
- SMTP and Brevo transports available.

**Circles**
- Requests with accept/reject, one-click login links, daily reminders and acceptance emails.

**Chat**
- Conversations with text, attachments, replies, reactions, edits, deletes and presence.
- Circle-only messaging by default; nightly unread digest; STOMP realtime updates.

**Announcements**
- Create, schedule, publish, automatic unpublish, optional email fan-out and realtime push.

**Insight**
- Mail and Instagram dashboards and charts; OTP and reset statistics; login-location map;
  failed-sign-in analytics with geolocation.

**Agents**
- Wish composer, delivery-ops triage and a read-only admin copilot.

**Operations**
- Nine Quartz jobs and four Spring scheduled tasks; scheduler status and trigger API.
- `/health`, OpenAPI, Swagger UI, Docker image, `prod` profile, Render deployment.

### Changed

- Festival dates are stored per year; year-scoped entries are marked non-recurring on the calendar.

### Security

- BCrypt password hashing, rate limiting on auth paths, byte-sniffed uploads, admin-only sensitive
  email types, circle-gated chat.

### Known issues

See [../operations/known-issues.md](../operations/known-issues.md).

## Guidance

| Change type | Section |
| --- | --- |
| New feature | `Added` |
| Behaviour change | `Changed` |
| Bug fix | `Fixed` |
| Vulnerability fix, dependency CVE, permission change | `Security` |
| Removal | `Removed` |
| Deprecation | `Deprecated` |

Every user-visible change gets an entry under `Unreleased`. On release, move the entries under the
new version heading with the date.
