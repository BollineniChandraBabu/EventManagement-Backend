# Logging and monitoring

## 1. Logging configuration

| Property | Value |
| --- | --- |
| `logging.level.root` | `ERROR` |
| `logging.level.org.springframework` | `WARN` |
| `logging.level.org.hibernate` | `ERROR` |
| `logging.level.com.familywishes` | `INFO` |
| Output | stdout (Render captures it) |
| Format | Spring Boot default (`timestamp level logger - message`) |

`@Slf4j` (Lombok) provides the logger. There is no file appender, no JSON layout and no correlation
id.

## 2. What is logged, and at which level

| Event | Level | Message shape |
| --- | --- | --- |
| Job start/end | INFO | `BIRTHDAY JOB TRIGGERED: <date>`, `… COMPLETED: <date>`, counts |
| Wish sent | INFO | `Birthday wish sent to <email>` |
| Wish skipped (already sent) | INFO | `Already sent birthday wish to <email>` |
| Email sent | INFO | `Email sent successfully to <email>` |
| Email failed | ERROR | `mail send failed` + stack trace |
| Gemini model failure | WARN | `Gemini model <m> failed (HTTP 429), trying another model` |
| Gemini catalogue refresh | INFO | `Gemini models available for fallback: [...]` |
| Gemini catalogue failure | WARN | `Could not list Gemini models, keeping the previous list` |
| Pollinations failure | WARN | `Pollinations image generation failed for URL <u> with status <s>` |
| Calendarific skipped | WARN | `Skipping Calendarific sync because calendarific.api.key is missing` |
| Calendarific failure | ERROR | `Calendarific sync failed` |
| On-demand festival fetch | INFO | `Fetched <n> on-demand festival entries for <year>` |
| IP lookup failure | WARN | `IP lookup failed for <ip>: <msg>` |
| Circle notification failure | ERROR | `Circle request notification failed for circle <id>` |
| Circle email sent | INFO | `Circle email queued for <email> (circle <id>)` |
| Reminder summary | INFO | `Sent <n> pending circle-request reminders (older than <d> days)` |
| Refresh token cleanup | INFO | `Refresh token cleanup completed. Deleted <n> expired token(s) at <ts> IST.` |
| Unread chat mail run | INFO | `Unread chat mail scheduler executed in zone <zone>` |
| Scheduler job failure | ERROR | Exception + `SchedulerTrackingService` records `lastStatus=FAILED` |
| Delivery ops run | INFO | `DeliveryOpsAgent dryRun=<b> trace=[...]` and the report |
| Gmail token revoked | ERROR | Instruction to regenerate `GMAIL_REFRESH_TOKEN` |
| Chat schema init | INFO | `Supabase chat schema initialized` |

Legacy `System.out.println` calls remain in `GoodMorningScheduler`, `GoodNightScheduler`,
`InstagramController` and `NotificationServiceImpl`; they duplicate or replace log lines.

## 3. What is deliberately not logged

* Passwords (except the legacy `violated_users` column, which is not logged).
* Tokens (access, refresh, OTP, reset, direct login).
* Email bodies (they are stored in `email_logs` instead).
* Recipient addresses in the delivery-ops agent's tool output (explicitly withheld from the model).

## 4. Health endpoint

`GET /health` (public):

```json
{ "status": "UP", "time": "2026-11-26T09:15:42.123456" }
```

It performs no dependency checks; it only proves the process is serving requests. A database outage
would not flip it to DOWN. See [../operations/health-checks.md](../operations/health-checks.md).

## 5. Scheduler observability

`SchedulerTrackingService` keeps per-job statistics in memory:

| Field | Meaning |
| --- | --- |
| `running` | A run is in progress |
| `totalRuns`, `successRuns`, `failedRuns` | Counters since start |
| `lastStartedAt`, `lastCompletedAt`, `lastDurationMs` | Timing |
| `lastStatus`, `lastError` | Last outcome |

Exposed with next/previous fire times by `SchedulerManagementService` at `GET /api/schedulers` and
`GET /api/schedulers/{jobName}`. Statistics reset on restart.

## 6. Business observability (data, not metrics)

| Source | What it answers |
| --- | --- |
| `email_logs` | What was sent, when, to whom, with what status and error |
| `GET /api/emails/status` | Paged view, filterable by tab/status/type |
| `GET /api/dashboard/mail` | Total users, upcoming events, sent today, failed |
| `GET /api/dashboard/chart/mail` | Daily sent/failed/total |
| `GET /api/dashboard/insta` | Instagram message counters |
| `GET /api/dashboard/violated-users` | Failed sign-in patterns |
| `GET /api/dashboard/chart/login-locations` | Where sign-ins came from |
| `GET /api/agent/status` | Agent flags and current failed-email count |
| `message_log` | Instagram send attempts and retries |

## 7. External monitoring

| Tool | Configuration |
| --- | --- |
| UptimeRobot | HTTP(S) monitor `family-wishes-backend-prod`, URL `https://eventmanagement-backend-ka9x.onrender.com/health`, interval 5 minutes, timeout 30 s, keyword `"status":"UP"`, alerts on DOWN and UP |
| Render | Platform logs and restart events |
| Neon / Supabase | Provider dashboards for database health, connections and storage usage |

## 8. Gaps and recommendations

1. **No metrics endpoint.** `management.endpoints.web.exposure.include=*` exposes whatever Actuator
   provides, but no Micrometer registry (Prometheus) is configured.
2. **Health does not check dependencies.** Add a readiness probe that pings the primary database.
3. **No correlation id.** Requests cannot be traced end to end across log lines.
4. **Logs are unstructured.** JSON logging would make them queryable.
5. **In-memory scheduler stats.** Persist or export them.
6. **`root=ERROR` hides framework warnings** that are often the first sign of trouble; consider
   `WARN` in production with sampling.
7. **No alerting on email failure volume** except the Instagram alert (>5 failures per 30 minutes)
   and the delivery-ops agent.
