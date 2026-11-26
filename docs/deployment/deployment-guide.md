# Deployment guide

## 1. Target platform

Production runs as a single Docker container on **Render**, built from the repository `Dockerfile`,
talking to Neon (primary database), Supabase (chat database and storage) and the external providers.

| Item | Value |
| --- | --- |
| Service URL | `https://eventmanagement-backend-ka9x.onrender.com` |
| Build | Render builds the `Dockerfile` on each deploy |
| Start command | `java -Dspring.profiles.active=prod -jar app.jar` (baked into the image) |
| Port | 8080 (Render routes HTTPS to it) |
| Health check | `/health` |
| Instances | 1 |

## 2. Prerequisites for a deployment

1. All required environment variables are set in Render
   ([secrets-management.md](secrets-management.md)).
2. The primary database is reachable and has a recent backup.
3. The chat database and storage bucket are reachable.
4. The change is merged, tests pass, and release notes are drafted.
5. The previous image/tag is recorded for rollback ([rollback-plan.md](rollback-plan.md)).

## 3. Deployment steps

### 3.1 Prepare

```bash
git checkout main && git pull
./gradlew clean test          # must be green
./gradlew bootJar             # optional local sanity build
```

Take a database backup:

```bash
pg_dump "$DB_URL" -Fc -f backup-$(date +%F-%H%M).dump
```

### 3.2 Deploy

**Option A — Render auto-deploy (default).** Merging to the deployed branch triggers a build and
deploy. Watch the build log and then the service log for `Started FamilyWishesApplication`.

**Option B — manual redeploy.** In the Render dashboard choose *Manual Deploy → Deploy latest
commit*.

**Option C — local image (for QA or a self-hosted host).**

```bash
docker build -t family-wishes:1.0.0 .
docker run -d --name family-wishes -p 8080:8080 --env-file .env family-wishes:1.0.0
docker logs -f family-wishes
```

### 3.3 Verify

| Step | Command | Expected |
| --- | --- | --- |
| Health | `curl -s $BASE/health` | `{"status":"UP",…}` |
| OpenAPI | `curl -s -o /dev/null -w '%{http_code}' $BASE/v3/api-docs` | `200` |
| Sign-in | `POST /api/auth/login` with an admin account | 200 tokens |
| Schedulers | `GET /api/schedulers` | Nine jobs with next fire times |
| Themes | `GET /api/themes` | 11 themes (seeds initialised) |
| Email | `POST /api/email/test` | Email arrives |
| Storage | `GET /api/emails/status?page=0&size=5` | Rows with image URLs resolve |
| Chat | `GET /api/chat/unread-count` | 200 |

### 3.4 Post-deploy

1. Watch logs for 15 minutes (startup errors, provider failures).
2. Confirm UptimeRobot reports the service up.
3. Announce the release ([../project/release-notes.md](../project/release-notes.md)).
4. If anything is wrong, follow [rollback-plan.md](rollback-plan.md).

## 4. Startup sequence and what can fail

| Step | Failure symptom | Action |
| --- | --- | --- |
| Load properties | Startup aborts with a missing property message | Fix the environment variable |
| Primary datasource | `Unable to connect` | Check `DB_URL`, Neon status |
| Hibernate schema update | Startup slow or fails on DDL | Review the last entity change |
| Chat schema initializer | Chat tables missing | Check `SUPABASE_DB_*` |
| Quartz init | `QRTZ_*` errors | Check database permissions |
| Gmail client | Startup fails if credentials are malformed | Verify the OAuth values |
| Tomcat bind | Port in use | Render handles this; locally stop the other process |

## 5. Zero-downtime notes

* A single instance means a short outage during deploys (Render restarts the container).
* Jobs interrupted mid-run are handled by Quartz misfire policies; a wish that did not send may need
  a manual trigger.
* Schema changes are applied at startup; a long `ALTER TABLE` delays availability.

## 6. Rollback

See [rollback-plan.md](rollback-plan.md). In short: redeploy the previous commit/image, and restore
the database only if a destructive change shipped.

## 7. Deployment checklist

- [ ] Tests green, release notes drafted
- [ ] Backup taken
- [ ] Environment variables unchanged or updated deliberately
- [ ] Previous version recorded
- [ ] Deploy completed and startup log clean
- [ ] Smoke checks passed
- [ ] Scheduler list correct, next fire times sane
- [ ] Monitoring green for 15 minutes
- [ ] Release notes published
