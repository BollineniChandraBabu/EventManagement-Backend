# Business logic

How each rule from [../business/business-requirements.md](../business/business-requirements.md) is
implemented, with the exact conditions the code applies.

## 1. Authentication

### 1.1 Password sign-in

```
load user by email (deleted = false)        → not found ⇒ 404 + record violated_users
if failed_login_attempts >= 5               → 400 lockout message (no password check)
authenticate via DaoAuthenticationProvider
    bad credentials                          → attempts + 1; at 5 email owner + 400 lockout; else 400 invalid
reset failed_login_attempts to 0
if mfa_enabled                               → 400 MFA_OTP_REQUIRED
enforceSingleSession(user)
issue access + refresh, store refresh, record login location
```

`enforceSingleSession` always mints a new `active_session_id`; when
`concurrent_login_allowed == false` it also revokes every refresh token of that user.

Unknown-email attempts are recorded with the supplied password. When that email has **5 or more**
rows in `violated_users`, one "Unknown Login Attempt Detected" email goes to `alert.email.to`. This
check runs on every such failure, so an attacker can cause repeated alerts — a known risk.

### 1.2 OTP

* One active code per user: the newest unused, unexpired code is reused.
* A new code is `String.format("%06d", SecureRandom.nextInt(1_000_000))`.
* Expiry `now + app.otp.ttl-minutes` (5).
* Resends reuse the same code and increment `resend_count`; at `app.otp.max-resend-attempts` (5) the
  request is rejected.
* Verification requires `!used && expiresAt > now && code.equals(request.otp())`; on success `used`
  is set before tokens are issued.
* `verifyOtp` and `verifyLoginOtp` both apply single-session rules; only `verifyLoginOtp` records the
  login location.

### 1.3 Password reset

* Same reuse-and-cap pattern as OTP with `app.password-reset.ttl-minutes` (30) and
  `app.password-reset.max-attempts` (5).
* The emailed link is `{app.password-reset.ui-url}?token={uuid}&email={email}`.
* `resetPasswordWithEmail` requires the token to belong to the given email and to be unused.
* After a reset, **all** reset tokens for the user are invalidated.
* `changePassword` requires the current password, rejects a new password equal to the old one, and
  also invalidates reset tokens.

### 1.4 Google SSO

`GoogleIdTokenVerifier` with the configured audience verifies the signature, then the code requires
`emailVerified == true`, an existing non-deleted user and an active account. MFA and single-session
rules apply exactly as for password sign-in.

### 1.5 Direct login

`issueDirectLoginToken` returns an existing valid unused token if one exists, otherwise creates one
with `app.circle.request.login-ttl-minutes` (60). `directLogin` requires an unused, unexpired token;
when an email is supplied it must match the token's user (case-insensitive). The token is marked used
**before** tokens are issued, so a replay fails.

### 1.6 Impersonation

`adminLoginAsUser` requires `ROLE_ADMIN`, refuses an admin target and an inactive target, and stamps
`impersonatedBy` in both tokens. `switchBackToAdmin` requires that claim and re-issues an admin token
only if the original account is still an active admin.

## 2. Users

| Rule | Implementation |
| --- | --- |
| New users get a temporary password | `encoder.encode("Test")`; no password in the request |
| Relationship must exist | `relationshipSeedRepository.findByCode(...)`; missing → `404` |
| Wishes URL must be safe | `WishesUrls.normalizeOrNull` (scheme, host, length, no whitespace, no user-info) |
| Wish settings are created with the user | `UserWishSettings.builder().user(user)…` cascaded |
| Deactivation hides the account | `deleted = true`; all lookups use `findByEmailAndDeletedFalse` |
| Directory exposes little | `DirectoryUserResponse` has no role, birthday, MFA or settings |
| Concurrent-login change allowed for admin or self | `updateConcurrentLogin` checks `isAdminAuthenticated() || isSelf(target)` |
| Wish preview is generated, not stored | `getCurrentUserWishPreview` calls the AI service and returns text + image bytes |

## 3. Events

* **Type resolution:** `eventType.trim().toUpperCase()` looked up with `active = true`; failure →
  `400 Invalid event type`.
* **Ownership:** `getById(id, isAdmin, requesterEmail)` throws `404` when the caller is neither admin
  nor the owner, so existence is not disclosed.
* **Listing:** admins use `findAllByFilters`; others use `findAllByFiltersAndUser(userEmail, …)`.
* **Recurrence:** the scheduler keeps an event when `recurring` is true or `event_date` equals today.
* **Image lifecycle:** a replacement upload deletes the previous object; deleting the event deletes
  its object. A failed upload throws `400`.
* **Wishes link:** blank removes the link; an invalid link throws `400` with the reason.

## 4. Festivals

* **Sync:** for the current year, each holiday with a date is upserted by name within that year.
  A blank message becomes `Happy {name}! Wishing you joy and prosperity.`, and `active` is set true.
  After the upsert, current-year rows whose normalised name was not seen are marked inactive.
* **Name normalisation:** trimmed and lower-cased for comparison; duplicates within one response are
  skipped.
* **On-demand years:** `ensureFestivalsForYear(year)` returns immediately when any row exists in that
  year; otherwise it fetches once and stores. Missing API key or a provider error leaves the year
  empty and logs.
* **Caching:** `listByMonth` is cached under `festivalsByMonth` keyed by month (`all` when null);
  `syncCalendarificFestivals` evicts everything.
* **Mappings:** upsert by (`special_event_id`, `user_id`); bulk returns `created`/`updated` counts.
  `summary` counts total, active, inactive, upcoming (next 30 days) and failed.
* **Failure flag:** the scheduler sets `last_wish_sent_on = today` on every attempt and
  `is_error = !sent`; a mapping with `last_wish_sent_on == today && !is_error` is skipped, so a failed
  one is retried on later runs that day and a success stops further sends.

## 5. Wish generation

### 5.1 Text

`AiServiceImpl.generate` builds a prompt that includes the recipient name, the fixed sender name
(`Chandra`), relationship, festival, tone and language, and demands JSON only:

```json
{ "subject": "...", "htmlMessage": "..." }
```

The response is stripped of Markdown fences and parsed into `AiWishResponse`. A missing candidate
throws `BadRequestException("AI response missing")`.

With `app.agent.wish.enabled=true`, `AgenticAiService` first calls `WishComposerAgent`:

1. Tools `get_recipient_profile(userId)` and `get_recent_wishes(userId)` are offered when a user id is
   known.
2. The model must finish by calling `submit_wish(subject, htmlMessage)`.
3. The validator rejects placeholder patterns (`[Name]`, `{{…}}`, `{name}`, `<name>`, `%s`,
   `undefined`), empty values and a subject already used in the recipient's recent wishes.
4. A rejection is returned to the model as a tool error so it can correct itself; the loop is capped
   at `app.agent.wish.max-steps` (6).
5. Any exception or an unaccepted result falls back to the single-prompt generator.

### 5.2 Image selection

`AiServiceImpl.callGeminiImage` asks `WishImageService.findRandomImage(userId, keys)` where `keys`
come from `WishImageKeys.forWish(event, festival)`:

1. `event` (for example `Birthday`) and its space-separated variant.
2. For festivals: the festival name, its space-separated variant, then `Festival`.

`WishImageServiceImpl` tries, in order: active images for that user with a matching event type, then
active images with a null `user_id` and a matching event type; the first candidate that downloads
successfully is returned. Event types are compared case-insensitively after upper-casing and
replacing `_` with a space.

If no uploaded image is found, Pollinations is called:

```
GET {app.pollinations.image.url}{prompt}?model={app.pollinations.image.model}
Authorization: Bearer {app.pollinations.image.key}   (only when set)
```

Any error returns `null`; the email is still sent.

### 5.3 Email composition

1. The generated HTML body is passed to `WishEmailExtras.appendWishesLink` when the user/event/mapping
   has a wishes URL: a table-based button plus a plain-text copy of the link, inserted before
   `</body>` or appended.
2. Festival mappings additionally get `appendPersonalNote` (escaped, newlines → `<br>`).
3. `EmailTemplateBuilder.build(html, signatureSource)` wraps the body in the shared template and adds
   the signature image (`cid:emailSignature`) or its URL.
4. Images are attached inline as `cid:birthdayImage` when the bytes are an image; other media types
   are attached as files named `wish-media{ext}`.
5. Every attempt writes `email_logs`; on success `status=SENT` and `sent_at`; on failure
   `status=FAILED`, `retry_count + 1`, `error_message`.

## 6. Email classification and visibility

| Rule | Implementation |
| --- | --- |
| Explicit type wins | `sendEmailWithAttachments(..., EmailType)` sets it directly |
| Otherwise infer from text | `classifyEmailType` checks, in order: `otp`, `reset your`/`forgot password`/`password reset`, `good morning`, `good night`, `birthday`, `festival`, `unread`+`chat`, `circle`; default `EVENT` |
| Sensitive types | `OTP`, `FORGOT_PASSWORD`, `CIRCLE_REQUEST` — non-admins cannot read the body or image |
| Non-sensitive types | `GOOD_MORNING`, `GOOD_NIGHT`, `BIRTHDAY`, `EVENT` (plus `FESTIVAL_WISH` and `UNREAD_CHAT_MESSAGE` in status views) |
| Status list is filtered | Non-admin listings exclude sensitive types and are limited to the caller's own address |
| Image is loaded on demand | The list carries `hasImage`; the bytes come from `GET /status/{id}/image` with `Cache-Control: private, max-age=600` |
| Retry | `FAILED` rows with `retryCount < 3` are re-sent; the retry reuses the log id |

## 7. Calendar

* Festivals: `listByMonth(month)` or `listByMonthForYear(month, year)`; the latter ensures the year
  exists first.
* A year-scoped festival keeps that year's date and is `recurring = false`; without a year the
  festival repeats yearly (`recurring = true`).
* Events: mapped with their own `recurring` flag and the owner's name/id.
* Birthdays: always `recurring = true`.
* Circle sharing: a non-admin sees own entries plus accepted members' entries, merged by id with own
  entries winning; `fromCircle = true` marks an entry that belongs to someone in the caller's circle.
* Admins see everything; `fromCircle` still flags entries that are not their own.

## 8. Circles

| Situation | Result |
| --- | --- |
| Request to self | `400 You cannot send a circle request to yourself` |
| Target missing, deleted or inactive | `404 User not found` |
| Already accepted | `400 This user is already in your circle` |
| Pending, target is the requester | Accepted immediately (`acceptExisting`) |
| Pending, caller is the requester | `400 A circle request is already pending` |
| Previously rejected | The same row is reused, direction flipped to the caller, status `PENDING`, `respondedAt` cleared |
| Accept/reject by someone else | `400 Only the addressee can respond to this circle request` |
| Accept/reject a handled request | `400 This circle request has already been handled` |
| Remove by a non-participant | `404 Circle not found` |
| New request mail | Sent best effort; a failure is logged and never fails the API |
| Accepted mail | Sent best effort to the requester |
| Reminder | Pending and created before `now - reminder-after-days` and not reminded today → email + stamp `lastReminderSentOn` |

The one-click link is `<app.circle.request.login-url>?token=…&email=…`; when a token cannot be
issued, the plain circles URL is used instead.

## 9. Chat

* **Gate:** `circlesOnlyChat` (default true) requires an accepted circle between sender and receiver;
  otherwise `400 You can only message users in your circle`.
* **Content rule:** at least one of text, encrypted payload or attachment must be present.
* **Type:** `resolveMessageType` derives the type from the payload and attachment (text, voice,
  media), defaulting to `TEXT`.
* **Conversation:** `createConversation` is an upsert on the unordered pair; `resolveConversationId`
  reads it back.
* **Attachments:** `MediaValidation.validateAttachmentAndDetect` accepts any file up to 50 MB and
  returns the sniffed type; the object key, original name and type are stored with the message.
* **Seen:** `markConversationSeen` sets `seen_at` for messages where `receiver_id = viewer`;
  `markSeen=true` is the default on the conversation read.
* **Deletes:** deleting the last sent message, one message or a batch all verify participation first;
  deleting a message removes its reactions.
* **Edits:** only the sender can edit; only `message_text` changes.
* **Reactions:** unique per (message, user, emoji); `like`/`unlike` are convenience wrappers.
* **Presence:** `heartbeat` sets `online = true` and `last_seen_at = now`; `offline` clears it. A user
  counts as online only when seen within the last 2 minutes; `markStaleUsersOffline` runs on the user
  list queries.
* **Unread digest:** `findUnreadCountsByReceiver` groups unread messages by receiver; each user with
  at least one unread message gets one email at the configured cron.

## 10. Notifications

* A notification can be edited or deleted only while unpublished (`ensureNotPublished`).
* `publish` requires the schedule window to be valid **and** in the future; validation happens before
  the previously published notification is touched.
* Publishing unpublishes the previous one and cancels its scheduled tasks.
* `publishedAt` is `scheduledFrom` when set, otherwise now.
* A future publish schedules a realtime push (and a scheduled unpublish when `scheduledTo` is set);
  an immediate publish pushes and, when `canSendEmail`, emails every active user.
* `getPublished` returns the single published row.

## 11. Dashboards

| Rule | Implementation |
| --- | --- |
| Scope | Admins see all data; others see their own (email logs by recipient) |
| Summary cache | `getDashboard` cached for 60 s under `dashboardSummary` |
| Chart dates | `yyyy-MM-dd`; end ≤ today; start < end (login locations allow start = end) |
| Violated-users paging | `page ≥ 0`, `1 ≤ size ≤ 500`, `searchKey ≤ 100` characters |
| Map points | Newest 300 only |
| Login-location points | Newest 1000, `truncated = true` when capped |
| Top lists | Top 5 IPs and emails by attempts |
| IP enrichment | `IpInfoService.lookup` cached 24 h unless the status is not `success` |
| Sensitive dashboards | OTP and password-reset dashboards are admin-only |

## 12. Media rules

| Rule | Implementation |
| --- | --- |
| Real type decides | `MediaContentTypes.detect` on the first bytes |
| Declared type must agree | `MediaValidation.validateCommon` |
| Size limits | image 5 MB, audio 10 MB, PDF 20 MB, video 50 MB, chat attachment 50 MB |
| Image-only endpoints | `validateImage` (profile pictures, wish images) |
| Storage key | Generated as `{folder}/{date}/{uuid}{ext}` with the detected extension |
| Compression | `GzipUtil.compress` when `app.media.gzip.enabled`; object labelled `Content-Encoding: gzip` |
| Read | `GzipUtil.decompress` tolerates both compressed and uncompressed objects |

## 13. Time and duplicate protection

| Concern | Rule |
| --- | --- |
| Zone | Everything uses `Asia/Kolkata` (`scheduler.time-zone`) |
| Birthday | At most one wish per user per day (`last_birthday_wish_sent`) |
| Event | One send per event per scheduler run; recurring events fire yearly on month/day |
| Festival | One send per mapping per day unless the previous attempt failed |
| Reminder | One per circle per day (`lastReminderSentOn`) |
| Digest | One per receiver per run (the job runs once daily) |
| Tokens | Single use via `used`; refresh tokens revocable |

## 14. Edge cases and their behaviour

| Case | Behaviour |
| --- | --- |
| Event with a null date and `recurring=false` | Never fires (date comparison fails) |
| Festival with a null date | Skipped during sync |
| User with no wish settings | Birthday wish skipped (`getWishSettings() == null`) |
| Email send fails after the image upload | The log row is still saved as `FAILED` with the image key |
| Storage download fails during email send | `downloadImage` returns null; email sent without the image |
| Chat message with only whitespace | Rejected as empty |
| Chat attachment upload fails | Object key is null; message is still stored without an attachment (see known issues) |
| Notification scheduled in the past | `400`, no state change |
| Second notification published | The first is unpublished automatically |
| Circle re-request after rejection | Row reused, direction flipped |
| Concurrent agent run | `409 A triage run is already in progress.` |
| AI disabled for copilot | `503` with an explanatory body |
