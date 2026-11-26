# Golden Greetings — Backend (`family-wishes`)

Spring Boot 3.3 backend for a family wish-automation platform. It stores family members,
birthdays, anniversaries and festivals, generates personalised greeting emails with an AI model,
sends them on a schedule, and gives signed-in users a calendar, circles (trusted contacts), chat,
notifications and a set of admin dashboards.

* **Language / runtime:** Java 17 (Gradle toolchain), Spring Boot 3.3.3
* **Build:** Gradle (`bootJar`, `test`, `spotlessApply`)
* **Database:** PostgreSQL (primary, hosted on Neon) + a second PostgreSQL instance for chat
  (Supabase)
* **Object storage:** Supabase Storage (S3-compatible API)
* **Deployment:** Render (Docker image, `prod` profile), Swagger UI published

## Features

| Area | What it provides |
| --- | --- |
| Authentication | Email/password login, JWT access (15 min) + refresh (7 days) tokens, OTP login, Google SSO, forgot/reset password, one-click direct login from email links |
| User management | Admin CRUD, roles (`ROLE_ADMIN`, `ROLE_USER`), activation/deactivation, profile pictures, wish preferences, per-user MFA and single-session flags |
| Events | Per-user birthdays, anniversaries, engagements and other seeded event types, optional custom wishes link, optional admin-uploaded greeting media |
| Wishes | AI-generated subject + HTML body (Gemini with model fallback), AI-generated or admin-uploaded image, personalised wishes link, signature, delivery logging and retry |
| Festivals | Calendarific sync (monthly + on demand for other years), festival-to-user mappings, bulk mapping, per-mapping personal note, festival wish scheduler |
| Schedulers | Quartz (JDBC, clustered) daily wish, good morning, birthday, festival wish, good night, refresh-token cleanup, unread-chat mail, circle-request reminders; Spring `@Scheduled` Instagram jobs and the delivery-ops agent |
| Calendar | Month view of festivals, birthdays and events, including accepted circle members' entries, optional year scoping |
| Circles | Requests, accept/reject, removal, status; request and reminder emails with one-click sign-in links; chat restricted to circles by default |
| Chat | Conversations, text/voice/attachment messages, replies, reactions, edits, deletes, presence, unread counts, STOMP over WebSocket, unread-message email digest |
| Notifications | Admin-authored announcements with optional email, publish/unpublish, scheduling window, realtime push |
| Dashboards | Mail/Instagram counters and charts, OTP and password-reset stats, login locations, failed-login ("violated users") analytics with IP geolocation |
| Media | Byte-sniffed validation of images, video, audio and PDF; gzip at rest; signed URLs |
| AI agents | Agentic wish composer with validator tool, delivery-ops triage agent, read-only admin copilot |
| Operations | `/health`, Swagger/OpenAPI, scheduler status and manual trigger, email status and retry |

## Prerequisites

* **JDK 17** available to Gradle (the build pins a Java 17 toolchain; you may launch Gradle with a
  newer JDK as long as a 17 installation is discoverable)
* Gradle 8.x (or the wrapper, if you generate it)
* A PostgreSQL database (primary). A second PostgreSQL database is required for chat.
* A Supabase (or any S3-compatible) bucket for media
* Credentials for the integrations you enable: Gmail OAuth, Gemini, Pollinations, Calendarific,
  Brevo (optional)

## Run locally

```bash
# 1. Export the required environment variables (see docs/backend/configuration-reference.md)
export DB_URL='jdbc:postgresql://localhost:5432/familywishes'
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET='<at least 32 characters>'
export GEMINI_API_KEY='...'
export GEMINI_API_URL='https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash'
export MAIL_USERNAME='you@example.com'
export MAIL_PASSWORD='app-password'
export GMAIL_CLIENT_ID='...' GMAIL_CLIENT_SECRET='...' GMAIL_REFRESH_TOKEN='...' GMAIL_FROM_EMAIL='...'
export SUPABASE_STORAGE_ACCESS_KEY_ID='...' SUPABASE_STORAGE_SECRET_ACCESS_KEY='...'
export SUPABASE_DB_URL='jdbc:postgresql://...' SUPABASE_DB_USERNAME='...' SUPABASE_DB_PASSWORD='...'

# 2. Run
./gradlew bootRun          # or: gradle bootRun
```

The API listens on `http://localhost:8080`.

| URL | Purpose |
| --- | --- |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON |
| `http://localhost:8080/health` | Liveness payload (`{"status":"UP"}`) |

Build a jar:

```bash
./gradlew clean bootJar     # build/libs/family-wishes-1.0.0.jar
java -jar build/libs/family-wishes-1.0.0.jar
```

Format only the files you touch (the repository is not fully spotless-clean):

```bash
./gradlew spotlessApply && git checkout -- $(git diff --name-only | grep -v '^src/main/java/com/familywishes/<your files>')
```

Run tests:

```bash
./gradlew test
```

## Docker

```bash
docker build -t golden-greetings-backend .
docker run --rm -p 8080:8080 --env-file .env golden-greetings-backend
```

The image runs `java -Dspring.profiles.active=prod -jar app.jar`.

## Production

* Base URL: `https://eventmanagement-backend-ka9x.onrender.com`
* Swagger UI: `https://eventmanagement-backend-ka9x.onrender.com/swagger-ui.html`
* Health monitor (UptimeRobot): `GET /health`, 5-minute interval, expect `"status":"UP"`

## Documentation

Full documentation lives in [`docs/`](docs/README.md):

* Product and requirements — [`docs/business/`](docs/business/project-overview.md)
* Architecture and design — [`docs/architecture/`](docs/architecture/system-architecture.md)
* Backend, API, data and configuration — [`docs/backend/`](docs/backend/backend-design.md)
* Testing — [`docs/testing/`](docs/testing/test-strategy.md)
* Build, deployment and environments — [`docs/deployment/`](docs/deployment/build-and-run.md)
* Security — [`docs/security/`](docs/security/security-design.md)
* Operations and support — [`docs/operations/`](docs/operations/operations-runbook.md)
* Project management — [`docs/project/`](docs/project/project-plan.md)

## Contributing

See [`docs/project/contribution-guide.md`](docs/project/contribution-guide.md) and
[`docs/project/coding-standards.md`](docs/project/coding-standards.md). `AGENTS.md` holds the
short repository-specific notes for coding agents.

## License

Proprietary. See the OpenAPI metadata (`License: Proprietary`).
