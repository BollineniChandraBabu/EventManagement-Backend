# 0002 — Stateless JWT access tokens with stored refresh tokens

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../backend/authentication-and-authorization.md](../../backend/authentication-and-authorization.md)

## Context

The Angular UI calls the API from a different origin (Firebase Hosting) and needs to stay signed in
across page reloads without server-side sessions. Sign-out and single-session control still need to
take effect quickly.

## Decision

* Issue an HS256 access token (`sub`, `scope`, `sid`, optional `impersonatedBy`) valid for 15 minutes,
  verified on every request without a database lookup beyond loading the user.
* Issue a refresh token valid for 7 days and persist it in `refresh_tokens` so it can be revoked.
* Keep CSRF disabled and sessions stateless, because no cookie-based session exists.

## Consequences

**Positive**

* Horizontal scaling needs no sticky sessions or shared session store.
* Refresh tokens can be revoked; access tokens are short enough that revocation delay is bounded.
* `sid` gives a cheap way to reject superseded sessions (see ADR 0003).

**Negative**

* An access token cannot be revoked before it expires; a stolen token is usable for up to 15 minutes.
* Refresh tokens are bearer secrets in the database and must be protected.
* `JwtAuthFilter` still loads the user per request to read the session state, so the "stateless" win
  is partial.

**Follow-up**

* Consider a token denylist if a shorter revocation window is required.
* Rotate `JWT_SECRET` only with a plan, since rotation invalidates all sessions.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Server-side sessions (Spring Session) | Requires a shared store and cookie/CORS handling across origins |
| Opaque tokens with introspection | A database round trip per request with no clear benefit here |
| Long-lived access tokens only | No revocation path at all |
