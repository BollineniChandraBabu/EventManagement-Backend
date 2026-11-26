# Authentication and authorization

## 1. Authentication mechanisms

| Mechanism | Endpoint | Credential | Result |
| --- | --- | --- | --- |
| Password | `POST /api/auth/login` | email + password (BCrypt compare) | tokens, or `MFA_OTP_REQUIRED` |
| OTP | `POST /api/auth/otp/send` then `/otp/verify` or `/otp/verify-login` | 6-digit code | tokens |
| Google SSO | `POST /api/auth/sso/google` | Google ID token | tokens |
| Direct login | `POST /api/auth/direct-login` | single-use token from an email | tokens |
| Refresh | `POST /api/auth/refresh` | refresh token | new access token |
| Impersonation | `POST /api/auth/admin/login-as-user` | admin token + target email | tokens for the target with `impersonatedBy` |

## 2. Token design

| Token | Algorithm | TTL | Claims | Storage |
| --- | --- | --- | --- | --- |
| Access | HS256 (`app.jwt.secret`) | 900 s | `sub` (email), `scope` (role name), `sid`, `impersonatedBy?`, `iat`, `exp` | Client only |
| Refresh | HS256 | 604 800 s | `sub`, `scope=REFRESH`, `impersonatedBy?`, `iat`, `exp` | `refresh_tokens` |

* `JwtService.generateAccessToken` reads `active_session_id` from the entity, so every sign-in path
  stamps the right `sid` without callers passing it.
* `isValid` parses and verifies the signature and expiry; an invalid token simply leaves the request
  unauthenticated (the URL rules then produce 401/403).
* A token with no `sid` (issued before the field existed) is accepted; this keeps old tokens valid.

## 3. Session lifecycle

```
sign-in ──► rotate users.active_session_id (UUID)
        ──► if concurrent_login_allowed = false: revoke all refresh tokens
        ──► access token carries the new sid
request ──► JwtAuthFilter: token.sid == users.active_session_id ? continue : 401 CONCURRENT_LOGIN
refresh ──► token must exist, be unrevoked and unexpired; revoked-by-newer-session ⇒ 401 CONCURRENT_LOGIN
```

## 4. Roles

| Role | Granted | Typical use |
| --- | --- | --- |
| `ROLE_ADMIN` | All endpoints, all data, scheduler control, impersonation | Family organiser |
| `ROLE_USER` | Own profile and settings, own events (read), calendar, circles, chat, notifications, themes | Family member |

Roles are stored as an ordinal in `users.role` and mapped to Spring authorities by
`CustomUserDetailsService` (`ROLE_ADMIN` / `ROLE_USER`).

## 5. URL-level rules (`SecurityConfig`)

Permit-all:

```
/api/auth/**        /v3/api-docs/**   /swagger-ui/**   /swagger-ui.html
/health             /actuator/**      /google/**       /oauth2callback
/gmail/**           /instagram/**     /ws-chat/**      /api/notifications/**
```

Everything else requires authentication. CSRF is disabled and the session policy is `STATELESS`
because there is no cookie session.

## 6. Method-level rules

| Annotation | Applied to |
| --- | --- |
| `@PreAuthorize("hasRole('ADMIN')")` | `AgentController` (class), `NotificationController` (class), `RelationshipSeedController` (class), `SpecialEventSeedController` (class), `TemplateTypeSeedController` (class), `WishImageAdminController` (class); user create/update/list/filter/deactivate/status; event create/update/delete; festival mapping writes/reads; email send-now/delete/OTP/reset status; dashboard OTP/reset/violated-users; admin login-as-user; concurrent-login for another user |
| `@PreAuthorize("isAuthenticated()")` | `GET /api/auth/admin/switch-back`, `GET /api/festival-wish-mappings/me` |
| none | Every other endpoint: any authenticated user (ownership is enforced in services) |

## 7. Ownership and scoping rules

| Resource | Rule |
| --- | --- |
| Event read | Admin or the event owner; otherwise `404` |
| Event list | Admin sees all; others see their own |
| Email status | Admin sees all; others see their own non-sensitive emails |
| Email image | Same as status; sensitive types admin-only |
| Calendar | Admin sees all; others see own plus accepted circle members |
| Login locations | Own, or any user's when admin |
| Chat | Participants only; messaging requires an accepted circle when circles-only is on |
| Chat attachment | Participant of the message |
| Circle response | Addressee only |
| Circle removal | Either participant |
| Theme preference | Own only |
| Wish settings / MFA / concurrent login | Own (or admin for concurrent login) |
| Directory | Any authenticated user, reduced projection |
| Notification publish | Admin |
| Scheduler trigger | Authenticated (no admin annotation — see known issues) |

## 8. Failed sign-in handling

| Control | Value |
| --- | --- |
| Attempt counter | `users.failed_login_attempts` |
| Threshold | `app.login.failed-attempt-threshold` (5) |
| Effect at threshold | Password sign-in refused with a contact-administrator message; owner emailed |
| Reset | Successful password sign-in sets it to 0 |
| Unknown-email tracking | `violated_users` row per attempt |
| Unknown-email alert | At ≥5 rows for the same email, an admin email is sent |

## 9. Rate limiting

| Item | Value |
| --- | --- |
| Scope | Ten auth paths (`login`, `refresh`, `otp/send`, `otp/verify`, `otp/verify-login`, `forgot-password`, `password-reset/confirm`, `change-password`, `sso/google`, `direct-login`) |
| Key | `clientIp + "|" + path` |
| Window | `app.rate-limit.window-seconds` (60) |
| Limit | `app.rate-limit.max-requests` (10) |
| Client IP | First `X-Forwarded-For` entry, else `X-Real-IP`, else remote address |
| Response | 429 `{timestamp, status, message}` |
| Storage | In-memory map, pruned above 100 000 keys |
| Disable | `app.rate-limit.enabled=false` |

> `X-Forwarded-For` is trusted, so a client that can set it directly (without a proxy) can evade the
> limit. Behind Render the header is set by the platform.

## 10. Secrets and passwords

* Passwords are hashed with BCrypt (strength default 10).
* `JWT_SECRET` must be at least 32 bytes for HS256; a short secret throws at startup.
* No secret is in the repository; every one is an environment variable.
* `violated_users.password` stores a clear-text attempted password — a known defect (see
  [../security/threat-model.md](../security/threat-model.md) T-07).

## 11. Known gaps

| Gap | Impact | Recommendation |
| --- | --- | --- |
| `/actuator/**` permit-all with `exposure.include=*` | Actuator data exposed | Restrict or disable |
| `/gmail/**`, `/google/**` public | Diagnostic endpoints callable by anyone | Remove or protect |
| `/api/notifications/**` permit-all | Depends on method annotations | Narrow the pattern |
| STOMP endpoint unauthenticated | Sender identity comes from the payload | Authenticate the handshake |
| `GET /api/users/{id}` open to all authenticated users | Profile disclosure | Restrict to admin/self |
| No refresh-token rotation on use | A stolen refresh token stays valid until expiry | Rotate on refresh |
| Access tokens cannot be revoked before expiry | Up to 15 minutes of residual access | Shorten TTL or add a denylist |
