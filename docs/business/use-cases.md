# Use cases

Notation: **Pre** = preconditions, **Post** = postconditions, **Alt** = alternate/exception flow.
All dates and times are Asia/Kolkata unless stated.

## Actors

| Actor | Description |
| --- | --- |
| Visitor | Unauthenticated caller |
| Member (`ROLE_USER`) | Signed-in family member |
| Admin (`ROLE_ADMIN`) | Signed-in administrator |
| Scheduler | Quartz or Spring scheduled task |
| Gmail / SMTP | Email transport |
| Gemini / Pollinations | AI providers |
| Calendarific | Festival provider |
| ip-api.com | Geolocation provider |
| Supabase Storage | Object storage |

---

## UC-01 Sign in with password

**Actor:** Visitor · **Goal:** obtain tokens

**Pre:** an active, non-deleted account exists.

**Main flow**
1. Visitor posts `POST /api/auth/login` with email, password and optional location data.
2. System loads the user by email (`deleted=false`). If absent → **Alt A**.
3. System rejects the attempt if `failed_login_attempts >= 5` → **Alt B**.
4. System authenticates the password. On failure → **Alt C**.
5. System resets `failed_login_attempts` to 0.
6. If `mfa_enabled` → **Alt D**.
7. System rotates `active_session_id`; if concurrent login is disallowed, revokes earlier refresh
   tokens.
8. System issues access (15 min) and refresh (7 days) tokens, stores the refresh token, records the
   login location, and returns `AuthResponse` with the unread chat count.

**Post:** tokens returned; `login_location_events` row added.

**Alt A:** `404` "User not found"; the attempt is written to `violated_users`; if that email has ≥5
recorded attempts the admin receives an "Unknown Login Attempt Detected" email.

**Alt B:** `400` "Too many failed login attempts. Please contact the Administrator."

**Alt C:** attempt counter incremented; at 5 the owner is emailed and the `400` lockout message is
returned, otherwise `400` "Invalid email or password".

**Alt D:** `400` with `MFA_OTP_REQUIRED`; the client continues with UC-02.

---

## UC-02 Sign in with OTP

**Actor:** Visitor · **Goal:** sign in without a password

**Pre:** active account.

**Main flow**
1. `POST /api/auth/otp/send` with email.
2. System reuses the still-valid unused code or creates a 6-digit code valid for 5 minutes.
3. System emails the code.
4. Visitor posts `POST /api/auth/otp/verify-login` with email, code and optional location.
5. System validates (unused, unexpired, matching), marks it used, applies single-session rules,
   issues tokens and records the login location.

**Alt:** resend limit (5) reached → `400` "OTP resend limit reached…"; wrong/expired code → `400`
"Invalid or expired OTP"; unknown or inactive user → `404`.

---

## UC-03 Reset a forgotten password

**Actor:** Visitor · **Goal:** regain access

**Pre:** active account with a known email.

**Main flow**
1. `POST /api/auth/forgot-password` with email.
2. System reuses the valid unused reset token or creates a UUID token valid for 30 minutes.
3. System emails the reset link (`{app.password-reset.ui-url}?token=…&email=…`).
4. Visitor posts `POST /api/auth/password-reset/confirm` with email, token and new password.
5. System validates the token/email pair, stores the BCrypt hash, and invalidates all reset tokens
   for the user.

**Alt:** repeat requests beyond 5 → `400` "Reset link request limit reached…"; invalid/expired token
→ `400` "Invalid token" / "Expired token".

---

## UC-04 Create a family member

**Actor:** Admin · **Goal:** add a person who will receive wishes

**Pre:** admin token.

**Main flow**
1. Admin posts `POST /api/users` with name, email, role, gender, date of birth, relationship code,
   three wish switches, optional birthday wishes URL, MFA and concurrent-login flags.
2. System validates the relationship against `relationship_seeds`, validates the wishes URL, encodes
   the initial password `Test`, creates the user with `active=true`, `deleted=false` and the wish
   settings row.
3. System returns the created `UserResponse`.

**Alt:** unknown relationship → `404`/`400`; invalid wishes URL → `400` with the reason; duplicate
email → `500` from the unique constraint (see [../operations/known-issues.md](../operations/known-issues.md)).

---

## UC-05 Record an event

**Actor:** Admin · **Goal:** a date that produces a yearly wish

**Pre:** the user and an active event type exist.

**Main flow**
1. Admin posts `POST /api/events` (JSON) or `/with-image` (multipart) with type, date, recurring
   flag, user and optional wishes URL/media.
2. System resolves the event type by upper-cased code and active flag.
3. System validates media (byte sniffing + limits) and uploads it, or leaves it null.
4. System saves the event and returns it with a signed image URL.

**Alt:** unknown type → `400` "Invalid event type"; invalid media → `400` with the media message;
upload failure → `400` "Unable to upload image to storage".

**Post:** the event is included in the calendar and in the daily wish job.

---

## UC-06 Daily event wishes (scheduler)

**Actor:** Scheduler (`dailyWishJob`, 06:10 IST)

**Pre:** events exist for today's month/day.

**Main flow**
1. Job loads today's events.
2. Keeps events that are recurring or whose date is exactly today.
3. For each event: build the AI request → generate text → choose an uploaded image or generate one
   → append the wishes link when present → send via Gmail with the signature.
4. After the loop, retry failed emails with fewer than 3 attempts.

**Alt:** one event fails → logged, other events continue; AI unavailable → text generation throws and
that event is skipped (image failure alone still sends).

---

## UC-07 Birthday wishes (scheduler)

**Actor:** Scheduler (`birthdayJob`, 06:30 IST)

**Pre:** users have a birthday today and `birthdayEnabled=true`.

**Main flow**
1. Job selects users whose birthday month/day is today.
2. Skips users whose wish switch is off or who already received a wish today
   (`last_birthday_wish_sent`).
3. Generates text and image, appends the personal wishes link when present, sends the email.
4. Stamps `last_birthday_wish_sent`.

**Alt:** failure → error email to the alert address; the date stamp is not written, so a later run
may retry.

---

## UC-08 Sync festivals

**Actor:** Scheduler (`festivalMonthlySyncScheduler`, 1st of month 02:00 IST) or an on-demand
calendar request for another year.

**Pre:** `calendarific.api.key` is set.

**Main flow**
1. Fetch holidays for the year from Calendarific (country `IN`).
2. For each holiday with a date: upsert `seed_special_events` by name and year, defaulting the
   message when empty, marking it active.
3. Mark current-year rows not present in the response inactive.
4. Evict the `festivalsByMonth` cache.

**Alt:** missing API key → warning and no change; provider error → logged, existing data kept;
on-demand fetch failure → the year stays empty without breaking the calendar.

---

## UC-09 Map a festival to members

**Actor:** Admin

**Main flow**
1. Admin posts `POST /api/festival-wish-mappings` (single) or `/bulk` (one festival, many users)
   with active flag, optional note and optional wishes URL.
2. System upserts by (festival, user), keeping the unique constraint.
3. System returns the mapping or counts (`created`, `updated`).

**Alt:** unknown festival/user → `404`; a repeated single upsert updates instead of duplicating.

---

## UC-10 View the calendar

**Actor:** Member or Admin

**Main flow**
1. `GET /api/calendar?month=11&year=2026` (both optional).
2. System resolves the caller, their accepted circle member ids, and the month's festivals.
3. Admin: all events and birthdays for the month. Member: own entries plus circle members' entries,
   de-duplicated by id.
4. System returns `CalendarResponse` entries with type (`Festival`/`Anniversary`/`Birthday`),
   `recurring`, `ownerUserId` and `fromCircle`.

**Alt:** a year other than the current one triggers a one-off festival fetch for that year.

---

## UC-11 Send a circle request

**Actor:** Member

**Pre:** the target is active and is not the caller.

**Main flow**
1. `POST /api/circles/requests` with `{userId}`.
2. System checks self-request and any existing circle:
   * accepted → `400` already in circle;
   * pending and the target is the requester → accept it instead;
   * pending otherwise → `400` already pending;
   * rejected → reuse the row, flip direction, set `PENDING`.
3. System saves the request, then (best effort) emails the addressee a one-click sign-in link and
   returns the request.

**Alt:** mail failure → logged, request still created; missing user id → `400`; unknown target →
`404`.

---

## UC-12 Accept a circle request and start chatting

**Actor:** Addressee, then both members

**Main flow**
1. Addressee opens the emailed link; the UI calls `POST /api/auth/direct-login` with token and email.
   The token is consumed and tokens are returned.
2. Addressee posts `POST /api/circles/requests/accept` with `{circleId}`.
3. System verifies the caller is the addressee and the request is pending, sets `ACCEPTED` and
   `respondedAt`, and emails the requester a confirmation.
4. Either member posts `POST /api/chat`; the circle check passes, the message is stored and
   published.

**Alt:** wrong caller → `400` "Only the addressee can respond…"; already handled → `400`; chat when
not in a circle (and circles-only is on) → `400` "You can only message users in your circle".

---

## UC-13 Publish an announcement

**Actor:** Admin

**Main flow**
1. Admin creates the notification (`POST /api/notifications`).
2. Admin publishes (`POST /api/notifications/{id}/publish`).
3. System validates that the schedule window is in the future, unpublishes the previous one, sets
   `publishedAt`, and either pushes immediately or schedules publication (and unpublication when
   `scheduledTo` is set).
4. If `canSendEmail`, the system emails all active users; the notification is pushed to the realtime
   topic.

**Alt:** past window → `400` and no side effects; unpublish → `POST /{id}/unpublish`.

---

## UC-14 Investigate failed logins

**Actor:** Admin

**Main flow**
1. `GET /api/dashboard/violated-users?startDate=…&endDate=…&page=0&size=50&searchKey=…`.
2. System validates the range (end ≤ today, start ≤ end) and the paging bounds.
3. System returns totals, a daily chart, the newest 300 map points, paged rows, unique IP/email
   counts and the top 5 IPs and emails; rows may be enriched with geolocation (cached 24 h).

**Alt:** invalid dates → `400` with a format message; geolocation failure → `status=fail` and not
cached.

---

## UC-15 Trigger a scheduler manually

**Actor:** Admin

**Main flow**
1. `GET /api/schedulers` to see names and health.
2. `POST /api/schedulers/{jobName}/trigger`.
3. Quartz fires the job immediately; `SchedulerTrackingService` records the run.

**Alt:** unknown job name → `404`; a job already running is shown as running until it completes.

---

## Use case → requirement map

| Use case | Requirements |
| --- | --- |
| UC-01, UC-02, UC-03 | FR-AUTH-01…11, FR-AUTH-17 |
| UC-04 | FR-USER-01…02, FR-USER-10 |
| UC-05 | FR-EVT-01…05 |
| UC-06, UC-07 | FR-WISH-01…13, FR-EVT-07 |
| UC-08, UC-09 | FR-FEST-01…07 |
| UC-10 | FR-CAL-01…06 |
| UC-11, UC-12 | FR-CIR-01…10, FR-CHAT-01 |
| UC-13 | FR-NOTIF-01…05 |
| UC-14 | FR-DASH-05…07 |
| UC-15 | FR-OPS-03 |
