# Build and run

## 1. Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 21 or newer to launch Gradle, **17 for the toolchain** | `java.toolchain.languageVersion = 17` |
| Gradle | 8.14.x | Use the Gradle in the Docker build image, or install locally |
| PostgreSQL | 14+ | Primary database; Supabase/Neon also work |
| Git | any | |

The build pins a Java 17 toolchain, so Gradle needs a JDK 17 installation even when it runs on a
newer JDK. Point Gradle at it in `~/.gradle/gradle.properties` (not in the repository):

```properties
org.gradle.java.installations.paths=/path/to/jdk-17
```

## 2. Build

```bash
# Compile
./gradlew compileJava

# Tests
./gradlew test

# Format only the files you touched
./gradlew spotlessApply

# Build the runnable jar
./gradlew bootJar
# → build/libs/family-wishes-1.0.0.jar
```

Notes:

* `tasks.named('build')` depends on `spotlessApply`, so `./gradlew build` reformats sources.
  Revert unrelated files afterwards if you do not want a whole-tree reformat.
* `spotlessCheck` currently reports pre-existing violations; do not use it as a merge gate without a
  baseline.

## 3. Configure

Create a `.env` file (add it to `.gitignore` — it is not ignored in the repository today) or export the variables. The minimum to start:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/familywishes'
export DB_USERNAME='postgres'
export DB_PASSWORD='postgres'
export JWT_SECRET='change-me-to-at-least-32-characters-long'
```

Feature-complete (email, AI, storage, chat, festivals):

```bash
export MAIL_USERNAME='you@gmail.com'
export MAIL_PASSWORD='app-password'
export GMAIL_CLIENT_ID='…'
export GMAIL_CLIENT_SECRET='…'
export GMAIL_REFRESH_TOKEN='…'
export GMAIL_FROM_EMAIL='you@gmail.com'
export GEMINI_API_KEY='…'
export GEMINI_API_URL='https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash'
export SUPABASE_STORAGE_ENDPOINT='https://<project>.storage.supabase.co/storage/v1/s3'
export SUPABASE_STORAGE_ACCESS_KEY_ID='…'
export SUPABASE_STORAGE_SECRET_ACCESS_KEY='…'
export SUPABASE_DB_URL='jdbc:postgresql://<host>:5432/postgres'
export SUPABASE_DB_USERNAME='postgres'
export SUPABASE_DB_PASSWORD='…'
export CALENDARIFIC_API_KEY='…'
export UI_BASE_URL='http://localhost:4200'
```

See [../backend/configuration-reference.md](../backend/configuration-reference.md) for every
property.

## 4. Run

```bash
# Foreground
./gradlew bootRun

# Or the jar
java -jar build/libs/family-wishes-1.0.0.jar

# Production profile locally (uses application-prod.properties)
java -Dspring.profiles.active=prod -jar build/libs/family-wishes-1.0.0.jar
```

Verify:

```bash
curl -s http://localhost:8080/health
# {"status":"UP","time":"..."}

curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/v3/api-docs   # 200
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

## 5. Run with Docker

```bash
docker build -t family-wishes:local .
docker run --rm -p 8080:8080 --env-file .env family-wishes:local
```

## 6. First-run checks

| Check | How |
| --- | --- |
| Schema created | Tables exist in the primary database (`users`, `events`, …) and `QRTZ_*` |
| Chat schema created | `chat_conversations`, `chat_messages`, `chat_message_reactions` exist |
| Seed data | `GET /api/themes` returns 11 themes |
| Scheduler registered | `GET /api/schedulers` lists nine jobs |
| Admin exists | Sign in as an admin; if none, create one in SQL |

Create the first admin manually:

```sql
-- password = BCrypt('Test'); replace with a hash you generate
INSERT INTO users (name,email,password,gender,role,deleted,failed_login_attempts,
                   mfa_enabled,concurrent_login_allowed,active,relationship_seed_id,
                   created_at,updated_at)
VALUES ('Admin','admin@example.com','<bcrypt>',0,0,0,0,0,1,1,
        (SELECT id FROM relationship_seeds WHERE code='ADMIN'), now(), now());
```

## 7. Common build and run problems

| Symptom | Cause | Fix |
| --- | --- | --- |
| `No matching toolchains found for requested specification: {languageVersion=17}` | No JDK 17 | Install JDK 17 and set `org.gradle.java.installations.paths` |
| `JWT_SECRET must be at least 32 characters` | Weak/short secret | Use a longer random value |
| `Unable to connect to the database` | Wrong `DB_URL`/credentials or unreachable host | Check the connection string and network |
| Chat endpoints fail at startup | Chat datasource misconfigured | Verify `SUPABASE_DB_*` |
| `invalid_grant` in logs | Gmail refresh token revoked | Re-authorise and update `GMAIL_REFRESH_TOKEN` |
| Emails not sent but no error | `MAIL_*`/Gmail not configured | Set credentials; check `email_logs` |
| Port 8080 already in use | Another instance running | Stop it or set `server.port` |

> Starting the app against production data starts the schedulers. Use a separate database, or stop
> the app before the wish times, when testing.

## 8. Useful commands

```bash
./gradlew clean test bootJar          # full build
./gradlew test --tests '*CircleServiceImplTest'
./gradlew dependencies                # dependency tree
./gradlew dependencyInsight --dependency ehcache
```
