# Monitoring and alerting

## 1. What is monitored today

| Signal | Tool | Interval |
| --- | --- | --- |
| HTTP availability (`/health`) | UptimeRobot | 5 minutes |
| Container restarts and logs | Render | Continuous |
| Database health | Neon dashboard | Manual |
| Chat database and storage | Supabase dashboard | Manual |
| Email failures | `email_logs` + `/api/dashboard/mail` | Manual / daily |
| Scheduler runs | `/api/schedulers` | Manual / daily |
| Instagram failures | `EmailAlertScheduler` (email when > 5 failed today) | Every 30 minutes |
| Failed email triage | `DeliveryOpsAgentScheduler` (when enabled) | Twice hourly |

There is **no** metrics backend, no dashboards, and no log aggregation beyond Render's log viewer.

## 2. Metrics that should be collected

| Metric | Type | Why |
| --- | --- | --- |
| `http_server_requests_seconds` | histogram | Latency and error rate |
| `hikaricp_connections_active` | gauge | Pool saturation |
| `jvm_memory_used_bytes` | gauge | Memory pressure |
| `emails_sent_total{type}` | counter | Delivery volume |
| `emails_failed_total{type}` | counter | Delivery health |
| `scheduler_runs_total{job,status}` | counter | Job reliability |
| `scheduler_duration_seconds{job}` | histogram | Job latency |
| `ai_requests_total{model,outcome}` | counter | Provider health |
| `chat_messages_total` | counter | Chat volume |
| `storage_bytes` | gauge | Quota tracking |

These are not exposed today. Adding Micrometer plus a Prometheus registry would produce them with
little code.

## 3. Alert catalogue

| ID | Condition | Severity | Channel | Runbook |
| --- | --- | --- | --- | --- |
| AL-01 | `/health` fails twice consecutively | High | UptimeRobot email | [troubleshooting-guide.md](troubleshooting-guide.md) §1 |
| AL-02 | More than 5 failed emails in a day | High | Email (Instagram alert) and delivery-ops agent | §3 |
| AL-03 | `dailyWishJob` or `birthdayJob` last status FAILED | High | Manual check, then agent alert | §4 |
| AL-04 | No email sent on a day with at least one birthday | Medium | Manual | §4 |
| AL-05 | Festival sync failed | Medium | Manual | §7 |
| AL-06 | Database connections > 80% | Medium | Provider alert | §1 |
| AL-07 | Storage usage > 80% of quota | Medium | Provider alert | §8 |
| AL-08 | AI provider failing for > 1 hour | Medium | Manual from logs | §5 |
| AL-09 | Many `CONCURRENT_LOGIN` rejections | Low | Manual | Expected behaviour |
| AL-10 | Spike in `violated_users` rows | Medium | Manual | Possible attack |

## 4. Notification routing

| Severity | Who | How | Response |
| --- | --- | --- | --- |
| High | Operator + backend owner | Email, then phone if unresolved in 15 minutes | Immediate |
| Medium | Operator | Email | Same day |
| Low | Backend owner | Email or backlog | Next release |

## 5. Application alert emails

| Alert | Trigger | Recipient |
| --- | --- | --- |
| Failed-login threshold reached | 5 wrong passwords | The account owner |
| Unknown login attempt | 5 rows in `violated_users` for an email | `alert.email.to` |
| Wish failure | An email send failed in a scheduler | `alert.email.to` |
| Instagram failure digest | More than 5 failures today | `alert.email.to` |
| Delivery-ops report | Agent run with failures | Administrator (via the agent) |

## 6. Log-based monitoring (today's substitute)

Search Render logs for:

| Pattern | Meaning |
| --- | --- |
| `ERROR` | Any failure; read the stack trace |
| `mail send failed` | Delivery problem |
| `invalid_grant` | Gmail token revoked |
| `Gemini model` | Provider fallback events |
| `Calendarific sync failed` | Festival sync problem |
| `IP lookup failed` | Geolocation quota |
| `Started FamilyWishesApplication` | Successful startup |

## 7. Dashboard queries (SQL)

```sql
-- Emails in the last 24 hours by status
SELECT status, COUNT(*) FROM email_logs
WHERE created_at >= now() - interval '1 day' GROUP BY status;

-- Failures by message
SELECT error_message, COUNT(*) FROM email_logs
WHERE status = 2 AND created_at >= now() - interval '7 days'
GROUP BY error_message ORDER BY 2 DESC;

-- Sign-in volume
SELECT date_trunc('day', logged_in_at) AS day, COUNT(*) FROM login_location_events
GROUP BY 1 ORDER BY 1 DESC LIMIT 14;

-- Failed attempts by IP
SELECT ip_address, COUNT(*) FROM violated_users
WHERE logged_in_at >= now() - interval '7 days'
GROUP BY 1 ORDER BY 2 DESC LIMIT 10;
```

## 8. Escalation and communication

1. Acknowledge the alert within 15 minutes during waking hours.
2. Post the status in the agreed channel (family group or operator chat).
3. If the service is down more than 30 minutes, follow
   [incident-management.md](incident-management.md).
4. After resolution, record what happened and what changed.

## 9. Gaps and roadmap

| Gap | Recommendation |
| --- | --- |
| No metrics | Add Micrometer + Prometheus or a hosted APM |
| No log aggregation | Export Render logs to a log service |
| No synthetic transaction | Add a daily sign-in and calendar read |
| Alerts depend on email | Add a second channel (chat or SMS) |
| Scheduler failures not alerted automatically | Add an alert when `lastStatus=FAILED` |
| No dashboard | Build a small status page from the APIs |
