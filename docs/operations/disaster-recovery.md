# Disaster recovery

## 1. Scope

Disasters that could take the service down or destroy data:

| Scenario | Likelihood | Impact |
| --- | --- | --- |
| Render region outage | Low | Service unavailable |
| Neon outage | Low | No sign-in, no wishes |
| Supabase outage | Low | Chat and media unavailable; wishes still send |
| Gmail/Google outage | Low | No email |
| Gemini outage | Medium | Wishes degraded (fallback, then 503) |
| Calendarific outage | Medium | Festival data stale |
| Accidental data deletion | Medium | Data loss |
| Credential compromise | Low | Data exposure |
| Corrupted deployment | Medium | Service down after deploy |

## 2. Recovery objectives

| Metric | Target |
| --- | --- |
| RTO (service restored) | 2 hours |
| RPO (data loss) | 24 hours |
| Communication to users | 30 minutes after detection |

## 3. Decision tree

```
Is the service reachable?
├── No  → Is it a deploy?  → Roll back (deployment/rollback-plan.md)
│        Is it the platform? → Wait or redeploy; check status pages
│        Is it the database? → Check Neon; restore if corrupt
└── Yes → Is data correct?
          ├── No  → Stop affected jobs, restore from backup
          └── Yes → Is email working? → Check Gmail credentials
```

## 4. Scenario playbooks

### 4.1 Render outage

1. Check the Render status page.
2. If prolonged, deploy the same image to another host (Docker on a VM or another PaaS).
3. Point DNS at the new host.
4. Update UptimeRobot.
5. Communicate the temporary URL.

### 4.2 Primary database outage

1. Check the Neon status page.
2. If the instance is unavailable, wait; if the data is corrupt, restore from backup
   ([backup-and-recovery.md](backup-and-recovery.md)).
3. Consider restoring into a new Neon branch and pointing `DB_URL` at it.
4. Disable the schedulers until the data is verified (they would send wishes from partial data).

### 4.3 Chat database outage

1. Chat endpoints fail; everything else works.
2. Communicate that chat is temporarily unavailable.
3. Restore or wait for Supabase.
4. Verify with `GET /api/chat/unread-count`.

### 4.4 Storage outage

1. Uploads fail; emails send without images.
2. Wait for Supabase; optionally switch the bucket endpoint if a replica exists.
3. Re-upload lost objects if the data is important.

### 4.5 Email provider outage

1. `email_logs` fills with `FAILED`.
2. Wishes are delayed; the retry logic sends them later.
3. If the outage is long, note the missed dates and send a manual follow-up.
4. Consider switching to SMTP (`EmailServiceImpl`) or Brevo temporarily.

### 4.6 AI outage

1. The model fallback tries other models.
2. If all fail, the scheduler logs the failure and skips the recipient.
3. Trigger the job again later; wishes for that day can still be sent if the guard allows.
4. As a last resort, use the non-agentic path (`AGENT_WISH_ENABLED=false`).

### 4.7 Credential compromise

1. Rotate `JWT_SECRET` (all sessions end).
2. Rotate database and storage credentials.
3. Revoke the Gmail refresh token and re-authorise.
4. Rotate AI keys.
5. Review `violated_users`, `login_location_events` and access logs.
6. Notify affected members.
7. Follow [incident-management.md](incident-management.md).

### 4.8 Data corruption or accidental deletion

1. Stop the application to prevent further writes.
2. Identify the scope (which table, when).
3. Restore the whole database or the affected table from the latest dump.
4. Reconcile with the chat database and storage if needed.
5. Restart and verify.

## 5. Communication plan

| Audience | Channel | Content |
| --- | --- | --- |
| Family members | The announcement feature or the family chat | What is unavailable, what to expect |
| Operator | Email/phone | Technical status |
| Backend owner | Email/phone | Technical detail and actions |

Message template:

> The greeting service is temporarily unavailable because of a problem at our hosting provider. Your
> data is safe. We expect to be back within two hours and will send any missed wishes afterwards.

## 6. Post-incident

1. Write a short timeline (detection → mitigation → resolution).
2. Identify the root cause.
3. Record follow-up actions with owners.
4. Update this document and the runbook.
5. Add monitoring so the same failure is detected earlier.

## 7. Testing the plan

| Test | Frequency |
| --- | --- |
| Restore a backup into a scratch database | Quarterly |
| Roll back a deployment | Quarterly |
| Simulate an AI outage (invalid key) | Yearly |
| Simulate a Gmail failure (revoked token) | Yearly |

## 8. Dependencies on single providers

| Dependency | Single point of failure? | Mitigation |
| --- | --- | --- |
| Render | Yes | Docker image runs anywhere |
| Neon | Yes | Provider backups; restore to a branch |
| Supabase | Yes | Separate database; chat degrades gracefully |
| Gmail | Yes | SMTP and Brevo alternatives exist |
| Gemini | Partly | Model fallback plus a non-agentic path |
| Calendarific | Yes | Festival data is cached in the database |
