# Exception handling

## 1. Strategy

* **Throw, do not catch** in controllers. Services throw typed runtime exceptions; one advice maps
  them to HTTP responses.
* **Two places** write error bodies because they run before the dispatcher:
  `RateLimitFilter` (429) and `JwtAuthFilter` (401 for a stale session).
* **Fail soft** where the user cannot act: image generation, geolocation, circle notification mail,
  realtime publishing and Instagram webhooks degrade instead of throwing.
* **Never leak internals** in messages; use short, user-safe text. The fallback handler returns
  `ex.getMessage()`, which is a known risk (see §6).

## 2. Error payload

```json
{
  "timestamp": "2026-11-26T09:15:42.123456",
  "status": 400,
  "message": "Invalid event type"
}
```

`timestamp` is `LocalDateTime` in `Asia/Kolkata`.

## 3. Exception types

| Exception | HTTP | When thrown |
| --- | --- | --- |
| `NotFoundException` | 404 | Missing entity; also used to hide another user's resource |
| `BadRequestException` | 400 | Invalid input, business rule violation, invalid/expired token, lockout, MFA required |
| `RateLimitExceededException` | 429 | Sliding-window limit exceeded (thrown by `RateLimiter`, handled by the filter and the advice) |
| `ConcurrentLoginException` | 401 | Refresh token revoked by a newer single-session sign-in |
| `AiUnavailableException` | 503 | No Gemini model answered |
| `MethodArgumentNotValidException` | 400 | Bean Validation failure (first field message) |
| `Exception` (fallback) | 500 | Anything unhandled |

## 4. Mapping table (`GlobalExceptionHandler`)

| Handler | Response |
| --- | --- |
| `handleNotFound` | 404, `ex.getMessage()` |
| `handleBad` | 400, `ex.getMessage()` |
| `handleRateLimit` | 429, `ex.getMessage()` |
| `handleConcurrentLogin` | 401, `"CONCURRENT_LOGIN: " + ex.getMessage()` |
| `validation` | 400, first binding error message |
| `handleAiUnavailable` | 503, `"The AI service is busy right now. Please try again in a minute."` |
| `fallback` | 500, `ex.getMessage()` |

## 5. Filter-written responses

| Filter | Status | Body |
| --- | --- | --- |
| `RateLimitFilter` | 429 | `{"timestamp":"…","status":429,"message":"Too many attempts. Please try again in a minute."}` |
| `JwtAuthFilter` (stale `sid`) | 401 | `{"timestamp":"…","status":401,"message":"CONCURRENT_LOGIN: Your account was signed in on another device, so this session ended. Please sign in again."}` |

Both are written manually because the advice is not in the chain yet. Quotes in the message are
replaced with `'` in the rate-limit body to keep the JSON valid.

## 6. Message catalogue

| Context | Message |
| --- | --- |
| Sign-in, unknown email | `User not found` |
| Sign-in, wrong password | `Invalid email or password` |
| Sign-in, locked | `Too many failed login attempts. Please contact the Administrator.` |
| Sign-in, second factor | `MFA_OTP_REQUIRED` |
| Refresh, invalid | `Invalid refresh token` |
| Refresh, expired | `Refresh token expired` |
| OTP, resend cap | `OTP resend limit reached. Please try again after the current code expires.` |
| OTP, invalid | `Invalid or expired OTP` / `OTP not found` |
| Reset, resend cap | `Reset link request limit reached. Please try again after the current link expires.` |
| Reset, bad token | `Invalid token` / `Expired token` |
| Password change | `Current password is incorrect` / `New password must be different from current password` |
| Direct login | `Invalid or expired login link` / `User account is inactive` |
| SSO | `Google SSO is not configured` / `Invalid Google ID token` / `Google email is not verified` |
| Impersonation | `Only admin can login as user` / `Admin cannot login as another admin` / `User account is inactive` |
| Events | `Event not found` / `Invalid event type` / `Unable to upload image to storage` |
| Circles | `You cannot send a circle request to yourself` / `This user is already in your circle` / `A circle request is already pending` / `Only the addressee can respond to this circle request` / `This circle request has already been handled` |
| Chat | `You can only message users in your circle` / `Message text, encrypted payload or attachment is required` / `Receiver not found` |
| Media | `Only PNG, JPEG, GIF, WebP or BMP images are allowed` / the long supported-media message / `The file is too large; the limit …` / `A non-empty file is required` |
| Wishes URL | `The wishes link must be at most 500 characters` / `must not contain spaces` / `must start with http:// or https://` / `must contain a website address` / `must not contain a username or password` / `is not a valid web address` |
| Notifications | `At least one notification id is required` plus schedule-window messages |
| Dashboards | `Invalid date format. Use yyyy-MM-dd for startDate and endDate` / `startDate must be before endDate` / `startDate and endDate must not be the same` / `endDate must not be greater than today` / `searchKey must be at most 100 characters` / `page must not be negative` / `size must be between 1 and 500` |
| Rate limit | `Too many attempts. Please try again in a minute.` |
| AI | `The AI service is busy right now. Please try again in a minute.` |
| Auth context | `Authenticated user not found` |

## 7. Logging of failures

| Failure | Level | What is logged |
| --- | --- | --- |
| Email send | `error` | `mail send failed` with the exception; the log row keeps the message |
| AI model failure | `warn` | Model name and HTTP status |
| All models failed | `error` via `AiUnavailableException` | The list of tried models |
| Pollinations failure | `warn` | URL and status, or the network message |
| Calendarific failure | `error` | `Calendarific sync failed` |
| ip-api failure | `warn` | IP and message |
| Circle mail failure | `error` | Circle id and recipient |
| Scheduler failure | `error` | Exception, recorded in the tracking statistics |
| Gmail token revoked | `error` | Instruction to regenerate the refresh token |

## 8. Risks and improvements

1. **Internal messages leak.** The fallback handler returns `ex.getMessage()`, which can expose SQL or
   library text. Prefer a generic message plus a correlation id.
2. **No error codes.** Clients match on message text (for example `MFA_OTP_REQUIRED` and the
   `CONCURRENT_LOGIN:` prefix). Add a `code` field for stability.
3. **Constraint violations become 500.** A duplicate email or token surfaces as a
   `DataIntegrityViolationException`; map it to 409/400.
4. **Filter bodies are hand-built.** Use Jackson to serialise them.
5. **`NotFoundException` used for authorisation.** Intentional non-disclosure, but it hides real 403s
   from logs; log the denial at debug level.
