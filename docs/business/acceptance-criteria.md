# Acceptance criteria

A feature is accepted when every criterion below is satisfied in the target environment. Criteria
are written so they can be checked against the running API (see
[../backend/api-specification.md](../backend/api-specification.md)) or against an automated test.

## 1. Authentication

| AC | Criterion |
| --- | --- |
| AC-AUTH-01 | `POST /api/auth/login` with valid credentials returns `200` and a body containing `accessToken`, `refreshToken`, `tokenType=Bearer`, `role`, `expiresIn=900`, `unreadChatMessagesCount`. |
| AC-AUTH-02 | A wrong password returns `400` with `message="Invalid email or password"` and increments `failed_login_attempts`. |
| AC-AUTH-03 | The fifth consecutive wrong password returns `400` with the contact-administrator message and sends one security email to the account owner. |
| AC-AUTH-04 | An unknown email returns `404` and creates a `violated_users` row containing the email and IP. |
| AC-AUTH-05 | `POST /api/auth/refresh` with a valid token returns a new access token; with a revoked token it returns `400`, or `401` with a `CONCURRENT_LOGIN:` prefix when the revocation came from a single-session sign-in. |
| AC-AUTH-06 | With `concurrentLoginAllowed=false`, signing in twice leaves only the newest refresh token usable and an old access token receives `401` with a body starting `CONCURRENT_LOGIN:`. |
| AC-AUTH-07 | An MFA-enabled user's password sign-in returns `400` with `MFA_OTP_REQUIRED` and no tokens. |
| AC-AUTH-08 | A sixth OTP resend within the code lifetime returns `400` with the resend-limit message. |
| AC-AUTH-09 | A password-reset token can be used once; a second attempt returns `400`. |
| AC-AUTH-10 | The eleventh request within 60 seconds to `/api/auth/login` from one IP returns `429` with `{status:429, message:…}`. |
| AC-AUTH-11 | `POST /api/auth/admin/login-as-user` succeeds for a non-admin target and fails for an admin target with `400`. |
| AC-AUTH-12 | `GET /api/auth/admin/switch-back` on a non-impersonated token returns `400`. |

## 2. Users

| AC | Criterion |
| --- | --- |
| AC-USER-01 | Creating a user returns `200` with the persisted values and a working sign-in using password `Test`. |
| AC-USER-02 | A birthday wishes URL of `ftp://x`, or one longer than 500 characters, is rejected with `400`. |
| AC-USER-03 | `GET /api/users/directory` never returns `role`, `dateOfBirth`, `mfaEnabled` or wish settings. |
| AC-USER-04 | A non-admin calling `GET /api/users` receives `403`. |
| AC-USER-05 | `PATCH /api/users/{id}/concurrent-login` succeeds for an admin and for the same user, and returns `403` for a different non-admin. |
| AC-USER-06 | A profile picture upload of a PDF returns `400` with the image-only message. |
| AC-USER-07 | Deactivating a user makes subsequent sign-in fail and removes the user from the public directory. |

## 3. Events and calendar

| AC | Criterion |
| --- | --- |
| AC-EVT-01 | Creating an event with `eventType="anniversary"` resolves to the seeded `ANNIVERSARY` type and is stored active. |
| AC-EVT-02 | Creating an event with an unknown type returns `400` "Invalid event type". |
| AC-EVT-03 | A non-admin reading another user's event receives `404`, not `403`. |
| AC-EVT-04 | Replacing an event image deletes the previous object from storage (the object key changes and the old key no longer resolves). |
| AC-CAL-01 | `GET /api/calendar?month=11` returns festivals, the caller's birthdays and events, and those of accepted circle members. |
| AC-CAL-02 | A circle member's entry has `fromCircle=true` and a non-null `ownerUserId`; the caller's own entries have `fromCircle=false`. |
| AC-CAL-03 | An entry present both as own and as circle appears once. |
| AC-CAL-04 | With `year=2027`, festival entries carry 2027 dates and `recurring=false`; birthdays remain `recurring=true`. |

## 4. Festivals

| AC | Criterion |
| --- | --- |
| AC-FEST-01 | Running the monthly sync with a valid key creates or updates `seed_special_events` rows for the current year and marks removed ones inactive. |
| AC-FEST-02 | Running the sync without a key logs a warning and changes nothing. |
| AC-FEST-03 | Requesting a year with no stored festivals triggers one provider fetch; the second request is served from the database. |
| AC-FEST-04 | Bulk mapping 3 users returns `{created:3, updated:0}`; repeating it returns `{created:0, updated:3}`. |
| AC-FEST-05 | `GET /api/festival-wish-mappings?status=FAILED` returns only mappings with `error=true`. |
| AC-FEST-06 | A mapping that failed keeps `error=true` after the run and is retried on the next run that day; after a success `error=false`. |

## 5. Wishes and email

| AC | Criterion |
| --- | --- |
| AC-WISH-01 | `POST /api/ai/generate-wish` returns `{subject, htmlMessage}` containing the recipient name and no placeholder tokens. |
| AC-WISH-02 | When the primary Gemini model returns 429, the call still succeeds using a fallback model. |
| AC-WISH-03 | When every model fails, the endpoint returns `503` with the AI-busy message. |
| AC-WISH-04 | With an uploaded image for the person and event type, that image is used; with only a global image for the type, that one is used; with neither, an image is generated. |
| AC-WISH-05 | When image generation fails, the email is still sent and `email_logs.status=SENT`. |
| AC-WISH-06 | An email for a user with a birthday wishes URL contains the button and the plain link; without a URL it contains neither. |
| AC-WISH-07 | A note or name containing `<script>` appears escaped in the email body. |
| AC-WISH-08 | Every send creates an `email_logs` row with the right `email_type`, `status`, `retry_count` and `sent_at`. |
| AC-WISH-09 | A failed email with `retry_count<3` is retried; at 3 it is not. |
| AC-WISH-10 | `GET /api/emails/status/{id}` for an OTP email returns `403` for a non-admin, including the recipient (sensitive type). |
| AC-WISH-11 | `GET /api/emails/status/{id}/image` returns the image bytes with the correct content type and `Cache-Control: private, max-age=600`. |
| AC-WISH-12 | With `AGENT_WISH_ENABLED=true`, a validator rejection is fed back and a corrected submission is accepted; a total agent failure still produces a wish via the fallback generator. |

## 6. Circles and chat

| AC | Criterion |
| --- | --- |
| AC-CIR-01 | Sending a request to someone who already requested you accepts the circle and returns `ACCEPTED`. |
| AC-CIR-02 | A non-addressee calling accept or reject receives `400`. |
| AC-CIR-03 | A new request produces one email to the addressee containing a `token=` and `email=` link; a mail failure does not fail the API call. |
| AC-CIR-04 | A request older than the reminder threshold is reminded once per day and stops after accept/reject. |
| AC-CIR-05 | `GET /api/circles` returns `circles`, `incomingRequests` and `outgoingRequests`. |
| AC-CHAT-01 | With circles-only enabled, messaging a non-circle user returns `400`; after acceptance the same call succeeds. |
| AC-CHAT-02 | A message with no text, no encrypted payload and no attachment returns `400`. |
| AC-CHAT-03 | A 60 MB attachment is rejected before storage; a 49 MB PDF is stored and downloadable with `application/pdf` and the original file name. |
| AC-CHAT-04 | A renamed file (SVG named `.png`) is rejected because the bytes disagree with the declared type. |
| AC-CHAT-05 | `GET /api/chat/unread-count` decreases to 0 after `GET /api/chat/messages/{otherUserId}?markSeen=true`. |
| AC-CHAT-06 | A STOMP message to `/app/chat.send` appears on `/topic/chat/{conversationId}` and is persisted. |
| AC-CHAT-07 | A user whose heartbeat is older than 2 minutes is reported offline. |

## 7. Notifications

| AC | Criterion |
| --- | --- |
| AC-NOTIF-01 | Publishing a second notification unpublishes the first. |
| AC-NOTIF-02 | Publishing with `scheduledFrom` in the past returns `400` and leaves the currently published notification unchanged. |
| AC-NOTIF-03 | A future `scheduledFrom` publishes at that time; `scheduledTo` unpublishes at that time. |
| AC-NOTIF-04 | `GET /api/notifications/published` is readable without a token and returns the published notification. |
| AC-NOTIF-05 | With `canSendEmail=true`, all active users receive the announcement email. |

## 8. Media and storage

| AC | Criterion |
| --- | --- |
| AC-MEDIA-01 | A 6 MB PNG is rejected with the image size message; a 4 MB PNG is accepted. |
| AC-MEDIA-02 | A video larger than 50 MB is rejected by the servlet container. |
| AC-MEDIA-03 | Stored media is gzipped (object starts with `1f 8b`) when gzip is enabled and is expanded on read. |
| AC-MEDIA-04 | With `app.media.gzip.enabled=false`, objects are stored uncompressed and still readable. |

## 9. Themes, seeds and dashboards

| AC | Criterion |
| --- | --- |
| AC-THEME-01 | `GET /api/themes` returns the 11 seeded themes with `code` and `displayName`. |
| AC-THEME-02 | `PUT /api/themes/me` with `liquid-glass` persists and is returned by `GET /api/themes/me`; an unknown code returns `400`. |
| AC-SEED-01 | Each seed controller supports create, update-by-code, list with search, read and delete. |
| AC-DASH-01 | `GET /api/dashboard/mail` returns four counters and is cached for 60 seconds. |
| AC-DASH-02 | `GET /api/dashboard/chart/mail` rejects equal start and end dates with `400`. |
| AC-DASH-03 | `GET /api/dashboard/violated-users` rejects `size=0` and `size=501` with `400`, and caps map points at 300. |
| AC-DASH-04 | A non-admin calling an admin-only dashboard endpoint receives `403`. |

## 10. Operations

| AC | Criterion |
| --- | --- |
| AC-OPS-01 | `GET /health` returns `200` with `{"status":"UP","time":…}` without a token. |
| AC-OPS-02 | `/v3/api-docs` returns valid OpenAPI 3 JSON and `/swagger-ui.html` loads. |
| AC-OPS-03 | `GET /api/schedulers` lists every job with counters and next fire time; `POST /api/schedulers/{job}/trigger` fires it. |
| AC-OPS-04 | `GET /api/agent/status` reports the three agent switches and the failed-email count. |
| AC-OPS-05 | `POST /api/agent/admin/ask` with the copilot disabled returns `503` with an explanatory body. |

## 11. Definition of done (all changes)

1. Code compiles under the Java 17 toolchain (`./gradlew compileJava`).
2. `./gradlew test` passes.
3. New or changed Java files are formatted with google-java-format.
4. No secret is committed; new configuration is documented in
   [../backend/configuration-reference.md](../backend/configuration-reference.md).
5. API changes are reflected in [../backend/api-specification.md](../backend/api-specification.md).
6. Error paths return the standard `{timestamp, status, message}` body.
7. Authorization is explicit: admin-only endpoints carry `@PreAuthorize("hasRole('ADMIN')")`.
8. User-visible text that reaches an email is escaped.
