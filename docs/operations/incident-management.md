# Incident management

## 1. What counts as an incident

| Type | Example |
| --- | --- |
| Availability | Service down or `/health` failing |
| Delivery | Wishes not sent for a day |
| Data | Data loss, corruption, unauthorised change |
| Security | Credential leak, unauthorised access, abuse |
| Privacy | Personal data exposed to the wrong member |

A single member's forgotten password is **not** an incident; use
[support-guide.md](support-guide.md).

## 2. Severity

| Severity | Definition | Examples | Response |
| --- | --- | --- | --- |
| **SEV-1** | Service unusable or data/security compromised | API down, database lost, tokens leaked | Immediate, all hands |
| **SEV-2** | Core feature broken for everyone | No emails sent, sign-in broken | Within 1 hour |
| **SEV-3** | Degraded or affects some users | Chat down, images missing | Within 1 day |
| **SEV-4** | Minor | A dashboard wrong | Next release |

## 3. Roles

| Role | Responsibility |
| --- | --- |
| Reporter | Detects and reports; provides details |
| Incident lead | Coordinates, decides, communicates (usually the operator) |
| Technical lead | Investigates and fixes (usually the backend owner) |
| Communicator | Updates family members |

One person may hold several roles at this scale.

## 4. Lifecycle

```
Detected → Acknowledged → Investigating → Mitigating → Resolved → Post-incident review
```

| Stage | Actions | Target time |
| --- | --- | --- |
| Detected | Alert, member report, or personal observation | — |
| Acknowledged | Someone owns it; severity assigned | 15 minutes |
| Investigating | Check health, logs, schedulers, providers | 30 minutes |
| Mitigating | Roll back, restart, restore, disable a job | 1 hour |
| Resolved | Service verified; monitoring green | — |
| Review | Timeline, root cause, actions | 3 days |

## 5. First 15 minutes

1. Confirm the symptom (`curl /health`, try a sign-in).
2. Check the last deploy — if it is recent, consider rollback first.
3. Check provider status pages (Render, Neon, Supabase, Google).
4. Check `GET /api/schedulers` for job failures.
5. Check logs for `ERROR` and `OutOfMemoryError`.
6. Assign a severity and tell the communicator.

## 6. Common mitigations (fastest first)

| Situation | Mitigation |
| --- | --- |
| Bad deploy | Roll back ([../deployment/rollback-plan.md](../deployment/rollback-plan.md)) |
| Container crashed | Restart in Render |
| Database unreachable | Wait or restore; disable schedulers |
| Email failing | Re-authorise Gmail; disable the jobs until fixed |
| AI failing | The fallback handles it; disable the agent if it worsens |
| Runaway job | Pause the trigger in Quartz (`/api/schedulers` does not pause; use Quartz tables or a restart) |
| Abuse | Tighten rate limits, block the IP at the edge |

## 7. Communication

| Audience | When | Channel |
| --- | --- | --- |
| Family members | SEV-1/SEV-2 within 30 minutes | Family chat or the announcement feature |
| Operators | Immediately | Direct message |
| Backend owner | Immediately for SEV-1/2 | Direct message |

Status message template:

> **Update <time>** — The greeting service is <status>. Impact: <what members cannot do>. We are
> <action>. Next update by <time>.

## 8. Evidence collection

1. Copy the relevant log lines (with timestamps).
2. Export the affected `email_logs` rows.
3. Note the scheduler status.
4. Record the deploy time and commit if relevant.
5. Do **not** paste tokens, OTPs or full personal data into the incident record.

## 9. Resolution and verification

| Check | Expected |
| --- | --- |
| `/health` | UP |
| Sign-in | Works |
| Scheduler | Jobs registered, next fire times sane |
| Email | A test email arrives |
| Data | Spot checks pass |
| Monitoring | No alerts for 30 minutes |

## 10. Post-incident review

Within 3 days, write:

1. **Summary** — one paragraph.
2. **Timeline** — detection, acknowledgement, mitigation, resolution with times.
3. **Impact** — who and what was affected, how many wishes missed.
4. **Root cause** — technical and process.
5. **What went well / what did not.**
6. **Actions** — each with an owner and a date, tracked in
   [../project/risk-register.md](../project/risk-register.md) or the defect log.

## 11. Specific playbooks

### Wishes not sent (SEV-2)

1. Check `GET /api/schedulers/birthdayJob` and `dailyWishJob`.
2. Check `email_logs` for the day.
3. If the AI failed, retry once the provider recovers.
4. If Gmail failed, re-authorise and trigger the job.
5. Send a manual apology/follow-up if the day is over.

### Service down after a deploy (SEV-1)

1. Roll back immediately.
2. Verify.
3. Investigate the change offline.

### Suspected credential leak (SEV-1)

1. Rotate the affected secret.
2. Restart.
3. Review access logs and `violated_users`.
4. Follow [../security/vulnerability-management.md](../security/vulnerability-management.md) §11.

### Data deleted (SEV-1)

1. Stop the application.
2. Restore from backup ([backup-and-recovery.md](backup-and-recovery.md)).
3. Verify.
4. Review who/what deleted it.

## 12. Metrics

| Metric | Target |
| --- | --- |
| Time to acknowledge | < 15 minutes |
| Time to mitigate | < 1 hour (SEV-1/2) |
| Incidents per quarter | Tracked |
| Repeat incidents | 0 for the same root cause |
