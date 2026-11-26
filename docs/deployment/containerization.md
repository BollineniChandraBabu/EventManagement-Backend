# Containerization

## 1. Dockerfile

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

| Aspect | Detail |
| --- | --- |
| Stages | Two: build (Gradle + JDK 17) and runtime (JRE 17 only) |
| Build command | `gradle --no-daemon clean bootJar` |
| Artifact | `build/libs/family-wishes-1.0.0.jar` → `/app/app.jar` |
| Profile | `prod` baked into the entrypoint |
| Port | 8080 |
| User | Default (root) — a non-root user is a recommended improvement |
| Health | The container has no `HEALTHCHECK`; Render and UptimeRobot probe `/health` |

## 2. Building

```bash
docker build -t family-wishes:1.0.0 .
docker build -t family-wishes:latest .
```

The build downloads Gradle dependencies each time unless you mount a cache:

```bash
docker build --build-arg GRADLE_USER_HOME=/tmp/gradle-cache -t family-wishes:1.0.0 .
```

For faster local iteration, build the jar first and copy it:

```bash
./gradlew bootJar
docker build -f Dockerfile.local -t family-wishes:dev .   # optional variant
```

## 3. Running

```bash
docker run -d --name family-wishes \
  -p 8080:8080 \
  --env-file .env \
  --restart unless-stopped \
  family-wishes:1.0.0

docker logs -f family-wishes
curl -s localhost:8080/health
```

Minimal run without a `.env` file:

```bash
docker run --rm -p 8080:8080 \
  -e DB_URL='jdbc:postgresql://host:5432/db' \
  -e DB_USERNAME=user -e DB_PASSWORD=pass \
  -e JWT_SECRET='a-very-long-random-secret-value-32+' \
  family-wishes:1.0.0
```

## 4. Image characteristics

| Property | Value |
| --- | --- |
| Base (runtime) | `eclipse-temurin:17-jre-alpine` (small, glibc-free) |
| Approximate size | ~200 MB with the fat jar |
| Shell | `sh` available (no bash) |
| Time zone data | Included in the JRE; the app sets `Asia/Kolkata` explicitly |
| Writable paths | `/tmp` only; no persistent volumes needed |
| Signals | SIGTERM handled by the JVM shutdown hook |

## 5. Configuration inside the container

* All configuration is environment variables; nothing is baked in except the profile name.
* Do not `COPY` a `.env` into the image — it would end up in an image layer.
* `-Dspring.profiles.active=prod` is fixed in the entrypoint; override with
  `-e SPRING_PROFILES_ACTIVE=...` only if you also change the entrypoint.

## 6. Docker Compose (local convenience)

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: familywishes
    ports: ['5432:5432']
  app:
    build: .
    ports: ['8080:8080']
    env_file: .env
    environment:
      DB_URL: jdbc:postgresql://db:5432/familywishes
    depends_on: [db]
```

## 7. Recommended improvements

1. **Non-root user**: `RUN addgroup -S app && adduser -S app -G app` and `USER app`.
2. **Layered jar**: use Spring Boot's layered jar extraction so dependency layers are cached.
3. **`.dockerignore`**: exclude `build/`, `.git/`, `.idea/`, `docs/` to shrink the context.
4. **Healthcheck**: `HEALTHCHECK CMD wget -qO- http://localhost:8080/health || exit 1` (Alpine has
   `wget`).
5. **JVM flags**: `-XX:MaxRAMPercentage=75` so the heap follows the container limit.
6. **Pin the base image digest** for reproducible builds.
7. **Version-agnostic jar copy**: `COPY --from=build /app/build/libs/*.jar app.jar` so a version bump
   does not break the build.

## 8. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| `COPY failed: ... family-wishes-1.0.0.jar` | Version changed in `build.gradle` | Update the `Dockerfile` path or use a wildcard |
| Container exits immediately | Missing `DB_URL`/`JWT_SECRET` | Check `docker logs` |
| `Connection refused` to the database | `localhost` inside the container is the container | Use the host name or `host.docker.internal` |
| Out of memory | Container limit too low for the heap | Raise memory or set `-XX:MaxRAMPercentage` |
| Slow start | Schema update and Quartz init | Expected on the first run |
