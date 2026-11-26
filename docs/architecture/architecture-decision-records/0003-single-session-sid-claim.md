# 0003 — Enforce single-session sign-in with a `sid` claim

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../backend/authentication-and-authorization.md](../../backend/authentication-and-authorization.md)

## Context

Some users want only one active device. Access tokens are stateless and cannot be revoked
individually, so "sign out the other device" needs a mechanism that works with short-lived tokens.

## Decision

* Give every user an `active_session_id` (UUID), rotated on every successful sign-in.
* Put that value in the access token as the `sid` claim.
* When `concurrent_login_allowed=false`, revoke all earlier refresh tokens at sign-in.
* In `JwtAuthFilter`, reject a request whose `sid` differs from the user's current
  `active_session_id` with `401` and a body starting `CONCURRENT_LOGIN:`.
* In `refresh()`, surface the same signal when the presented refresh token was revoked by a newer
  single-session sign-in.

## Consequences

**Positive**

* Old devices lose access immediately, not after 15 minutes.
* The UI can show a specific message and offer a fresh sign-in.
* No extra table or cache: the check reuses the user row that is already loaded.

**Negative**

* Every request loads the user, so the check is not free.
* `sid` is null for tokens issued before the field existed; those tokens remain valid by design.
* A user who disallows concurrent logins and signs in twice on the same device still invalidates the
  first session.

**Follow-up**

* Consider a 401 code header instead of a message prefix.
* Document the behaviour for support (see [../../operations/troubleshooting-guide.md](../../operations/troubleshooting-guide.md)).

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Token denylist in a shared cache | Extra infrastructure; no cache server in the deployment |
| Shorter access tokens only | Does not give immediate sign-out |
| One refresh token per user (no access-token check) | The old access token would still work for its remaining lifetime |
