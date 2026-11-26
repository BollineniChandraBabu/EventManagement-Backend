# Threat model

Method: enumerate the assets, the actors, and the ways the system could be abused, then record the
existing mitigation and the residual risk. Severity reflects impact × likelihood in this deployment
(one family group, small user base, internet-facing API).

## 1. Assets

| Asset | Why it matters |
| --- | --- |
| User accounts and passwords | Account takeover |
| Access and refresh tokens | Impersonation |
| Personal data (names, birthdays, relationships, pictures) | Privacy |
| Chat messages and attachments | Privacy, trust |
| Email content and OTP codes | Account takeover, privacy |
| `violated_users` records | Contain attempted passwords |
| Storage objects | Private media |
| Provider credentials | Cost and abuse |
| Availability of wishes | The product's core promise |

## 2. Actors

| Actor | Capability |
| --- | --- |
| Anonymous internet user | Can reach public endpoints |
| Registered member | Valid token, limited scope |
| Malicious member | Valid token, tries to escalate |
| Compromised admin | Full access |
| Automated scanner/bot | Probes endpoints, credentials |
| Insider with database access | Reads everything |

## 3. Threats and mitigations

### T-01 Credential stuffing / brute force

* **Vector:** repeated sign-in attempts with leaked password lists.
* **Mitigation:** BCrypt, 5-attempt lockout, rate limiting (10/60 s per IP+path), owner notification,
  `violated_users` analytics.
* **Residual:** low. Distributed attempts across many IPs are only slowed by the lockout.

### T-02 Token theft

* **Vector:** XSS in the UI, logs, shared device.
* **Mitigation:** 15-minute access tokens, revocable refresh tokens, `sid` check for single-session
  users, tokens never logged.
* **Residual:** medium — an access token is usable for up to 15 minutes; the UI stores tokens in
  browser storage (outside this repository).

### T-03 Authorisation bypass (IDOR)

* **Vector:** guessing ids of events, emails, chat messages.
* **Mitigation:** ownership checks return 404; chat participation checks; admin annotations.
* **Residual:** low–medium — `GET /api/users/{id}` is open to any authenticated user (T-10).

### T-04 Upload abuse

* **Vector:** upload a script or a huge file; exploit a mislabelled type.
* **Mitigation:** byte sniffing, declared-type agreement, per-kind size limits, server-generated
  keys, separate bucket.
* **Residual:** low — no malware scanning; a polyglot file could still pass a signature check.

### T-05 WebSocket spoofing

* **Vector:** connect to `/ws-chat` and send a frame claiming another `senderId`.
* **Mitigation:** the HTTP chat endpoint enforces the circle gate; the STOMP path persists messages
  through the same service.
* **Residual:** **medium** — the STOMP handshake is not authenticated, so identity is taken from the
  payload. Recommendation: authenticate the handshake and derive the sender from the principal.

### T-06 Public diagnostic endpoints

* **Vector:** call `/google/login`, `/oauth2callback`, `/gmail/send`, `/gmail/refresh`.
* **Mitigation:** they are diagnostic and require parameters; `/gmail/send` needs an access token.
* **Residual:** **medium** — they can be used to probe Google APIs through the service. Recommendation:
  remove after setup or protect with admin auth.

### T-07 Clear-text attempted passwords

* **Vector:** read `violated_users.password` (database access, backup leak, SQL injection).
* **Mitigation:** none in code today; the table is only written by the auth service.
* **Residual:** **high** — the column must be dropped or hashed. Immediate recommendation: restrict
  database access and exclude the table from ad-hoc exports.

### T-08 Actuator exposure

* **Vector:** read `/actuator/*` without a token.
* **Mitigation:** none today (`management.endpoints.web.exposure.include=*` and permit-all).
* **Residual:** **medium** — may reveal configuration, health details and metrics. Recommendation:
  restrict the exposure list and require admin auth.

### T-09 Scheduler manipulation

* **Vector:** any authenticated user triggers a job (`POST /api/schedulers/{job}/trigger`).
* **Mitigation:** jobs are idempotent per day for wishes; the action is auditable in logs.
* **Residual:** **medium** — a user can force a wish run (cost, duplicate risk if guards fail).
  Recommendation: add `@PreAuthorize("hasRole('ADMIN')")`.

### T-10 Profile disclosure

* **Vector:** `GET /api/users/{id}` by any authenticated member.
* **Mitigation:** the directory endpoint returns a reduced projection; this endpoint does not.
* **Residual:** **medium** — exposes role, birthday, MFA and settings. Recommendation: admin or self
  only.

### T-11 Refresh-token replay

* **Vector:** steal a refresh token and keep refreshing.
* **Mitigation:** revocation on single-session sign-in, cleanup job, 7-day expiry.
* **Residual:** low–medium — tokens are not rotated on use. Recommendation: rotate on refresh and
  detect reuse.

### T-12 Rate-limit evasion

* **Vector:** spoof `X-Forwarded-For` to get a fresh bucket.
* **Mitigation:** behind Render the header is set by the platform; the container port is not public.
* **Residual:** low if the proxy overwrites the header; higher if the container is exposed directly.

### T-13 Email abuse / spam

* **Vector:** trigger many emails (OTP requests, circle requests).
* **Mitigation:** OTP and reset resend caps, rate limiting, circle requests limited to registered
  users.
* **Residual:** low–medium — a member can send many circle requests; no per-user cap today.

### T-14 Data exfiltration through the copilot

* **Vector:** ask the admin copilot for sensitive data.
* **Mitigation:** the copilot is admin-only, read-only, with a small tool set; recipient addresses
  are withheld from the delivery-ops agent's tool output.
* **Residual:** low — an admin is already trusted with the data.

### T-15 Dependency compromise

* **Vector:** a malicious or vulnerable transitive dependency.
* **Mitigation:** BOM-pinned versions, Maven Central only, no dynamic versions.
* **Residual:** medium until scanning is automated
  ([vulnerability-management.md](vulnerability-management.md)).

### T-16 Provider outage or quota exhaustion

* **Vector:** Gemini, Gmail or Calendarific unavailable.
* **Mitigation:** model fallback, retries, graceful degradation, alerts.
* **Residual:** medium — wishes can be delayed; the email log records what happened.

### T-17 Database compromise

* **Vector:** stolen connection string or provider breach.
* **Mitigation:** TLS connections, least-privilege credentials, provider security, backups.
* **Residual:** high impact but low likelihood; the `violated_users` column makes it worse.

### T-18 Denial of service

* **Vector:** flood the API or a costly endpoint (AI generation, dashboard aggregation).
* **Mitigation:** rate limiting on auth paths only, caching, provider timeouts.
* **Residual:** medium — non-auth endpoints have no rate limit. Recommendation: add a global limit.

### T-19 Misconfiguration in deployment

* **Vector:** wrong `UI_BASE_URL` (links to an attacker site), weak `JWT_SECRET`, permissive CORS.
* **Mitigation:** documented configuration, startup validation for the JWT secret, fixed CORS list.
* **Residual:** low if the deployment checklist is followed.

### T-20 Insider access

* **Vector:** an operator reads personal data or `violated_users`.
* **Mitigation:** least-privilege provider accounts, audit of admin actions in the app (partially).
* **Residual:** medium — no formal access review process today.

## 4. Risk summary

| ID | Threat | Severity | Status |
| --- | --- | --- | --- |
| T-07 | Clear-text attempted passwords | High | Open |
| T-05 | WebSocket spoofing | Medium | Open |
| T-06 | Public diagnostic endpoints | Medium | Open |
| T-08 | Actuator exposure | Medium | Open |
| T-09 | Scheduler manipulation | Medium | Open |
| T-10 | Profile disclosure | Medium | Open |
| T-15 | Dependency compromise | Medium | Partially mitigated |
| T-18 | Denial of service | Medium | Partially mitigated |
| T-20 | Insider access | Medium | Partially mitigated |
| T-02 | Token theft | Medium | Mitigated, residual |
| T-11 | Refresh-token replay | Low–Medium | Partially mitigated |
| T-01, T-03, T-04, T-12, T-13, T-14, T-16, T-17, T-19 | Various | Low | Mitigated |

## 5. Recommended roadmap

1. Drop `violated_users.password` (or store a hash) — highest value, smallest change.
2. Authenticate the STOMP handshake.
3. Remove the OAuth/Gmail diagnostic controller.
4. Restrict actuator exposure and the permit-all list.
5. Add `@PreAuthorize("hasRole('ADMIN')")` to the scheduler controller.
6. Restrict `GET /api/users/{id}` to admin or self.
7. Add a global rate limit for non-auth endpoints.
8. Rotate refresh tokens on use.
9. Automate dependency scanning.
10. Add a per-user cap on pending circle requests.
