# Secure coding guidelines

Rules that match this codebase, with the pattern to follow and the anti-pattern to avoid.

## 1. Authentication and sessions

1. **Never compare passwords yourself.** Use `AuthenticationManager` / `PasswordEncoder.matches`.
2. **Never log or return a token.** Only the caller's own tokens appear in a response body.
3. **Rotate `active_session_id` on every sign-in.** Do not reuse the previous value.
4. **Check `sid` in every authenticated request path** — do not add a filter that bypasses
   `JwtAuthFilter`.
5. **Single-use tokens.** Mark `used` in the same transaction before issuing credentials.
6. **Do not weaken the resend caps.** They are the only protection against email-bombing.

## 2. Authorisation

1. **Default deny.** A new controller starts with authentication required; add `@PreAuthorize` for
   admin operations.
2. **Check ownership in the service**, not in the controller, so every caller gets the same rule.
3. **Return 404 for a hidden resource**, not 403, to avoid disclosing existence.
4. **Never trust an id from the client** for "my" resources; resolve the caller from the security
   context.
5. **Admin impersonation must stay one-way** and must reject admin targets.

## 3. Input validation

1. **Validate at the boundary.** Bean Validation on request records; no `if (x == null)` chains in
   controllers.
2. **Whitelist sort fields.** Never pass a client string into an `ORDER BY`.
3. **Use parameterised queries.** JPA derived queries and `NamedParameterJdbcTemplate` bind
   parameters; never concatenate SQL.
4. **Validate URLs** with `WishesUrls` (scheme, host, no user-info, no whitespace, length).
5. **Bound page sizes.** Reject `size` outside a sane range on aggregate endpoints.
6. **Reject unknown enum values** rather than defaulting silently where the value is security
   relevant.

## 4. Output encoding

1. **Escape every user value in email HTML** (`WishEmailExtras.escape`).
2. **Do not build HTML by concatenating unescaped input** — the note, name, link and subject all
   pass through the escape helper.
3. **Let Jackson serialise JSON**; do not hand-build JSON strings.
4. **Set the correct content type** on binary responses.

## 5. File uploads

1. **Sniff the bytes.** `MediaContentTypes.detect` decides the type.
2. **Require agreement** between the declared and detected types.
3. **Enforce per-kind size limits** before reading the whole file where possible.
4. **Generate the object key server-side**; never reuse the client file name in the path.
5. **Store only the key** in the database, not the bytes.
6. **Serve via presigned URLs or a streaming endpoint** with the stored content type.

## 6. Secrets and configuration

1. **No secret in code, tests, docs or commit messages.**
2. **Read secrets from properties bound to environment variables.**
3. **Fail fast** if a required secret is missing or too short.
4. **Never print configuration** at startup.
5. **Do not commit `.env`** — add it to `.gitignore` first.

## 7. Error handling

1. **Throw typed exceptions**; let `GlobalExceptionHandler` map them.
2. **Do not include stack traces, SQL or internal paths** in messages.
3. **Do not swallow exceptions** silently; log at `warn`/`error` with context.
4. **Do not use exceptions for control flow** in hot paths.

## 8. Logging

1. **Log identifiers, not personal content.** A user id or an email log id is enough.
2. **Never log tokens, OTPs, passwords or full email bodies.**
3. **Use parameterised logging** (`log.info("sent to {}", id)`), not string concatenation.
4. **Replace `System.out.println`** with `log`.
5. **Log security events** (lockout, impersonation, failed authorisation) at `warn`.

## 9. Concurrency and state

1. **Keep services stateless**; no mutable fields.
2. **Use `@Transactional` for multi-write operations** and keep external calls outside transactions
   where possible.
3. **Guard shared maps** (`SchedulerTrackingService`, notification tasks) with concurrency-safe
   structures.
4. **Make scheduled jobs idempotent**; use date stamps and unique constraints.

## 10. Dependencies

1. **Prefer BOM-managed versions.**
2. **No dynamic versions** (`+`, `latest.release`).
3. **Check licences and CVEs** before adding a dependency.
4. **Remove unused dependencies.**

## 11. Code review checklist

- [ ] New endpoint: authentication and authorisation explicit?
- [ ] Ownership checked in the service?
- [ ] Inputs validated, including lengths and enums?
- [ ] SQL parameterised, sort fields whitelisted?
- [ ] User text escaped in emails and any HTML?
- [ ] Uploads validated by bytes and size?
- [ ] No secret, token or personal data in logs or error messages?
- [ ] No `System.out.println`?
- [ ] Tests added for the new behaviour and its failure path?
- [ ] Documentation updated (API spec, configuration reference, business logic)?

## 12. Anti-patterns seen (and how to fix)

| Anti-pattern | Where | Fix |
| --- | --- | --- |
| Storing an attempted password | `violated_users` | Drop the column or store a hash |
| Returning `ex.getMessage()` in the 500 handler | `GlobalExceptionHandler` | Generic message plus a correlation id |
| `System.out.println` | Schedulers, controllers | `log` |
| Keyword-based email classification | `classifyEmailType` | Always pass `EmailType` |
| Public diagnostic controller | `GoogleOAuthController` | Remove or protect |
| Permit-all for a whole prefix | `/api/notifications/**` | Narrow to `/published` |
| Unauthenticated STOMP frames | `ChatSocketController` | Authenticate the handshake |
| Trusting `X-Forwarded-For` | `RateLimitFilter` | Trust only the proxy header |
| `ddl-auto=update` in production | `application-prod.properties` | Move to Flyway |
