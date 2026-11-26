# Known issues

Current defects and limitations an operator or developer should know about. Severity follows
[../testing/defect-tracking.md](../testing/defect-tracking.md).

## 1. Security

| ID | Issue | Impact | Workaround | Status |
| --- | --- | --- | --- | --- |
| KI-01 | `violated_users.password` stores the attempted password in clear text | Reading the table exposes real passwords | Restrict database access; exclude from exports | Open (DEF-001) |
| KI-02 | `/actuator/**` is permitted and all endpoints are exposed | Configuration and metrics readable without a token | Restrict the exposure list; block at the edge | Open (DEF-002) |
| KI-03 | `/google/**`, `/oauth2callback`, `/gmail/**` are public | Diagnostic endpoints can be called by anyone | Remove after OAuth setup | Open (DEF-003) |
| KI-04 | The STOMP endpoint does not authenticate the sender | A crafted frame can claim another user | Restrict network access to `/ws-chat` | Open (DEF-004) |
| KI-05 | `POST /api/schedulers/{job}/trigger` has no admin check | Any member can force a job | Monitor scheduler runs | Open (DEF-005) |
| KI-06 | `GET /api/users/{id}` returns the full profile to any authenticated user | Role, birthday and settings disclosed | Use the directory endpoint in the UI | Open (DEF-006) |
| KI-07 | Refresh tokens are not rotated on use | A stolen token stays valid until expiry | Shorten the refresh TTL if needed | Open |
| KI-08 | The error fallback returns `ex.getMessage()` | Internal messages can leak | Watch for 500s in logs | Open |
| KI-09 | Swagger UI is public | API surface disclosed | Acceptable for now | Open |

## 2. Functional

| ID | Issue | Impact | Workaround | Status |
| --- | --- | --- | --- | --- |
| KI-10 | Creating a user with a duplicate email returns 500 | Poor UX | Check the email first | Open (DEF-007) |
| KI-11 | Non-recurring events still appear in month queries | A past one-off may show in that month | The UI filters by year/date | Open (DEF-012) |
| KI-12 | Email type is inferred from keywords when not passed | Misclassification possible | Callers pass `EmailType` where it matters | Open (DEF-013) |
| KI-13 | Unknown dashboard `mailTab` silently falls back | Unexpected results | Use documented values | Open (DEF-008) |
| KI-14 | A chat message is stored even if the attachment upload fails | A message without its file | Re-send with the attachment | Open (DEF-009) |
| KI-15 | Notification publish schedules are in memory | A restart loses a pending publish | Re-publish after a restart | Open (DEF-014) |
| KI-16 | The calendar requires `month`; omitting it returns no events | Empty response | Always send `month` | Open |
| KI-17 | Festival wishes for a year not synced need an on-demand fetch | The first request is slow | Request the year once | By design |
| KI-18 | Wish images match on a normalised event-type string | A typo means the image is not found | Use the exact type name | By design |

## 3. Operational

| ID | Issue | Impact | Workaround | Status |
| --- | --- | --- | --- | --- |
| KI-19 | Scheduler statistics are in memory | Reset on restart; per instance | Use logs for history | Open (DEF-011) |
| KI-20 | Job times are fixed in code (except two crons) | A change needs a redeploy | Trigger manually | Open |
| KI-21 | Quartz tables are re-initialised on every start | Slightly slower start | Set `initialize-schema=never` later | Open |
| KI-22 | `ddl-auto=update` cannot drop or narrow columns | Schema drift | Manual SQL | Open (ADR 0011) |
| KI-23 | Rate limiting and caches are per instance | Multiple instances each have their own budget | Keep one instance | Open |
| KI-24 | `System.out.println` in a few classes | Inconsistent logs | Ignore | Open (DEF-010) |
| KI-25 | Render free tier sleeps when idle | Slow first request | Paid instance or keep-alive | Open |
| KI-26 | No metrics endpoint or log aggregation | Limited observability | Use logs and dashboards | Open |
| KI-27 | `/health` does not check dependencies | A database outage still reports UP | Use the deeper checks | Open |

## 4. Data and retention

| ID | Issue | Impact | Workaround | Status |
| --- | --- | --- | --- | --- |
| KI-28 | `login_location_events` and `violated_users` grow without pruning | Unbounded growth | Manual cleanup | Open |
| KI-29 | Expired OTP/reset/login tokens are not pruned | Minor table growth | Manual cleanup | Open |
| KI-30 | Users are soft-deleted only | Personal data remains | Manual hard delete | Open |
| KI-31 | Deleting an email row may leave the storage object | Orphaned objects | Manual cleanup | Open |
| KI-32 | Chat attachments are not virus-scanned | Malware could be shared within a circle | Trust-based model | Accepted |

## 5. Integrations

| ID | Issue | Impact | Workaround | Status |
| --- | --- | --- | --- | --- |
| KI-33 | Gmail refresh tokens can be revoked | Email sending stops | Re-authorise | Open |
| KI-34 | Pollinations images are best-effort | Some emails lack an image | Upload wish images | By design |
| KI-35 | Gemini free-tier quotas are low | Wishes may fall back or fail | Paid key or agent off | Open |
| KI-36 | ip-api.com is HTTP and rate limited | Geolocation gaps | 24-hour cache | By design |
| KI-37 | The Instagram feature ships with a placeholder token | The feature is dormant | Configure it or ignore | Open |
| KI-38 | Calendarific free plan limits | Fewer festivals | Paid plan | Open |

## 6. Documentation and process

| ID | Issue | Impact | Status |
| --- | --- | --- | --- |
| KI-39 | No CI pipeline | Tests are not enforced | Open |
| KI-40 | No coverage measurement | Trends unknown | Open |
| KI-41 | No integration or scheduler tests | Regressions can slip through | Open |
| KI-42 | `.env` is not in `.gitignore` | Risk of committing secrets | Open |

## 7. Prioritised fix list

| Priority | Items |
| --- | --- |
| P1 | KI-01 (clear-text passwords) |
| P2 | KI-02, KI-03, KI-04, KI-05, KI-06, KI-10 |
| P3 | KI-07, KI-14, KI-15, KI-22, KI-28…KI-31, KI-39…KI-42 |
| P4 | KI-09, KI-11, KI-12, KI-13, KI-20, KI-21, KI-24, KI-27, KI-33, KI-37 |

## 8. Reporting a new issue

Use the template in [../testing/defect-tracking.md](../testing/defect-tracking.md) §4, then add it
here if it affects operations. Keep this list short: remove entries when fixed.
