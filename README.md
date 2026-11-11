# family-wishes

Production-ready Spring Boot 3.x (Java 17) backend for family wish automation.

## Features
- JWT auth + refresh tokens
- OTP login and forgot/reset password
- Role-based user management
- Circles and circle requests
- Concurrent login control (single-session option per user)
- Media uploads (image, video, audio, PDF) with gzip at rest and gzip responses
- Event management + admin-managed festival seed table for scheduler templates
- Calendarific festival sync (monthly)
- Festival-to-user mapping with auto-send scheduler
- Gemini API wish generation
- Quartz daily scheduler
- SMTP email sending with retry and status tracking


## Seed management APIs
- `GET /api/seed/special-events/{id}` (admin)
- `GET /api/seed/special-events` (admin, supports pagination/search/sort)
- `POST /api/seed/special-events` (admin)
- `GET /api/seed/special-events/today` (admin, active seeds for current IST day)
- `PUT /api/seed/special-events/{id}` (admin)
- `DELETE /api/seed/special-events/{id}` (admin)

These APIs manage data in the `seed_special_events` table used by the festival scheduler.

## Festival APIs
- `GET /api/festivals?month=MM` (authenticated, defaults to current IST month)
- `POST /api/festival-wish-mappings` (admin, upsert mapping of festival -> user)
- `GET /api/festival-wish-mappings` (admin)
- `DELETE /api/festival-wish-mappings/{id}` (admin)

Festival sync job:
- Runs monthly via cron (`0 0 2 1 * ?`) and re-syncs Calendarific festival data for all months.
- Existing rows are upserted by festival name and their dates are updated every year.
- Rows no longer present in latest sync are marked inactive.

### Do you need both `Event` and `SpecialEvent`?
- `Event` is user-scoped and used by the daily email/AI wishes flow.
- `SpecialEvent` seed rows are global templates used by Instagram festival broadcasting.
- If you want only one model, we can later unify them by redesigning `Event` to support global template records and message bodies.

## Run
```bash
gradle bootRun
```

## Build
```bash
gradle clean bootJar
```

## Environment variables
- `JWT_SECRET`
- `GEMINI_API_KEY`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `DB_URL`, `DB_USER`, `DB_PASS` for production profile
- `CALENDARIFIC_API_KEY`
- `CALENDARIFIC_COUNTRY` (default `IN`)
- `CALENDARIFIC_API_URL` (default `https://calendarific.com/api/v2/holidays`)
- `UI_BASE_URL` (default `https://chandu-goldengreetings.web.app`) — one base for every emailed deep link
- `PASSWORD_RESET_UI_URL` (optional override; defaults to `${UI_BASE_URL}/reset-password`)
- `CIRCLE_REQUEST_LOGIN_URL` (optional override; defaults to `${UI_BASE_URL}/circles`)
- `CIRCLE_REQUEST_LOGIN_TTL_MINUTES` (default `60`)
- `APP_CHAT_CIRCLES_ONLY` (default `true`)
- `APP_MEDIA_GZIP_ENABLED` (default `true`)

## Database reference
- Primary PostgreSQL database is hosted on Neon.
- Neon project console: https://console.neon.tech/

## Deployment
- Platform: Render
- Production base URL: `https://eventmanagement-backend-ka9x.onrender.com`

## API Docs
- Local Swagger UI: `http://localhost:8080/swagger-ui.html`
- Local OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Production Swagger UI: `https://eventmanagement-backend-ka9x.onrender.com/swagger-ui.html`
- Production OpenAPI JSON: `https://eventmanagement-backend-ka9x.onrender.com/v3/api-docs`

## Monitoring (UptimeRobot)

The production uptime monitor is managed in UptimeRobot:
- Dashboard: https://dashboard.uptimerobot.com/monitors

### Recommended monitor configuration for this service
- **Monitor type:** HTTP(s)
- **Friendly name:** `family-wishes-backend-prod`
- **URL to monitor:** `https://eventmanagement-backend-ka9x.onrender.com/health`
- **Monitoring interval:** 5 minutes (or 1 minute for tighter SLA)
- **Timeout:** 30 seconds
- **HTTP method:** `GET`
- **Keyword/value check (optional):** expect JSON containing `"status":"UP"`

### Why `/health`
- This project exposes a public health endpoint at `GET /health`.
- The endpoint returns a JSON payload that includes a status key with value `UP` when the API is healthy.

Example response:
```json
{
  "status": "UP",
  "time": "2026-01-01T10:00:00"
}
```

### Alerting recommendations
- Configure at least one email + one chat/phone contact in UptimeRobot.
- Alert on both **DOWN** and **UP** (recovery) events.
- If you run multiple environments, create separate monitors (e.g., `staging`, `prod`).

## AI Providers

This backend integrates with Gemini for AI wish generation and can be extended to support other providers.

- Gemini: https://gemini.google.com/
- Pollinations AI: https://pollinations.ai/

### Gemini setup notes
- Ensure `GEMINI_API_KEY` is set in your environment.
- Validate the integration via the API endpoint: `POST /api/ai/generate-wish`.
- Production AI endpoint: `https://eventmanagement-backend-ka9x.onrender.com/api/ai/generate-wish`.

### Provider dashboard references
- Gemini developer/API resources are managed via Google AI/Vertex tooling based on your account setup.
- Pollinations image API reference: https://gen.pollinations.ai/image/{prompt}?model=imagen-4

## Gmail OAuth troubleshooting

If you see this error while sending emails:

```text
com.google.auth.oauth2.GoogleAuthException: ...
"error": "invalid_grant",
"error_description": "Token has been expired or revoked."
```

your configured `GMAIL_REFRESH_TOKEN` is no longer valid.

### Fix steps
1. Re-run Google OAuth consent for your Gmail app and generate a **new refresh token**.
2. Update deployment environment variables:
   - `GMAIL_CLIENT_ID`
   - `GMAIL_CLIENT_SECRET`
   - `GMAIL_REFRESH_TOKEN`
3. Restart the backend service.

The backend now logs a clear startup/runtime hint when this specific revoked/expired token condition is detected.

## Pollinations image provider setup

This backend now uses Pollinations image generation API:

```text
https://gen.pollinations.ai/image/{prompt}?model=imagen-4
```

Required environment variables:
- `POLLINATIONS_IMAGE_URL` (default: `https://gen.pollinations.ai/image/`)
- `POLLINATIONS_IMAGE_KEY` (optional; set this if your account requires API-key auth)
- `POLLINATIONS_IMAGE_MODEL` (default: `imagen-4`)

If Pollinations image generation fails, backend logs a warning and continues sending emails **without** inline AI images.

Example request:

```bash
curl "https://gen.pollinations.ai/image/a beautiful sunset over mountains?model=imagen-4" \
  --header "Authorization: Bearer <your_pollinations_api_key>"
```

## Agentic AI (`com.familywishes.agent`)
All agents are **off by default** and share one tool-calling loop (`AgentRunner`) on top of the existing Gemini endpoint.

| Agent | What it does | Flag |
|---|---|---|
| `WishComposerAgent` | Reads recipient profile + recent wishes, drafts, and must pass a validator tool (`submit_wish`) before the wish is accepted. Wired in via a `@Primary` `AgenticAiService`, so all schedulers/endpoints benefit; falls back to the old single prompt on any failure. | `AGENT_WISH_ENABLED` |
| `DeliveryOpsAgent` | Every 30 min (only when failed emails exist): classifies errors, retries transient ones, sends at most one admin alert. Dry-run by default. | `AGENT_DELIVERY_OPS_ENABLED`, `AGENT_DELIVERY_OPS_DRY_RUN` |
| `AdminCopilotAgent` | `POST /api/agent/admin/ask` - read-only natural-language questions about email stats, failed logins, birthdays. Admin only. | `AGENT_COPILOT_ENABLED` |

## Gemini model fallback (`com.familywishes.ai`)
All Gemini text calls (wishes and agents) go through `GeminiClient`:

1. It calls the model in `GEMINI_API_URL`.
2. On a rate limit (429), a missing model (404), a server error (5xx) or a timeout, it tries the next model from `GeminiModelCatalog`, at most `GEMINI_FALLBACK_MAX_RETRIES` (default 5) extra times.
3. A model that just failed is skipped for `GEMINI_FALLBACK_COOLDOWN_SECONDS` (default 60).
4. If nothing answers, the API returns `503`. Other errors (bad key, bad request) are not retried.

`GeminiModelCatalog` asks Google which models can generate text and caches the list for `GEMINI_FALLBACK_MODEL_CACHE_MINUTES` (default 60), so new models are picked up and retired ones disappear automatically. No model names are configured anywhere. `GEMINI_FALLBACK_EXCLUDED_MODEL_PATTERN` is a regex for kinds of models to skip (embedding, image, audio, preview, ...). Set `GEMINI_FALLBACK_MAX_RETRIES=0` to turn the fallback off.

## Caching (Ehcache)
Configured in `src/main/resources/ehcache.xml`. Every cache has a size limit and an expiry, so memory use stays bounded (`CacheConfigurationTest` enforces this and checks that each name in `CacheNames` is declared).

| Cache | What | Lifetime | Cleared when |
|---|---|---|---|
| `seedData` + query cache (Hibernate) | event types, relationships, template types and the lookups by code done on every user/event save and email send | 1 h | Hibernate clears it itself when a row changes |
| `emailImages` | an email's image, loaded on demand through `GET /api/emails/status/{id}/image` | 15 min | the email is deleted |
| `ipInfo` | IP geolocation from ip-api.com (45 requests/min limit); failures are not cached | 24 h | expiry |
| `festivalsByMonth` | festivals of a month for the calendar | 6 h | festival sync, or any special-event create/update/delete |
| `dashboardSummary` | the four dashboard counters | 60 s | expiry |

Cached values must implement `Serializable` (so the caches also work if they are ever configured to store by value).

## Email status API and images
`GET /api/emails/status` (and the other status lists) no longer include image data; each row only has `hasImage`. Fetch the picture when it is needed with `GET /api/emails/status/{id}/image` (same access rules as the status endpoints: admins see everything, other users only their own non-sensitive emails). The response is the image itself with its content type and `Cache-Control: private, max-age=600`.

## Custom wishes link (birthday and events)
A user can have a **birthday wishes page** (`users.birthday_wishes_url`) and an event such as an anniversary can have a **wishes page** (`events.wishes_url`), for example one of your static HTML pages. When the email is sent:

- with a link: the email ends with a button "Open your birthday / anniversary wishes" and a plain-text copy of the link,
- without a link: nothing is added (the feature is skipped for that user or event).

Links must be `http://` or `https://` addresses of at most 500 characters, without spaces or a username/password (`WishesUrls`). Anything else is rejected with a `400`. Saving an empty value removes the link. Festival mappings can have a personal note instead, which is added at the end of the festival email. All text that comes from a user is HTML-escaped (`WishEmailExtras`).

## Images for wishes
Uploaded images (`/wish-images`) are chosen first, otherwise an image is generated, and the email is always sent (even if generation fails). The first match wins:

1. an image uploaded for that person and event type,
2. an image uploaded for everyone for that event type,
3. for festivals, the same two steps for the general `Festival` type,
4. otherwise a generated image.

Event types are not case-sensitive, and codes such as `WEDDING_ANNIVERSARY` also match images named `Wedding Anniversary`. Anniversary and other event emails now get an image too (they used to be sent without one).

## Uploaded media types
Uploads for wishes (`/wish-images`, `/api/events/with-image`, `/api/festival-wish-mappings/with-image`) and chat attachments accept any supported media, not only images:

| Kind | Formats | Size limit |
|---|---|---|
| Image | PNG, JPEG, GIF, WebP, BMP | 5 MB |
| Video | MP4, WebM, MOV, AVI, MKV, MPEG, 3GP | 50 MB |
| Audio | MP3, WAV, OGG, M4A, FLAC | 10 MB |
| Document | PDF | 20 MB |

Validation lives in `MediaValidation` and detection in `MediaContentTypes`. The file's first bytes decide the real type, so a renamed file (for example an SVG called `.png`) is rejected; the declared content type is only accepted when it agrees with the bytes. The stored object keeps the detected content type and a matching extension.

Profile pictures stay image-only. When an email carries an uploaded image it is shown inline (`cid:birthdayImage`); other media types are sent as attachments instead. Chat attachments are served back with their real content type and file name. The multipart request limit is 50 MB (`spring.servlet.multipart.max-file-size`).

### gzip compression
Uploaded media is gzipped before it is stored and expanded again on download. The stored object is labelled `Content-Encoding: gzip`, so a presigned or public URL still renders in a browser. This is on by default and controlled by `app.media.gzip.enabled` (env `APP_MEDIA_GZIP_ENABLED`). JSON and other text responses are compressed by Spring Boot as well (`server.compression.enabled=true`).

## Circles and circle requests
Circles are stored in the `circles` table (one row per pair, with a direction until accepted).

- `GET /api/circles` returns `circles`, `incomingRequests` and `outgoingRequests`.
- `POST /api/circles/requests` sends a request (`{"userId": 2}`). Sending to someone who already asked you accepts it instead.
- `POST /api/circles/requests/accept` and `POST /api/circles/requests/reject` act on `{"circleId": 5}`. Only the addressee can respond.
- `DELETE /api/circles` removes a circle (`{"circleId": 5}`).
- `GET /api/circles/status/{userId}` reports whether a user is in your circle.

When a **new** request is created (`POST /api/circles/requests`), the addressee gets a notification email with a one-click sign-in link. The link opens the circles page and signs the user in directly, with no password. It is a single-use `login_tokens` row that expires after `app.circle.request.login-ttl-minutes` (env `CIRCLE_REQUEST_LOGIN_TTL_MINUTES`, default 60). Repeated emails reuse the same still-valid link.

- The link target is `app.circle.request.login-url` (env `CIRCLE_REQUEST_LOGIN_URL`, default `${UI_BASE_URL}/circles`).
- The app opens it as `<login-url>?token=<token>&email=<email>` and calls `POST /api/auth/direct-login` (`{"token": "...", "email": "..."}`), which returns the usual access and refresh tokens.
- The email is sent asynchronously; a mail failure never blocks the circle request.

Chat is restricted to circle members by default. Messaging is allowed only between users in each other's accepted circle (`app.chat.circles-only=true`, env `APP_CHAT_CIRCLES_ONLY`). Set it to `false` to open chat to everyone.

Once a circle request is accepted, the two users can see each other's birthdays and events on the calendar. `GET /api/calendar?month=11` returns the signed-in user's own entries plus the birthdays and anniversaries of their accepted circle members. Circle entries carry `ownerUserId` (the person the entry belongs to) and `fromCircle=true`; the user's own entries have `fromCircle=false`. Admins already see everyone, so `fromCircle` only flags entries that are not the admin's own.

## Concurrent login control
Each user has a `concurrentLoginAllowed` flag that defaults to **true** (concurrent logins allowed).

- Admin create/update (`POST /api/users`, `PUT /api/users`) set the flag from the request body.
- `PATCH /api/users/me/concurrent-login` lets a signed-in user change their own flag (`{"concurrentLoginAllowed": false}`).
- `PATCH /api/users/{id}/concurrent-login` is allowed for an admin, or for the user themselves.

When the flag is `false`, a successful sign-in revokes that user's earlier refresh tokens, so only the newest session survives. Access tokens are short lived and are not revoked.

## Festival wishes API
- `POST /api/festival-wish-mappings/bulk` maps one festival to many users (existing mappings are updated).
- `GET /api/festival-wish-mappings/summary` returns the dashboard counts.
- `GET /api/festival-wish-mappings?status=ALL|ACTIVE|INACTIVE|FAILED` filters the list. Responses include `lastWishSentOn`, `error` and `customMessage`.
- A mapping whose last attempt failed keeps `error = true` until a later run succeeds; the flag is now cleared after a success (before, a mapping that failed once was sent again on every run that day).

## Events and violated users
- `PUT /api/events/{id}` updates an event (it did not exist before, so editing an event could not be saved). `GET /api/events/{id}` is limited to admins and the event's owner.
- `GET /api/dashboard/violated-users` accepts `searchKey` (email or IP) and returns `uniqueIps`, `uniqueEmails`, `topIps` and `topEmails`. Map points are limited to the newest 300 and are no longer looked up one by one in ip-api.com.

## Theme styles API
Theme styles are seeded into the `theme_seeds` table at startup. Each row stores a technical name (`code`) and a user-friendly name (`display_name`):

| Technical name | User-friendly name |
| --- | --- |
| skeuomorphism | Realistic Design |
| neomorphism | Soft UI |
| glassmorphism | Frosted Glass |
| claymorphism | Soft 3D Design |
| minimalism | Clean & Simple |
| maximalism | Bold & Vibrant |
| brutalism | Bold & Raw |
| liquid-glass | Liquid Glass |
| bento-grid | Modular Grid |
| spatial-ui | 3D Spatial Design |
| default | default |

- `GET /api/themes` returns the active theme styles (`id`, `code`, `displayName`).
- `GET /api/themes/me` returns the signed-in user's selected theme, or `default` when none is saved.
- `PUT /api/themes/me` saves the signed-in user's selected theme by its technical name (`{ "theme": "liquid-glass" }`) and returns the stored value. Unknown themes are rejected with `400`.

The user's choice is stored in `user_theme_preferences`, whose `theme` column defaults to `default`.
