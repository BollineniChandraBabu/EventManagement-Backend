# Defect tracking

## 1. Severity

| Severity | Definition | Examples |
| --- | --- | --- |
| **Critical** | Data loss, security breach, or the product cannot be used | Tokens forgeable; all wishes fail; production down |
| **High** | A core feature is broken with no workaround | Sign-in broken; emails not sent; calendar empty |
| **Medium** | A feature is impaired but a workaround exists | Wrong image chosen; a filter returns too much |
| **Low** | Cosmetic or minor inconvenience | Sorting oddity; inconsistent message text |

## 2. Priority

| Priority | Meaning | Response |
| --- | --- | --- |
| P1 | Fix immediately in a hotfix | Same day |
| P2 | Fix in the current release | Within the sprint |
| P3 | Fix in a later release | Backlog |
| P4 | Nice to have | Backlog, may never be done |

Severity and priority are independent: a critical severity in an unused feature may be P2.

## 3. Lifecycle

```
New → Triaged → In Progress → Fixed → Verified → Closed
                        ↘ Reopened ↗
                        ↘ Deferred (with reason)
```

| State | Entry condition | Exit condition |
| --- | --- | --- |
| New | Reported with reproduction steps | Triaged with severity and priority |
| Triaged | Severity and priority agreed | Assigned to a developer |
| In Progress | Work started | A fix is pushed |
| Fixed | Fix merged and deployed to QA | Verified by the tester |
| Verified | Reproduction now passes; regression test added | Closed |
| Closed | — | — |
| Reopened | The issue reproduces again | Back in progress |
| Deferred | Accepted risk or blocked | Re-triaged later |

## 4. Report template

```markdown
**Title:** Short, specific summary
**ID:** DEF-###
**Severity:** Critical | High | Medium | Low
**Priority:** P1 | P2 | P3 | P4
**Environment:** local | QA | production (commit / date)
**Endpoint / screen:**
**Preconditions:**
**Steps to reproduce:**
1. …
2. …
**Expected:**
**Actual:**
**Evidence:** request/response, log lines, screenshot, email_logs id
**Workaround:**
**Related:** requirement, test case id
```

## 5. Known defects (open)

| ID | Title | Severity | Priority | Notes |
| --- | --- | --- | --- | --- |
| DEF-001 | `violated_users` stores the attempted password in clear text | High | P1 | Remove the column or hash it; restrict database access meanwhile |
| DEF-002 | Actuator endpoints reachable without authentication | Medium | P2 | Restrict exposure and the permit-all rule |
| DEF-003 | `/gmail/**`, `/google/**`, `/oauth2callback` are public | Medium | P2 | Remove the diagnostic controller after OAuth setup |
| DEF-004 | STOMP chat frames are not authenticated | Medium | P2 | Authenticate the handshake; derive the sender from the principal |
| DEF-005 | `POST /api/schedulers/{job}/trigger` has no admin check | Medium | P2 | Add `@PreAuthorize("hasRole('ADMIN')")` |
| DEF-006 | `GET /api/users/{id}` exposes full profiles to any authenticated user | Medium | P2 | Restrict to admin or self |
| DEF-007 | Duplicate email on user creation returns 500 | Medium | P2 | Pre-check or map the constraint violation to 409 |
| DEF-008 | Unknown dashboard `mailTab` silently falls back | Low | P3 | Reject unknown values |
| DEF-009 | Chat attachment upload failure stores a message without the file | Low | P3 | Fail the request instead |
| DEF-010 | `System.out.println` in schedulers and controllers | Low | P4 | Replace with `log` |
| DEF-011 | Scheduler statistics reset on restart | Low | P4 | Persist or export them |
| DEF-012 | Non-recurring events still appear in month queries | Low | P4 | Filter by year and date in the calendar |
| DEF-013 | Email type inferred by keywords can misclassify | Low | P3 | Always pass `EmailType` |
| DEF-014 | Notification publish schedule is lost on restart | Low | P3 | Persist scheduled publishes |
| DEF-015 | Swagger UI public in production | Low | P4 | Disable or protect |

## 6. Metrics

| Metric | Definition | Target |
| --- | --- | --- |
| Defect density | Defects per 1 000 lines changed | < 5 |
| Escape rate | Defects found after release ÷ total | < 10% |
| Mean time to fix (P1) | Report → verified | < 24 h |
| Mean time to fix (P2) | Report → verified | < 10 days |
| Reopen rate | Reopened ÷ closed | < 5% |
| Regression share | Defects caused by a recent change | Tracked; aim to fall |

## 7. Triage meeting

Weekly, 15 minutes: review new defects, confirm severity and priority, assign an owner, close
verified ones. A P1 defect triggers an immediate ad-hoc triage.

## 8. Root-cause categories

| Category | Meaning |
| --- | --- |
| Requirement | The behaviour was never specified |
| Design | The approach was wrong |
| Code | Implementation error |
| Configuration | Wrong property or environment |
| Data | Bad or unexpected data |
| Integration | Provider contract or availability |
| Environment | Infrastructure or deployment |
| Test gap | A test should have caught it |

Record the category on every closed defect so trends are visible.

## 9. Escalation

| Situation | Action |
| --- | --- |
| P1 confirmed in production | Notify the operator and start the incident process ([../operations/incident-management.md](../operations/incident-management.md)) |
| Security defect | Treat as P1 if exploitable; follow [../security/vulnerability-management.md](../security/vulnerability-management.md) |
| Data loss | Stop the affected job and restore from backup if needed ([../operations/backup-and-recovery.md](../operations/backup-and-recovery.md)) |
