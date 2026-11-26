# Risk register

Scoring: probability (1–5) × impact (1–5) = score. 15+ = critical, 10–14 = high, 5–9 = medium,
< 5 = low.

## 1. Technical risks

| ID | Risk | Prob | Impact | Score | Mitigation | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| R-01 | Gmail refresh token revoked; wishes stop | 3 | 5 | 15 | Detect `invalid_grant`, alert, documented re-consent runbook, SMTP fallback | Operator |
| R-02 | Gemini quota exhausted; wishes degrade | 4 | 3 | 12 | Model fallback, agent flag, non-agentic path | Backend owner |
| R-03 | `violated_users` clear-text passwords leaked | 2 | 5 | 10 | Restrict DB access now; remove the column (WBS-S1) | Backend owner |
| R-04 | Duplicate wishes after a manual trigger or second instance | 2 | 3 | 6 | Date stamps, single instance, guard checks | Backend owner |
| R-05 | A missed wish run (container down at 06:30) | 2 | 4 | 8 | Quartz misfire `FireAndProceed`, manual trigger | Operator |
| R-06 | Schema change breaks startup | 2 | 5 | 10 | Backups, review DDL, staged rollout, Flyway | Backend owner |
| R-07 | Chat database unavailable | 2 | 3 | 6 | Separate database; chat degrades only | Operator |
| R-08 | Storage quota exceeded | 2 | 3 | 6 | Gzip, monitor growth, cleanup | Operator |
| R-09 | Provider API change (Calendarific/Gemini) | 3 | 3 | 9 | Lenient parsing, model discovery, logs | Backend owner |
| R-10 | Render free tier sleeps; slow first request | 4 | 2 | 8 | Paid instance or keep-alive | Operator |

## 2. Security risks

| ID | Risk | Prob | Impact | Score | Mitigation | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| R-11 | Actuator endpoints exposed | 4 | 3 | 12 | Restrict exposure (WBS-S2) | Backend owner |
| R-12 | Unauthenticated STOMP spoofing | 2 | 3 | 6 | Authenticate the handshake (WBS-S6) | Backend owner |
| R-13 | Any member triggers schedulers | 3 | 3 | 9 | Add `@PreAuthorize` (WBS-S4) | Backend owner |
| R-14 | Profile disclosure via `GET /api/users/{id}` | 4 | 2 | 8 | Restrict to admin/self (WBS-S5) | Backend owner |
| R-15 | Credential leak in the repository | 2 | 5 | 10 | gitleaks, `.env` gitignore, review | Backend owner |
| R-16 | Vulnerable dependency exploited | 3 | 4 | 12 | Scanning, SLAs, monthly rebuild | Backend owner |
| R-17 | Brute force / credential stuffing | 3 | 3 | 9 | Lockout, rate limit, alerts | Backend owner |
| R-18 | Email abuse (OTP/circle spam) | 2 | 2 | 4 | Resend caps, rate limits | Backend owner |

## 3. Operational risks

| ID | Risk | Prob | Impact | Score | Mitigation | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| R-19 | Single developer unavailable | 3 | 3 | 9 | Documentation (this set), simple architecture | Project owner |
| R-20 | No CI; a broken change ships | 4 | 3 | 12 | Add CI (WBS-Q1) | Backend owner |
| R-21 | No metrics; failures detected late | 4 | 3 | 12 | Add metrics and alerts (WBS-R2/R3) | Backend owner |
| R-22 | Backup not restorable | 2 | 5 | 10 | Quarterly restore test | Operator |
| R-23 | Secrets not rotated | 3 | 3 | 9 | Rotation calendar | Operator |
| R-24 | Documentation drifts from code | 4 | 2 | 8 | Review per release | Backend owner |

## 4. Product risks

| ID | Risk | Prob | Impact | Score | Mitigation | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| R-25 | Generated wish is inappropriate | 2 | 4 | 8 | Validator, tone guidance, review of samples | Backend owner |
| R-26 | Members do not adopt chat | 3 | 2 | 6 | Circle invitations, unread emails | Product owner |
| R-27 | Data retention expectations unmet | 3 | 3 | 9 | Retention policy and erasure job | Product owner |
| R-28 | Scope creep (multi-family) | 2 | 4 | 8 | Documented out of scope | Project owner |

## 5. External and compliance risks

| ID | Risk | Prob | Impact | Score | Mitigation | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| R-29 | Provider terms change (AI, email) | 2 | 3 | 6 | Monitor, keep alternatives | Backend owner |
| R-30 | Privacy complaint from a member | 1 | 4 | 4 | Clear purpose, deletion on request | Product owner |
| R-31 | Region-specific data rules | 1 | 3 | 3 | Single-country deployment | Product owner |

## 6. Top five risks and actions

| Rank | Risk | Action now |
| --- | --- | --- |
| 1 | R-01 Gmail token revoked | Verify monthly; keep the runbook handy |
| 2 | R-02 Gemini quota | Check quotas; keep the agent flag ready to disable |
| 3 | R-11 Actuator exposure | Restrict this week |
| 4 | R-20 No CI | Add a test workflow |
| 5 | R-03 Clear-text passwords | Remove the column |

## 7. Review

Review this register monthly and after every incident. Update probabilities from evidence (incident
counts, alert history) rather than intuition.
