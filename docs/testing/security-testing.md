# Security testing

## 1. Objectives

1. Confirm every protected endpoint rejects unauthenticated and under-privileged callers.
2. Confirm ownership checks prevent cross-user access.
3. Confirm inputs cannot be abused (upload spoofing, oversize payloads, injection, link injection).
4. Confirm rate limiting and lockout work.
5. Confirm no secret or personal data leaks through responses or logs.

## 2. Test categories

| Category | Cases | Reference |
| --- | --- | --- |
| Authentication bypass | SEC-AUTH | [authentication-and-authorization.md](../backend/authentication-and-authorization.md) |
| Authorisation and roles | SEC-AUTHZ | [access-control-matrix.md](../security/access-control-matrix.md) |
| Session management | SEC-SESS | ADR 0002, 0003 |
| Input validation and injection | SEC-INPUT | [secure-coding-guidelines.md](../security/secure-coding-guidelines.md) |
| File upload | SEC-FILE | [business-logic.md](../backend/business-logic.md) §12 |
| Rate limiting and abuse | SEC-RATE | §9 of the auth document |
| Data exposure | SEC-DATA | [data-classification.md](../security/data-classification.md) |
| Configuration and infrastructure | SEC-CONF | [threat-model.md](../security/threat-model.md) |

## 3. Authentication bypass (SEC-AUTH)

| ID | Test | Expected |
| --- | --- | --- |
| SEC-AUTH-01 | Call every non-public endpoint without a token | 401 |
| SEC-AUTH-02 | Call with `Authorization: Bearer` + garbage | 401 |
| SEC-AUTH-03 | Call with a token signed by another secret | 401 |
| SEC-AUTH-04 | Call with an expired access token | 401 |
| SEC-AUTH-05 | Use a refresh token as an access token | 401 (wrong scope) |
| SEC-AUTH-06 | Use an access token as a refresh token | 400/401 |
| SEC-AUTH-07 | Tamper with the `sid` claim | 401 |
| SEC-AUTH-08 | Use an old access token after single-session sign-in | 401 `CONCURRENT_LOGIN:` |
| SEC-AUTH-09 | Replay a consumed direct-login token | 400 |
| SEC-AUTH-10 | Replay a consumed OTP | 400 |
| SEC-AUTH-11 | Use an OTP issued for another user's email | 400 |

## 4. Authorisation and roles (SEC-AUTHZ)

| ID | Test | Expected |
| --- | --- | --- |
| SEC-AUTHZ-01 | Member calls `POST /api/users` | 403 |
| SEC-AUTHZ-02 | Member calls `GET /api/users` | 403 |
| SEC-AUTHZ-03 | Member reads another user's event by id | 404 |
| SEC-AUTHZ-04 | Member lists events | Only own |
| SEC-AUTHZ-05 | Member reads another user's non-sensitive email status | 403 |
| SEC-AUTHZ-06 | Member reads an OTP email status (own or not) | 403 |
| SEC-AUTHZ-07 | Member publishes a notification | 403 |
| SEC-AUTHZ-08 | Member triggers a scheduler | **Currently allowed** — defect to confirm and fix |
| SEC-AUTHZ-09 | Member calls `/api/dashboard/violated-users` | 403 |
| SEC-AUTHZ-10 | Member changes another user's concurrent-login flag | 403 |
| SEC-AUTHZ-11 | Non-participant downloads a chat attachment | 403/404 |
| SEC-AUTHZ-12 | Non-addressee accepts a circle request | 400 |
| SEC-AUTHZ-13 | Non-participant deletes a circle | 404 |
| SEC-AUTHZ-14 | Non-sender edits a chat message | 403/400 |
| SEC-AUTHZ-15 | Member calls `/api/agent/**` | 403 |
| SEC-AUTHZ-16 | Member creates a wish image | 403 |
| SEC-AUTHZ-17 | Member writes seed data | 403 |

## 5. Session management (SEC-SESS)

| ID | Test | Expected |
| --- | --- | --- |
| SEC-SESS-01 | Two sign-ins with single-session on | Only the newest works |
| SEC-SESS-02 | Revoked refresh token | 400/401 |
| SEC-SESS-03 | Refresh token after expiry | 400 `Refresh token expired` |
| SEC-SESS-04 | Cleanup job removes expired tokens | Row deleted |
| SEC-SESS-05 | Impersonation token cannot switch back without the claim | 400 |
| SEC-SESS-06 | Impersonation of an admin | 400 |
| SEC-SESS-07 | Client-side sign-out leaves the refresh token valid | Documented limitation |

## 6. Input validation and injection (SEC-INPUT)

| ID | Test | Expected |
| --- | --- | --- |
| SEC-INPUT-01 | `searchKey` with `' OR 1=1 --` | Treated as text; no error, no data leak |
| SEC-INPUT-02 | `sortBy` with a SQL fragment | Whitelisted or rejected; no injection |
| SEC-INPUT-03 | Wishes URL `javascript:alert(1)` | 400 |
| SEC-INPUT-04 | Wishes URL with `user:pass@host` | 400 |
| SEC-INPUT-05 | Wishes URL with spaces | 400 |
| SEC-INPUT-06 | Note with `<script>alert(1)</script>` in a festival mapping | Escaped in the email |
| SEC-INPUT-07 | Name with `<img onerror=…>` | Escaped in the email |
| SEC-INPUT-08 | Over-long title (>150) in a notification | 400 |
| SEC-INPUT-09 | Over-long `searchKey` (>100) on violated users | 400 |
| SEC-INPUT-10 | Negative page or oversized page size | 400 where validated |
| SEC-INPUT-11 | JSON with an unknown field | Ignored or 400 (documented) |
| SEC-INPUT-12 | Date with the wrong format | 400 with the format message |

## 7. File upload (SEC-FILE)

| ID | Test | Expected |
| --- | --- | --- |
| SEC-FILE-01 | SVG renamed `.png` | 400 |
| SEC-FILE-02 | HTML renamed `.jpg` | 400 |
| SEC-FILE-03 | ZIP renamed `.pdf` | 400 |
| SEC-FILE-04 | 60 MB upload | 413 |
| SEC-FILE-05 | 6 MB image | 400 (image limit) |
| SEC-FILE-06 | Empty file | 400 |
| SEC-FILE-07 | File name with `../` | Ignored; the server generates the key |
| SEC-FILE-08 | Executable as a chat attachment | Accepted as `application/octet-stream`; confirm `Content-Disposition` |
| SEC-FILE-09 | Guessing an object URL | Presigned or opaque keys prevent enumeration |

## 8. Rate limiting and abuse (SEC-RATE)

| ID | Test | Expected |
| --- | --- | --- |
| SEC-RATE-01 | 11 logins in 60 s from one IP | 429 |
| SEC-RATE-02 | Different path after the limit | Unaffected |
| SEC-RATE-03 | Spoofed `X-Forwarded-For` sent directly to the container | Bypasses the limit — verify the proxy overwrites it |
| SEC-RATE-04 | 5 wrong passwords | Lockout + owner email |
| SEC-RATE-05 | 6 OTP requests | Resend cap |
| SEC-RATE-06 | 6 reset requests | Resend cap |
| SEC-RATE-07 | Repeated unknown-email attempts | `violated_users` rows; alert email at 5 |

## 9. Data exposure (SEC-DATA)

| ID | Test | Expected |
| --- | --- | --- |
| SEC-DATA-01 | Directory response fields | No role, birthday, MFA or settings |
| SEC-DATA-02 | Error bodies | No stack trace, no SQL |
| SEC-DATA-03 | Logs | No passwords or tokens |
| SEC-DATA-04 | `violated_users` contents | Contains clear-text passwords — **known defect**; restrict database access |
| SEC-DATA-05 | Email list for a member | Only own, non-sensitive |
| SEC-DATA-06 | Chat attachment of another conversation | 403/404 |
| SEC-DATA-07 | Actuator endpoints without a token | **Currently reachable** — confirm and restrict |

## 10. Configuration and infrastructure (SEC-CONF)

| ID | Test | Expected |
| --- | --- | --- |
| SEC-CONF-01 | Repository contains no secrets | No keys, passwords or tokens in git |
| SEC-CONF-02 | `.env` not committed | Absent or ignored |
| SEC-CONF-03 | CORS from an unknown origin | Rejected by the browser preflight |
| SEC-CONF-04 | CORS from the production UI origin | Allowed |
| SEC-CONF-05 | Swagger UI in production | Decide and document (currently public) |
| SEC-CONF-06 | TLS enforced | HTTP redirects or is unavailable |
| SEC-CONF-07 | Security headers | Review what Render/Spring adds; consider HSTS and `X-Content-Type-Options` |

## 11. Tooling

| Tool | Use |
| --- | --- |
| OWASP ZAP baseline scan | Automated crawl for common issues |
| `curl` scripts | Auth and role probes |
| Trivy / OWASP Dependency-Check | Dependency CVEs |
| gitleaks | Secret scanning of the repository |
| Manual review | Logic flaws scanners miss (ownership, state machines) |

## 12. Reporting

Record each finding with an ID, severity, affected endpoint, reproduction steps, evidence and a
recommendation. Track it in [defect-tracking.md](defect-tracking.md) and summarise it in
[../security/threat-model.md](../security/threat-model.md).

## 13. Findings to re-verify every release

| # | Finding | Severity |
| --- | --- | --- |
| 1 | `violated_users.password` stores clear text | High |
| 2 | Actuator endpoints reachable without authentication | Medium |
| 3 | `/gmail/**`, `/google/**`, `/oauth2callback` public | Medium |
| 4 | STOMP messages are not authenticated | Medium |
| 5 | Scheduler trigger has no admin check | Medium |
| 6 | `GET /api/users/{id}` open to every authenticated user | Medium |
| 7 | No refresh-token rotation | Low |
| 8 | `X-Forwarded-For` trusted for rate limiting | Low |
| 9 | Error fallback returns internal messages | Low |
| 10 | Swagger UI public in production | Low |
