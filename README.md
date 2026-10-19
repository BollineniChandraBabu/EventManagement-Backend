# family-wishes

Production-ready Spring Boot 3.x (Java 17) backend for family wish automation.

## Features
- JWT auth + refresh tokens
- OTP login and forgot/reset password
- Role-based user management
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

