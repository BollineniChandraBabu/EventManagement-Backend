# Functional requirements

Requirement IDs are stable and referenced from [acceptance-criteria.md](acceptance-criteria.md) and
[../testing/test-cases.md](../testing/test-cases.md). "Admin" means a principal with
`ROLE_ADMIN`; "user" means any authenticated principal unless stated otherwise.

## 1. Authentication and account access

| ID | Requirement |
| --- | --- |
| FR-AUTH-01 | The system shall authenticate a user with email and password and return an access token (15 minutes), a refresh token (7 days), the role, the access-token lifetime and the unread chat count. |
| FR-AUTH-02 | The system shall reject a sign-in for an unknown email with `404` and record the attempt in `violated_users` with the email, the supplied password, the reported location, the client IP and coordinates. |
| FR-AUTH-03 | The system shall increment `users.failed_login_attempts` on a bad password and reset it to zero on a successful sign-in. |
| FR-AUTH-04 | At 5 failed attempts the system shall refuse further password sign-ins with "Too many failed login attempts. Please contact the Administrator." and email the account owner an "Account Security Alert". |
| FR-AUTH-05 | When an unknown email has been attempted at least 5 times, the system shall email the administrator an "Unknown Login Attempt Detected" alert including the email, IP and time. |
| FR-AUTH-06 | The system shall record every successful sign-in in `login_location_events` with the location, IP, latitude, longitude and IST timestamp. |
| FR-AUTH-07 | When `mfa_enabled` is true the system shall not issue tokens from a password or SSO sign-in; it shall return `400` with the message `MFA_OTP_REQUIRED` so the client continues with OTP verification. |
| FR-AUTH-08 | The system shall issue a 6-digit OTP valid for `app.otp.ttl-minutes` (default 5) and shall reuse the still-valid code on resend, allowing at most `app.otp.max-resend-attempts` (default 5) resends. |
| FR-AUTH-09 | The system shall verify an OTP only when it is unused, unexpired and matches; a successful verification marks it used and issues tokens. |
| FR-AUTH-10 | The system shall email a password-reset link containing a UUID token valid for `app.password-reset.ttl-minutes` (default 30), reuse the still-valid token on repeat requests, and cap repeats at `app.password-reset.max-attempts` (default 5). |
| FR-AUTH-11 | The system shall reset a password given a valid, unused, unexpired token (optionally with the matching email) and invalidate all outstanding reset tokens for that user. |
| FR-AUTH-12 | The system shall allow a signed-in user to change their password after verifying the current password and rejecting an unchanged new password. |
| FR-AUTH-13 | The system shall authenticate a Google ID token, require a verified email, and issue tokens only for an existing active account. |
| FR-AUTH-14 | The system shall issue a single-use direct-login token (`login_tokens`, TTL `app.circle.request.login-ttl-minutes`, default 60) for emailed links and consume it on first use, rejecting a mismatched email. |
| FR-AUTH-15 | The system shall allow an admin to sign in as a non-admin user, stamping `impersonatedBy` in the token, and allow that session to switch back to the admin. |
| FR-AUTH-16 | When `users.concurrent_login_allowed` is false, a new sign-in shall revoke the user's earlier refresh tokens and rotate `active_session_id`; requests bearing a stale `sid` shall be rejected with `401` and a message beginning `CONCURRENT_LOGIN:`. |
| FR-AUTH-17 | The system shall rate-limit the auth endpoints listed in `RateLimitFilter` to `app.rate-limit.max-requests` per `app.rate-limit.window-seconds` per client IP and per path, returning `429`. |
| FR-AUTH-18 | The system shall expose the configured Google SSO client id at `GET /api/auth/sso/token`. |

## 2. Users

| ID | Requirement |
| --- | --- |
| FR-USER-01 | An admin shall create a user with name, email, role, gender, date of birth, relationship code, three wish switches, optional birthday wishes URL, MFA flag and concurrent-login flag. |
| FR-USER-02 | A newly created user shall receive the password `Test` (BCrypt-hashed) and must change it; the API never accepts a password on create. |
| FR-USER-03 | An admin shall update a user by email, and list users with search, role, gender, relationship and active filters, paged and sorted. |
| FR-USER-04 | Any authenticated user shall read a small public directory (`GET /api/users/directory`) containing only id, name, email, relationship, presence and profile picture. |
| FR-USER-05 | A user shall read and update their own wish switches, MFA flag and concurrent-login flag. |
| FR-USER-06 | A user shall read their own record (`GET /api/users/me`) and preview their birthday wish (`GET /api/users/me/wish-preview`). |
| FR-USER-07 | A user shall upload a profile picture (image-only, ≤5 MB) either directly or via a presigned upload URL, and remove it. |
| FR-USER-08 | An admin (or the user themselves) shall change the concurrent-login flag of a specific user. |
| FR-USER-09 | An admin shall deactivate a user (soft delete: `deleted=true`), deactivate several at once, or toggle `active`. |
| FR-USER-10 | The system shall reject a birthday wishes URL that is not an absolute `http`/`https` URL of at most 500 characters, has whitespace, or carries user-info. |

## 3. Events

| ID | Requirement |
| --- | --- |
| FR-EVT-01 | An admin shall create, update and delete an event for a user with a seeded event type, an optional date, a recurring flag, an optional wishes URL and optional media. |
| FR-EVT-02 | The event type shall resolve against `event_type_seeds` by upper-cased code and must be active; an unknown type yields `400 Invalid event type`. |
| FR-EVT-03 | A non-admin shall list only their own events and read only their own event; a request for someone else's event returns `404` (existence is not revealed). |
| FR-EVT-04 | An admin shall list all events with search, type, user and active filters, paged and sorted. |
| FR-EVT-05 | Replacing an event's image shall delete the previous stored object. |
| FR-EVT-06 | Deleting an event shall delete its stored image object. |
| FR-EVT-07 | An event with `recurring=true` shall fire every year on its month/day; a non-recurring event shall fire only when the date matches exactly. |

## 4. Festivals and festival wish mappings

| ID | Requirement |
| --- | --- |
| FR-FEST-01 | The system shall sync Calendarific holidays for the current year monthly (cron `0 0 2 1 * ?`, Asia/Kolkata) and upsert them into `seed_special_events` keyed by name and year. |
| FR-FEST-02 | The system shall mark current-year festival rows that disappear from a sync as inactive. |
| FR-FEST-03 | The system shall fetch and store a requested year's festivals on demand the first time a calendar or festival request asks for that year, and serve later requests from the database. |
| FR-FEST-04 | The system shall list festivals by month (optionally by year) with paging, search and sorting. |
| FR-FEST-05 | An admin shall map a festival to a user (upsert by festival+user), map one festival to many users in bulk, list mappings with status filters, view summary counters and delete mappings. |
| FR-FEST-06 | A mapping shall support an optional personal note (≤500 characters) and an optional wishes URL, both appended to the festival email. |
| FR-FEST-07 | A mapping whose send failed shall keep `isError=true` and be retried on later runs that day; a success shall clear the flag. |
| FR-FEST-08 | A user shall list their own festival mappings (`GET /api/festival-wish-mappings/me`). |

## 5. Wish generation and email delivery

| ID | Requirement |
| --- | --- |
| FR-WISH-01 | The system shall generate a wish as JSON with `subject` and `htmlMessage` from recipient name, relationship, event, festival, tone and language. |
| FR-WISH-02 | The system shall call Gemini through `GeminiClient`, which retries the next available model on 429, 404, 5xx or network error, skips a failed model for `app.gemini.fallback.cooldown-seconds`, and returns `503` when no model answers. |
| FR-WISH-03 | The system shall discover usable text models from Google's model list, cache it for `app.gemini.fallback.model-cache-minutes`, and exclude models matching `app.gemini.fallback.excluded-model-pattern`. |
| FR-WISH-04 | The system shall select a greeting image in this order: an uploaded image for that person and event type, an uploaded image for everyone for that event type, for festivals the same two steps for `Festival`, otherwise a generated image. |
| FR-WISH-05 | Event-type matching shall be case-insensitive and shall also match a space-separated form (for example `WEDDING_ANNIVERSARY` matches `Wedding Anniversary`). |
| FR-WISH-06 | When image generation fails the system shall still send the email, without an inline image. |
| FR-WISH-07 | The system shall append a "wishes page" button and a plain-text copy of the link when a user's birthday or an event carries a wishes URL, and append a personal note for festival mappings. |
| FR-WISH-08 | The system shall HTML-escape all user-supplied text placed into an email. |
| FR-WISH-09 | The system shall render every email through the shared template with the configured signature image (inline when available). |
| FR-WISH-10 | The system shall log every email attempt in `email_logs` with recipient, subject, body, status, type, retry count, error message, sent time and image key. |
| FR-WISH-11 | The system shall classify the email type from an explicit type, else from the subject/body keywords. |
| FR-WISH-12 | The system shall retry failed emails with `retryCount < 3`. |
| FR-WISH-13 | When `app.agent.wish.enabled` is true the system shall attempt the agentic composer (profile + recent wishes + `submit_wish` validation) and fall back to the single-prompt generator on any failure. |
| FR-WISH-14 | An admin shall send an ad-hoc wish email for a user, subject, body and event-type code (`POST /api/email/send-now`). |

## 6. Calendar

| ID | Requirement |
| --- | --- |
| FR-CAL-01 | The system shall return a month's festivals, birthdays and events for the signed-in user, optionally scoped to a year. |
| FR-CAL-02 | A regular user's calendar shall also include the birthdays and events of their accepted circle members. |
| FR-CAL-03 | An admin's calendar shall include everybody's entries. |
| FR-CAL-04 | Circle entries shall carry `ownerUserId` and `fromCircle=true`; the user's own entries carry `fromCircle=false`. |
| FR-CAL-05 | Duplicate entries (same id) shall be collapsed so a circle member's entry never appears twice. |
| FR-CAL-06 | A year-scoped festival entry shall keep that year's date and be marked `recurring=false`; birthdays shall always be `recurring=true`. |

## 7. Circles

| ID | Requirement |
| --- | --- |
| FR-CIR-01 | A user shall send a circle request to another active, non-deleted user, but not to themselves. |
| FR-CIR-02 | Sending a request to a user who already sent one shall accept it instead. |
| FR-CIR-03 | A duplicate request shall be rejected; a previously rejected pair may be re-requested and the row is reused with the direction flipped. |
| FR-CIR-04 | Only the addressee shall accept or reject a pending request; a handled request cannot be handled again. |
| FR-CIR-05 | Either participant shall remove an accepted circle or a request. |
| FR-CIR-06 | The system shall report the relationship status with a given user (`inCircle`, status, circle id). |
| FR-CIR-07 | A new request shall email the addressee with a one-click direct-login link; a mail failure shall never block the request. |
| FR-CIR-08 | Accepting a request shall email the requester a confirmation. |
| FR-CIR-09 | A request pending longer than `app.circle.request.reminder-after-days` (default 7) shall be reminded daily at `app.circle.request.reminder-cron` (default 09:00), once per day, until handled. |
| FR-CIR-10 | Accepted circle membership shall unlock calendar sharing and (by default) chat between the two users. |

## 8. Chat

| ID | Requirement |
| --- | --- |
| FR-CHAT-01 | When `app.chat.circles-only` is true, a user shall message only users with whom they have an accepted circle; otherwise messaging shall be refused with `400`. |
| FR-CHAT-02 | A message shall carry text, an encrypted payload, an attachment, or a combination; an empty message shall be rejected. |
| FR-CHAT-03 | Attachments shall accept any file up to 50 MB, with the media type detected from the bytes, and be stored in object storage. |
| FR-CHAT-04 | The system shall support replies, reactions (emoji, per user), likes, edits, deletion of one message, of several, or of the last sent message. |
| FR-CHAT-05 | The system shall mark a conversation seen for the receiver and expose unread counts per conversation and in total. |
| FR-CHAT-06 | The system shall track presence with a heartbeat, mark users offline on request, and treat a user unseen for more than 2 minutes as offline. |
| FR-CHAT-07 | The system shall publish messages, presence, seen and typing events over STOMP topics `/topic/chat/{conversationId}`. |
| FR-CHAT-08 | The system shall email each user with unread messages a digest at `app.chat.unread-mail.cron` (default 02:00 daily). |
| FR-CHAT-09 | A user shall list chat candidates, active users, conversations and all messages (with search), paged. |

## 9. Notifications

| ID | Requirement |
| --- | --- |
| FR-NOTIF-01 | An admin shall create, update, delete (single or bulk), list and read notifications with a title (≤150 characters), message, email flag and optional schedule window. |
| FR-NOTIF-02 | Publishing shall unpublish the previously published notification, stamp `publishedAt` (from `scheduledFrom` when set), and reject a window that has already passed. |
| FR-NOTIF-03 | When the publish time is in the future the system shall schedule publication and, if `scheduledTo` is set, scheduled unpublication. |
| FR-NOTIF-04 | When `canSendEmail` is true the system shall email the notification to all active users. |
| FR-NOTIF-05 | Any caller shall read the currently published notification at `GET /api/notifications/published`. |

## 10. Themes

| ID | Requirement |
| --- | --- |
| FR-THEME-01 | The system shall seed the theme styles at startup into `theme_seeds` (code + display name). |
| FR-THEME-02 | The system shall list active themes, return the signed-in user's theme (defaulting to `default`), and save a chosen theme, rejecting an unknown code with `400`. |
| FR-THEME-03 | The system shall keep the seeded theme list in step with the `UITheme` enum without duplicating rows. |

## 11. Media and storage

| ID | Requirement |
| --- | --- |
| FR-MEDIA-01 | The system shall detect media type from the first bytes and accept only PNG, JPEG, GIF, WebP, BMP (5 MB), MP4/WebM/MOV/AVI/MKV/MPEG/3GP (50 MB), MP3/WAV/OGG/M4A/FLAC (10 MB) and PDF (20 MB). |
| FR-MEDIA-02 | The system shall reject a file whose declared content type disagrees with its bytes. |
| FR-MEDIA-03 | The system shall gzip stored media (labelled `Content-Encoding: gzip`) when `app.media.gzip.enabled` is true and expand it on read. |
| FR-MEDIA-04 | The system shall serve stored media through signed URLs and keep the detected content type and a matching extension. |
| FR-MEDIA-05 | Profile pictures shall remain image-only. |

## 12. Admin content (seed data and wish images)

| ID | Requirement |
| --- | --- |
| FR-SEED-01 | An admin shall manage event types, relationships and template types (create, update by code, list with search, read, delete single/bulk) and read filter values. |
| FR-SEED-02 | An admin shall manage special-event seeds (festival templates) with name, date, message and active flag, including a "today" view. |
| FR-SEED-03 | An admin shall upload, replace and delete wish images for a user or for everyone, per event type, with an active flag. |
| FR-SEED-04 | Deleting a seed or image shall remove the associated stored object where one exists. |

## 13. Dashboards and analytics

| ID | Requirement |
| --- | --- |
| FR-DASH-01 | The system shall return mail counters (total users, upcoming events, emails sent today, failed emails) scoped to the caller (all users for an admin) and cache them for 60 seconds. |
| FR-DASH-02 | The system shall return Instagram message counters and charts. |
| FR-DASH-03 | The system shall return mail charts over a validated date range (`yyyy-MM-dd`, end not in the future, start before end). |
| FR-DASH-04 | An admin shall read OTP and password-reset mail dashboards and charts. |
| FR-DASH-05 | The system shall return login-location points for the caller, or for a chosen user when the caller is an admin, capped at 1000 points and flagged `truncated` when capped. |
| FR-DASH-06 | An admin shall read the failed-login ("violated users") dashboard with totals, a daily chart, map points (newest 300), paged rows, unique IP/email counts, top IPs and top emails, and IP geolocation enrichment. |
| FR-DASH-07 | The system shall not look up IP geolocation more than once per IP per 24 hours (cache) and shall not cache failures. |

## 14. Administration and operations

| ID | Requirement |
| --- | --- |
| FR-OPS-01 | The system shall expose `GET /health` returning `status=UP` and the current IST time, without authentication. |
| FR-OPS-02 | The system shall publish OpenAPI at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`. |
| FR-OPS-03 | The system shall list schedulers with run counters, last status/duration/error and next/previous fire times, and allow an admin to trigger a job by name. |
| FR-OPS-04 | The system shall expose the AI agent switches and the current failed-email count at `GET /api/agent/status`. |
| FR-OPS-05 | An admin shall ask the read-only copilot natural-language questions and run the delivery-ops triage manually. |

## 15. Cross-cutting rules

| ID | Requirement |
| --- | --- |
| FR-X-01 | All timestamps written by entities shall use `Asia/Kolkata`. |
| FR-X-02 | All list endpoints shall return the shared `PagedResponse` shape (`content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`). |
| FR-X-03 | Errors shall return `{timestamp, status, message}` with a suitable HTTP status. |
| FR-X-04 | Unknown or unauthorised access to another user's resource shall return `404` rather than `403` where existence must not be disclosed. |

## 16. Related documents

* [../backend/business-logic.md](../backend/business-logic.md) — how each rule is implemented
* [../backend/api-specification.md](../backend/api-specification.md) — endpoints and payloads
* [acceptance-criteria.md](acceptance-criteria.md)
