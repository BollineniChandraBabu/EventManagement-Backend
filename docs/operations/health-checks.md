# Health checks

## 1. Application endpoint

```
GET /health        (public, no token)
200 {"status":"UP","time":"2026-11-26T09:15:42.123456"}
```

| Property | Value |
| --- | --- |
| Implementation | `HealthController` |
| Dependencies checked | **None** |
| Response time | A few milliseconds |
| Authentication | None |
| Use | Uptime monitoring, container probes |

**What it proves:** the JVM is running, Tomcat accepts connections and the Spring context is up.

**What it does not prove:** the database is reachable, the chat database is up, Gmail works, or the
schedulers are running. A database outage does not flip this endpoint to DOWN.

## 2. Recommended deeper checks

| Check | How | What it proves |
| --- | --- | --- |
| Primary database | `GET /api/themes` with a token | JPA and the primary DB work |
| Chat database | `GET /api/chat/unread-count` | Chat datasource works |
| Scheduler | `GET /api/schedulers` | Quartz is running, jobs registered |
| Email | `GET /api/emails/status?page=0&size=1` | Email log queries work |
| Storage | `GET /api/emails/status/{id}/image` for an email with an image | Storage download works |
| AI | `POST /api/ai/generate-wish` | Gemini reachable (costs quota) |

Suggested readiness endpoint (not implemented):

```java
@GetMapping("/health/ready")
public ResponseEntity<Map<String,Object>> ready() {
  boolean db = themeRepository.count() >= 0;     // triggers a query
  return db ? ResponseEntity.ok(Map.of("status","UP"))
            : ResponseEntity.status(503).body(Map.of("status","DOWN","reason","database"));
}
```

## 3. Scheduler health

`GET /api/schedulers` returns, per job: `running`, `totalRuns`, `successRuns`, `failedRuns`,
`lastStartedAt`, `lastCompletedAt`, `lastDurationMs`, `lastStatus`, `lastError`, `nextFireTime`,
`previousFireTime`.

| Signal | Healthy | Act |
| --- | --- | --- |
| `failedRuns` increasing | 0 | Read `lastError`, fix, re-trigger |
| `nextFireTime` null | Set | The trigger may be misconfigured |
| `running=true` for hours | false | The job may be stuck; check logs |
| No runs after a restart | Runs appear after the next fire time | Trigger manually to confirm |

Note: statistics are in memory and reset on restart.

## 4. External monitoring (UptimeRobot)

| Setting | Value |
| --- | --- |
| Monitor type | HTTP(S) |
| Name | `family-wishes-backend-prod` |
| URL | `https://eventmanagement-backend-ka9x.onrender.com/health` |
| Interval | 5 minutes |
| Timeout | 30 seconds |
| Keyword | `"status":"UP"` |
| Alerts | Email on DOWN and on UP |

Limitations: a 5-minute interval means up to 5 minutes of undetected downtime; Render's free tier
sleeps when idle, so a slow first response may trigger a false alert — increase the timeout to 60 s
if that happens.

## 5. Health indicators to watch

| Indicator | Healthy range | Where |
| --- | --- | --- |
| `/health` latency | < 300 ms | UptimeRobot |
| Failed emails today | 0 | `/api/dashboard/mail` |
| Emails sent today | > 0 on a day with wishes | `/api/dashboard/mail` |
| Scheduler failures | 0 | `/api/schedulers` |
| Wish job duration | < 5 minutes | `/api/schedulers/dailyWishJob` |
| Database connections | < 8 of 10 | Neon |
| Chat connections | < 8 of 10 | Supabase |
| Storage growth | < 50 MB/month | Supabase |
| Container memory | < 80% of limit | Render |

## 6. Alert thresholds

| Condition | Severity | Action |
| --- | --- | --- |
| `/health` DOWN twice in a row | High | Investigate immediately |
| Failed emails > 5 in a day | High | Check Gmail credentials and AI |
| Wish job failed | High | Fix and re-trigger |
| No emails sent on a day with birthdays | Medium | Check the scheduler and settings |
| Festival sync failed | Medium | Re-trigger |
| Storage near quota | Medium | Clean up or upgrade |
| Database connections > 80% | Medium | Investigate leaks, raise the pool |

## 7. Manual health review (weekly)

```bash
BASE=https://eventmanagement-backend-ka9x.onrender.com
curl -s $BASE/health
curl -s $BASE/api/schedulers -H "Authorization: Bearer $ADMIN"
curl -s $BASE/api/dashboard/mail -H "Authorization: Bearer $ADMIN"
curl -s "$BASE/api/emails/status?page=0&size=10" -H "Authorization: Bearer $ADMIN"
```

## 8. Improving health checks (roadmap)

1. Add `/health/ready` that pings both databases.
2. Expose Micrometer metrics and a Prometheus endpoint.
3. Add a synthetic check that signs in and reads the calendar daily.
4. Alert on the absence of the daily wish run rather than only on HTTP status.
