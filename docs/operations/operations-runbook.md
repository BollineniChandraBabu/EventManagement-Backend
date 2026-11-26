# Operations runbook

Daily, weekly and monthly procedures for keeping the service healthy. Each task says what to check,
how, and what to do if it is wrong.

## 1. Daily

| Task | How | If wrong |
| --- | --- | --- |
| Service is up | `curl -s https://<host>/health` or UptimeRobot | See [troubleshooting-guide.md](troubleshooting-guide.md) §1 |
| Morning wish run completed | `GET /api/schedulers/dailyWishJob` and `birthdayJob` | Check logs; trigger manually after fixing the cause |
| Emails actually sent | `GET /api/emails/status?page=0&size=20` as admin | Look at `error_message`; check the Gmail token |
| Failed email count | `GET /api/dashboard/mail` → `failedEmails` | Retry via the daily job or the delivery-ops agent |
| Provider errors in logs | Search for `WARN`/`ERROR` in the last 24 hours | Follow the matching section in the troubleshooting guide |

## 2. Weekly

| Task | How |
| --- | --- |
| Review failed sign-ins | `GET /api/dashboard/violated-users` for the week |
| Review delivery failures | Filter `email_logs` by `FAILED` and the reason |
| Check scheduler statistics | `GET /api/schedulers` (runs, failures, next fire times) |
| Check storage growth | Supabase dashboard |
| Check database size and connections | Neon dashboard |
| Review open defects | [../testing/defect-tracking.md](../testing/defect-tracking.md) |

## 3. Monthly

| Task | How |
| --- | --- |
| Confirm the festival sync ran | `GET /api/festivals?month=<next month>` |
| Verify the Gmail refresh token | `POST /api/email/test` |
| Verify AI keys and quotas | `POST /api/ai/generate-wish`, `GET /api/ai/pollinations/balance` |
| Check dependency advisories | [../security/vulnerability-management.md](../security/vulnerability-management.md) |
| Review backups | Confirm the latest backup exists and can be listed |
| Rotate credentials if due | [../deployment/secrets-management.md](../deployment/secrets-management.md) |

## 4. Quarterly

| Task | How |
| --- | --- |
| Restore test | Restore a backup into a scratch database and start the app against it |
| Rollback drill | [../deployment/rollback-plan.md](../deployment/rollback-plan.md) §10 |
| Access review | Who has Render, Neon and Supabase access |
| Documentation review | This set of docs still matches the code |

## 5. Common operations

### Trigger a job manually

```bash
curl -X POST "$BASE/api/schedulers/dailyWishJob/trigger" -H "Authorization: Bearer $ADMIN"
```

### Retry failed emails

The daily wish job calls `retryFailed()`. To retry immediately, trigger `dailyWishJob`.

### Re-sync festivals

```bash
curl -X POST "$BASE/api/schedulers/festivalMonthlySyncScheduler/trigger" -H "Authorization: Bearer $ADMIN"
```

### Re-authorise Gmail

1. Open `/google/login` on the deployed host.
2. Approve consent.
3. Copy the new refresh token from the callback output.
4. Update `GMAIL_REFRESH_TOKEN` in Render and restart.

### Deactivate a user

```bash
curl -X POST "$BASE/api/users/42/deactivate" -H "Authorization: Bearer $ADMIN"
```

### Publish an announcement

```bash
curl -X POST "$BASE/api/notifications" -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Diwali wishes","message":"Happy Diwali!","canSendEmail":true}'
curl -X POST "$BASE/api/notifications/1/publish" -H "Authorization: Bearer $ADMIN"
```

### Check chat health

```bash
curl -s "$BASE/api/chat/unread-count" -H "Authorization: Bearer $TOKEN"
```

## 6. Routine health thresholds

| Metric | Healthy | Investigate | Act |
| --- | --- | --- | --- |
| `/health` | 200 in < 300 ms | > 1 s | Check database and container |
| Failed emails today | 0 | 1–5 | Check the provider |
| Failed emails today | — | > 5 | Fix credentials, alert, consider disabling jobs |
| Wish job duration | < 5 minutes | 5–15 minutes | Check AI latency |
| Scheduler failures | 0 | 1 | Read the error, re-trigger |
| Database connections | < 8 of 10 | 8–9 | Investigate leaks |
| Storage growth | < 50 MB/month | > 200 MB/month | Review uploads |

## 7. Escalation

| Situation | Escalate to |
| --- | --- |
| Service down > 15 minutes | Operator → incident ([incident-management.md](incident-management.md)) |
| Data loss suspected | Operator, stop the affected job, restore |
| Security issue | Backend owner, treat as P1 |
| Provider outage | Note it, wait, communicate |

## 8. Log locations

| Log | Where |
| --- | --- |
| Application | Render service logs |
| Build | Render build logs |
| Database | Neon and Supabase dashboards |
| Email | `email_logs` (query) and the Gmail account's sent folder |
| Uptime | UptimeRobot dashboard |
