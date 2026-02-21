# family-wishes

Production-ready Spring Boot 3.x (Java 17) backend for family wish automation.

## Features
- JWT auth + refresh tokens
- OTP login and forgot/reset password
- Role-based user management
- Event management and festival templates with versioning
- Gemini API wish generation
- Quartz daily scheduler
- SMTP email sending with retry and status tracking

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
- Hugging Face: https://huggingface.co/

### Gemini setup notes
- Ensure `GEMINI_API_KEY` is set in your environment.
- Validate the integration via the API endpoint: `POST /api/ai/generate-wish`.
- Production AI endpoint: `https://eventmanagement-backend-ka9x.onrender.com/api/ai/generate-wish`.

### Provider dashboard references
- Gemini developer/API resources are managed via Google AI/Vertex tooling based on your account setup.
- Hugging Face models and tokens can be managed at: https://huggingface.co/
