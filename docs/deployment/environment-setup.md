# Environment setup

## 1. What you need

| Item | Required | Notes |
| --- | --- | --- |
| JDK 17 | Yes | Toolchain for compilation |
| JDK 21+ | Optional | To launch Gradle |
| Gradle 8.14 | Yes (or the Docker image) | The wrapper is not committed |
| PostgreSQL | Yes | Local instance or a hosted one |
| Gmail account with OAuth | For email features | Requires the Gmail API enabled |
| Google AI Studio key | For AI features | Gemini |
| Supabase project | For storage and chat | Bucket plus S3 keys plus the database |
| Calendarific key | For festivals | Free plan is enough |
| Angular UI (optional) | For end-to-end work | Separate repository |

## 2. Install the JDKs

```bash
# Debian/Ubuntu example
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk

# macOS
brew install openjdk@17
```

Confirm:

```bash
java -version
```

Point Gradle at JDK 17 in `~/.gradle/gradle.properties`:

```properties
org.gradle.java.installations.paths=/usr/lib/jvm/java-17-openjdk-amd64
```

## 3. Install Gradle

Either install Gradle 8.14 locally or use the Docker build image, which already contains it:

```bash
docker run --rm -it -v "$PWD":/app -w /app gradle:8.14.3-jdk17-alpine gradle --no-daemon test
```

## 4. Local PostgreSQL

```bash
createdb familywishes

# or with Docker
docker run --rm -d --name fw-pg -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=familywishes postgres:16-alpine
```

Hibernate creates the schema on first start (`ddl-auto=update`).

## 5. Gmail OAuth (one-time)

1. In Google Cloud Console create a project, enable **Gmail API**.
2. Create **OAuth client ID** (Web application) with redirect URI
   `http://localhost:8080/oauth2callback` (and the production URI).
3. Note the client id and secret.
4. Start the app with the client id/secret set.
5. Open `http://localhost:8080/google/login`, approve the `gmail.send` scope.
6. The callback prints the tokens; copy the refresh token into `GMAIL_REFRESH_TOKEN`.
7. Verify with `POST /api/email/test`.

## 6. Gemini

1. Create an API key in Google AI Studio.
2. Set `GEMINI_API_KEY`.
3. Set `GEMINI_API_URL` to a base URL **including** the model, for example
   `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash`.
4. Verify with `POST /api/ai/generate-wish`.

## 7. Supabase

1. Create a project.
2. Storage: create a bucket (default `golden-greetings`), generate S3 access keys, note the S3
   endpoint and region.
3. Database: note the connection string; the chat schema is created at startup.
4. Optional: upload `signature.png` and the default profile pictures.

## 8. Calendarific

1. Register and copy the API key.
2. Set `CALENDARIFIC_API_KEY` (country defaults to `IN`).
3. Verify by triggering `POST /api/schedulers/festivalMonthlySyncScheduler/trigger`.

## 9. IDE setup

| IDE | Notes |
| --- | --- |
| IntelliJ IDEA | Import as a Gradle project; set the project SDK to 17; enable annotation processing for Lombok; install the Spotless plugin (optional) |
| VS Code | Java Extension Pack; set `java.configuration.runtimes` to include 17 |
| Eclipse | Buildship; Lombok must be installed into the IDE |

## 10. Verification checklist

| Step | Command | Expected |
| --- | --- | --- |
| Compile | `./gradlew compileJava` | BUILD SUCCESSFUL |
| Tests | `./gradlew test` | All green |
| Start | `./gradlew bootRun` | `Started FamilyWishesApplication` |
| Health | `curl localhost:8080/health` | `UP` |
| Docs | Open `/swagger-ui.html` | UI renders |
| Sign-in | `POST /api/auth/login` | Tokens |
| Mail | `POST /api/email/test` | Email arrives |
| AI | `POST /api/ai/generate-wish` | JSON wish |

## 11. Troubleshooting the setup

| Problem | Fix |
| --- | --- |
| Gradle cannot find JDK 17 | Set `org.gradle.java.installations.paths` |
| Lombok errors in the IDE | Enable annotation processing |
| `psql: command not found` | Install the PostgreSQL client |
| Gmail `access_denied` | The OAuth consent screen is in testing mode; add your account as a test user |
| Supabase S3 403 | Keys must be the S3 access keys, not the anon/service key |
| Chat schema not created | Check `SUPABASE_DB_*` and the startup log |
