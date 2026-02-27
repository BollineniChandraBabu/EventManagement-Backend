# family-wishes

Production-ready Spring Boot 3.x (Java 17) backend for family wish automation.

## Features
- JWT auth + refresh tokens
- OTP login and forgot/reset password
- Role-based user management
- Event management + admin-managed festival seed table for scheduler templates
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
