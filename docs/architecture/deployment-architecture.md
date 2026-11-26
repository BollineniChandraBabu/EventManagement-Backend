# Deployment architecture

## 1. Production topology

```
                    Internet (HTTPS 443)
                            │
                 ┌──────────▼───────────┐
                 │   Render edge / TLS  │  eventmanagement-backend-ka9x.onrender.com
                 └──────────┬───────────┘
                            │ HTTP 8080 (container network)
        ┌───────────────────▼────────────────────┐
        │  Docker container: eclipse-temurin:17  │
        │  java -Dspring.profiles.active=prod    │
        │  -jar app.jar                          │
        │  Tomcat 8080 · Quartz · @Async pool    │
        └───┬─────────────┬──────────────┬───────┘
            │             │              │
            │ JDBC        │ JDBC (chat)  │ S3 API
            ▼             ▼              ▼
     ┌────────────┐ ┌─────────────┐ ┌──────────────────┐
     │ Neon       │ │ Supabase    │ │ Supabase Storage │
     │ PostgreSQL │ │ PostgreSQL  │ │ bucket           │
     │ (primary)  │ │ (chat)      │ │ golden-greetings │
     └────────────┘ └─────────────┘ └──────────────────┘
            │
            │ outbound HTTPS
            ▼
   Gmail API · Gemini · Pollinations · Calendarific · ip-api.com · Google Identity
```

## 2. Hosting components

| Component | Provider | Notes |
| --- | --- | --- |
| Application container | Render | Built from the repository `Dockerfile`; single instance; `prod` profile |
| TLS / ingress | Render | Terminates HTTPS; forwards to container port 8080 |
| Primary database | Neon | PostgreSQL; connection string in `DB_URL` |
| Chat database | Supabase | PostgreSQL; connection string in `SUPABASE_DB_URL` |
| Object storage | Supabase | S3-compatible endpoint; bucket `golden-greetings`; region `ap-southeast-1` |
| Uptime monitoring | UptimeRobot | HTTP monitor on `/health`, 5-minute interval, 30 s timeout |
| UI hosting | Firebase Hosting | `https://chandu-goldengreetings.web.app` (separate deployment) |

## 3. Container

```dockerfile
FROM gradle:8.14.3-jdk17-alpine AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/family-wishes-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Dspring.profiles.active=prod","-jar","app.jar"]
```

| Property | Value |
| --- | --- |
| Build image | `gradle:8.14.3-jdk17-alpine` |
| Runtime image | `eclipse-temurin:17-jre-alpine` |
| Artifact | `build/libs/family-wishes-1.0.0.jar` |
| Profile | `prod` (baked into the entrypoint) |
| Port | 8080 (`server.port=8080` in `application-prod.properties`) |
| State | None inside the container; everything external |
| Config | Environment variables only |

## 4. Configuration and secrets

All settings arrive as environment variables; `application.properties` supplies defaults. The
complete list is in [../backend/configuration-reference.md](../backend/configuration-reference.md)
and the handling rules in [../deployment/secrets-management.md](../deployment/secrets-management.md).

Required in production:

| Variable | Used for |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Primary database |
| `JWT_SECRET` | Token signing (≥32 characters) |
| `GEMINI_API_KEY`, `GEMINI_API_URL` | AI text |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP identity, alert recipient, Brevo sender |
| `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN`, `GMAIL_FROM_EMAIL` | Gmail sending |
| `SUPABASE_STORAGE_ENDPOINT`, `SUPABASE_STORAGE_ACCESS_KEY_ID`, `SUPABASE_STORAGE_SECRET_ACCESS_KEY` | Media storage |
| `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD` | Chat database |
| `CALENDARIFIC_API_KEY` | Festival sync |
| `UI_BASE_URL` | Emailed deep links |

## 5. Network and ports

| Flow | Source → destination | Port / protocol |
| --- | --- | --- |
| UI → API | Browser → Render | 443 HTTPS |
| UI → WebSocket | Browser → Render | 443 WSS (`/ws-chat`) |
| Monitor → API | UptimeRobot → Render | 443 HTTPS (`/health`) |
| API → primary DB | Container → Neon | 5432 PostgreSQL (TLS per connection string) |
| API → chat DB | Container → Supabase | 5432 PostgreSQL |
| API → storage | Container → Supabase S3 | 443 HTTPS |
| API → Gmail | Container → Google | 443 HTTPS |
| API → Gemini | Container → Google | 443 HTTPS |
| API → images | Container → Pollinations | 443 HTTPS |
| API → festivals | Container → Calendarific | 443 HTTPS |
| API → geolocation | Container → ip-api.com | 80 HTTP (no credentials) |

CORS allows `http://localhost:4200`, `http://127.0.0.1:4200`, `https://*.firebaseapp.com`,
`https://chandu-goldengreetings.web.app`, `http://chandu-goldengreetings.web.app`, `https://*.web.app`
with credentials. The STOMP endpoint allows the same origins except localhost:4200 variants that are
listed explicitly.

## 6. Startup sequence

1. Container starts; Spring Boot loads `application.properties` and `application-prod.properties`.
2. `PrimaryDataSourceConfig` builds the primary Hikari pool; `SupabaseChatDbConfig` builds the chat
   pool.
3. Hibernate validates/updates the schema (`ddl-auto=update`).
4. `ChatSchemaInitializer` creates or upgrades the chat tables and indexes.
5. Quartz initialises its tables (`initialize-schema=always`) and registers the durable jobs.
6. `WishTemplateSeedDataInitializer` seeds reference data (themes, template types).
7. `GmailConfig` builds the Gmail client (fails fast if the OAuth credentials are unusable).
8. Tomcat listens on 8080; `/health` answers immediately.

## 7. Scaling model

| Dimension | Today | How to scale |
| --- | --- | --- |
| HTTP throughput | 1 instance | Add instances behind Render; JWT is stateless; Quartz is clustered |
| Scheduled jobs | 1 instance | Clustered Quartz elects one runner per trigger |
| Rate limiting | per instance | Needs a shared store before running several instances |
| Cache | per instance | Acceptable; each instance warms independently |
| Database | managed | Vertical scaling at Neon/Supabase; indexes already cover the hot queries |
| Storage | managed | Unbounded; objects are small (≤50 MB each) |

## 8. Availability and recovery

| Concern | Design |
| --- | --- |
| Health probe | `GET /health` → `{"status":"UP","time":…}` |
| Restart safety | All state external; Quartz jobs durable; `@Async` work lost on crash is retried by the next run |
| Duplicate protection | Date stamps on wishes; unique constraints on tokens and pairs |
| Database recovery | Managed by Neon/Supabase backup features; see [../operations/backup-and-recovery.md](../operations/backup-and-recovery.md) |
| Deployment rollback | Redeploy the previous image; schema is additive, so a rollback of code is safe unless a destructive column change shipped |

## 9. Environment parity

| Aspect | Local | Production |
| --- | --- | --- |
| Profile | default | `prod` |
| Port | 8080 | 8080 (behind Render) |
| Database | local or shared PostgreSQL | Neon |
| Chat database | local or shared PostgreSQL | Supabase |
| Storage | Supabase bucket (same) | Supabase bucket |
| Email | Gmail API (same credentials) or SMTP | Gmail API |
| Scheduler | runs locally (beware duplicate sends) | runs once in the container |
| TLS | none | Render |

> Running the application locally against the production database will start a second scheduler.
> Use a separate database, or disable the jobs, when testing.

## 10. Related documents

* [../deployment/deployment-guide.md](../deployment/deployment-guide.md)
* [../deployment/containerization.md](../deployment/containerization.md)
* [../deployment/infrastructure-requirements.md](../deployment/infrastructure-requirements.md)
* [../operations/health-checks.md](../operations/health-checks.md)
