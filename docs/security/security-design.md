# Security design

## 1. Security objectives

| Objective | How it is met |
| --- | --- |
| Only the right people sign in | BCrypt passwords, OTP/SSO, lockout, rate limiting, single-session option |
| Each user sees only their data | Ownership checks in services, circle-gated chat, admin-only endpoints |
| Wishes cannot be forged or spammed | Authenticated schedulers, rate limits on auth paths, duplicate guards |
| Uploads cannot become attacks | Byte-sniffed types, size limits, server-generated keys |
| Secrets stay secret | Environment-only configuration, no secrets in git |
| Actions are traceable | `email_logs`, `login_location_events`, `violated_users`, scheduler statistics |

## 2. Defence in depth

```
Layer 1  Transport      TLS at Render; HTTPS and WSS only
Layer 2  Edge           Rate limiting per IP+path before authentication
Layer 3  Authentication JWT signature, expiry, sid check
Layer 4  Authorisation  URL rules, @PreAuthorize, ownership checks in services
Layer 5  Validation     Bean Validation, WishesUrls, MediaValidation
Layer 6  Data           Parameterised JPA and SQL queries; escaped email HTML
Layer 7  Storage        Presigned URLs, opaque keys, bucket-scoped credentials
Layer 8  Operations     Least-privilege credentials, backups, monitoring
```

## 3. Authentication architecture

| Element | Design |
| --- | --- |
| Password storage | BCrypt, strength 10 |
| Access token | JWT HS256, 15 minutes, claims `sub`, `scope`, `sid`, optional `impersonatedBy` |
| Refresh token | JWT HS256, 7 days, stored in `refresh_tokens`, revocable |
| Session binding | `users.active_session_id` compared with the token's `sid` |
| Second factor | OTP (6 digits, 5 minutes) when `mfa_enabled` |
| Passwordless | OTP login and emailed direct-login links |
| SSO | Google ID-token verification with the client id as audience |
| Impersonation | Admin-only, non-admin target, `impersonatedBy` claim, switch-back endpoint |
| Lockout | 5 failed passwords, owner notified |
| Rate limiting | 10 requests per 60 seconds per IP+path on ten auth paths |

## 4. Authorisation architecture

| Mechanism | Scope |
| --- | --- |
| Permit-all list | Auth, docs, health, actuator, OAuth helpers, Instagram, WebSocket, notifications (see the gaps) |
| Authenticated-by-default | Everything else |
| `@PreAuthorize("hasRole('ADMIN')")` | Admin operations listed in [access-control-matrix.md](access-control-matrix.md) |
| Ownership checks | Events, emails, login locations, chat participation, circle membership |
| Non-disclosure | A hidden resource returns 404, not 403 |

## 5. Data protection

| Data | Protection |
| --- | --- |
| Passwords | BCrypt hash; never returned |
| Tokens | Never logged; refresh tokens revocable; single-use OTP/reset/direct-login tokens |
| Personal data | Returned only to the owner or an admin; the directory returns a reduced projection |
| Email bodies | Stored in `email_logs`, visible to the owner and admins; OTP/reset/circle emails are admin-only |
| Chat | Circle-gated; attachments require participation |
| Media | Private bucket; presigned URLs; opaque UUID keys |
| Backups | Provider-managed; restore access restricted |

## 6. Input validation

| Input | Validation |
| --- | --- |
| Emails | `@Email`, normalised to lower case for lookups |
| Names, titles | Length limits, required fields |
| Wishes URLs | Scheme `http`/`https`, host present, no whitespace, no user-info, ≤500 chars |
| Sort fields | Whitelisted per endpoint |
| Page and size | Validated on the sensitive dashboards; defaults elsewhere |
| Dates | `yyyy-MM-dd`, range checks, not in the future where it matters |
| Files | Byte sniffing, declared-type agreement, size per kind |
| Chat payload | At least one of text, encrypted payload or attachment |

## 7. Output encoding

| Output | Encoding |
| --- | --- |
| JSON | Jackson (escapes control characters) |
| Email HTML | `WishEmailExtras.escape` for names, notes and links; `NotificationServiceImpl.escapeHtml` |
| Error bodies | Plain text messages, no stack traces intended (the fallback can leak — see the threat model) |
| File downloads | Stored content type and original file name; `Content-Disposition: inline` |

## 8. Logging and monitoring for security

| Event | Where |
| --- | --- |
| Failed sign-in | `violated_users`, `users.failed_login_attempts`, application log |
| Lockout | Owner email + log |
| Unknown-email probing | `violated_users` + admin alert at 5 |
| Successful sign-in location | `login_location_events` |
| Sensitive email access | Enforced by role, not logged |
| Scheduler failures | `SchedulerTrackingService`, logs |
| Rate-limit trips | 429 responses (not persisted) |

## 9. Secrets and configuration

* Every secret is an environment variable; the repository has placeholders only.
* `JWT_SECRET` must be at least 32 characters.
* Storage credentials are bucket-scoped.
* The Gmail OAuth client requests only `gmail.send`.
* Rotation guidance is in [../deployment/secrets-management.md](../deployment/secrets-management.md).

## 10. Known weaknesses (summary)

| # | Weakness | Severity | Reference |
| --- | --- | --- | --- |
| 1 | Clear-text attempted passwords in `violated_users` | High | T-07 |
| 2 | Actuator endpoints public | Medium | T-08 |
| 3 | Diagnostic OAuth/Gmail endpoints public | Medium | T-06 |
| 4 | Unauthenticated STOMP messages | Medium | T-05 |
| 5 | Scheduler trigger without admin check | Medium | T-09 |
| 6 | Full profile readable by any authenticated user | Medium | T-10 |
| 7 | No refresh-token rotation | Low | T-11 |
| 8 | Trusting `X-Forwarded-For` | Low | T-12 |

Full analysis: [threat-model.md](threat-model.md).

## 11. Security requirements traceability

| Requirement | Implementation |
| --- | --- |
| SR-01 Strong password storage | BCrypt |
| SR-02 Session expiry | 15-minute access tokens |
| SR-03 Revocation | Refresh-token revocation, `sid` check |
| SR-04 Least privilege | Roles + ownership checks |
| SR-05 Input validation | Bean Validation + custom validators |
| SR-06 Upload safety | Byte sniffing + size limits |
| SR-07 Secret management | Environment variables |
| SR-08 Auditability | Email logs, login events, violation records |
| SR-09 Transport security | TLS at the platform |
| SR-10 Rate limiting | `RateLimitFilter` |
