# Integration design

Every external system the backend talks to, with the exact endpoint, authentication, payload shape,
failure handling and the properties that control it. Operational detail (quotas, troubleshooting)
is in [../backend/external-api-integrations.md](../backend/external-api-integrations.md).

## 1. Integration summary

| Integration | Direction | Protocol | Auth | Failure handling |
| --- | --- | --- | --- | --- |
| Gmail API | Outbound | HTTPS JSON + base64url MIME | OAuth 2.0 refresh token | Log `FAILED`, retry up to 3, alert |
| SMTP (`JavaMailSender`) | Outbound | SMTP 587 STARTTLS | Username/password | Log `FAILED`, retry |
| Brevo | Outbound | HTTPS JSON | `api-key` header | Logged by caller |
| Gemini `generateContent` | Outbound | HTTPS JSON | `x-goog-api-key` | Model fallback, then `503` |
| Gemini `models` list | Outbound | HTTPS JSON | `x-goog-api-key` | Keep previous list for 1 min |
| Pollinations image | Outbound | HTTPS GET (binary) | Optional bearer | Return `null`, send email without image |
| Pollinations balance | Outbound | HTTPS GET | Optional bearer | `400` "Unable to fetch pollinations balance" |
| Calendarific | Outbound | HTTPS JSON | `api_key` query | Log, keep existing rows |
| ip-api.com | Outbound | HTTP JSON | none | `status=fail`, not cached |
| Google Identity | Outbound | HTTPS (library) | client id audience | `400` "Unable to verify Google ID token" |
| Supabase Storage | Outbound | S3 API | Access key / secret | Return `null`/`400`, delete is best effort |
| Instagram Graph | Outbound | HTTPS JSON | Page access token | Message logged `FAILED`, retried |
| Instagram webhook | Inbound | HTTPS GET/POST | Verify token (GET only) | Always `200 EVENT_RECEIVED` on POST |
| Google OAuth helper | Inbound | HTTPS redirect/callback | none (public) | Returns provider response |

## 2. Gmail API

| Item | Value |
| --- | --- |
| Endpoint | `https://gmail.googleapis.com/gmail/v1/users/me/messages/send` (via the Google client library) |
| Auth | `UserCredentials` from `gmail.client-id`, `gmail.client-secret`, `gmail.refresh-token`; access token refreshed on demand |
| Request | MIME message (`To`, `Subject`, HTML body, inline image `cid:birthdayImage`, inline signature `cid:emailSignature`, attachments) base64url-encoded into `raw` |
| Scope | `https://www.googleapis.com/auth/gmail.send` |
| Success | `email_logs.status = SENT`, `sent_at` set |
| Failure | `status = FAILED`, `retry_count + 1`, `error_message` set |
| Revoked token | `GmailConfig.isInvalidGrant` logs a clear instruction to re-authorise and update `GMAIL_REFRESH_TOKEN` |
| Retry | `retryFailed()` re-sends `FAILED` rows with `retryCount < 3` |
| Config | `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN`, `GMAIL_FROM_EMAIL`, `gmail.redirect-uri` |

## 3. SMTP and Brevo

| Item | SMTP | Brevo |
| --- | --- | --- |
| Class | `EmailServiceImpl` | `BrevoEmailServiceImpl` |
| Config | `spring.mail.*` (`MAIL_USERNAME`, `MAIL_PASSWORD`, host `smtp.gmail.com`, port 587, STARTTLS) | `brevo.api.key`, `brevo.api.url`, `brevo.sender.email`, `brevo.sender.name` |
| Use | Alternative transport and failure alerts (`sendFailureAlert`) | Alternative transactional transport |
| Logging | `email_logs` | Caller-managed |

## 4. Gemini (text)

| Item | Value |
| --- | --- |
| Base URL + primary model | Parsed from `GEMINI_API_URL` by `GeminiEndpoint.parse` |
| Call | `POST {baseUrl}/models/{model}:generateContent` with `contents[0].parts[0].text` |
| Auth | Header `x-goog-api-key: ${GEMINI_API_KEY}` |
| Model discovery | `GET {baseUrl}/models?pageSize=200`, filtered to models supporting `generateContent` and not matching the exclusion regex |
| Fallback | Primary model first, then discovered models not yet tried and not in cooldown; at most `GEMINI_FALLBACK_MAX_RETRIES` extra attempts |
| Retryable | 429, 404, 5xx, network errors |
| Not retryable | 400, 401, 403 (bad key or bad request) → rethrown |
| Cooldown | `GEMINI_FALLBACK_COOLDOWN_SECONDS` (60) per failed model |
| Catalogue cache | `GEMINI_FALLBACK_MODEL_CACHE_MINUTES` (60) |
| Exhaustion | `AiUnavailableException` → HTTP 503 |
| Agents | `GeminiAgentClient` sends `systemInstruction`, `contents`, `tools.functionDeclarations`, `toolConfig` |

## 5. Pollinations (images)

| Item | Value |
| --- | --- |
| Image endpoint | `GET {POLLINATIONS_IMAGE_URL}{url-encoded prompt}?model={POLLINATIONS_IMAGE_MODEL}` |
| Auth | `Authorization: Bearer ${POLLINATIONS_IMAGE_KEY}` when the key is set |
| Accept | `image/png` |
| Success | Image bytes used as the email's inline image |
| Failure | Warning logged, `null` returned, email sent without an image |
| Balance | `GET ${POLLINATIONS_BALANCE_URL}` → JSON map returned to admins |
| Prompt | Occasion, recipient name, style requirements (photorealistic, readable typography, no watermark) |

## 6. Calendarific (festivals)

| Item | Value |
| --- | --- |
| Endpoint | `GET {CALENDARIFIC_API_URL}?api_key=…&country=IN&year=YYYY` |
| Auth | Query parameter `api_key` |
| Response | `response.holidays[]` with `name` and `date.iso` (or `date.datetime.{year,month,day}`) |
| Upsert key | Festival name (case-insensitive) within the same year |
| Defaults | Message `Happy {name}! Wishing you joy and prosperity.` when empty; `active=true` |
| Deactivation | Current-year rows not returned by the sync are marked inactive |
| Scheduling | Monthly (`0 0 2 1 * ?`) and on demand for other years |
| Failure | Logged; existing data untouched; a failed on-demand fetch leaves the year empty |

## 7. ip-api.com (geolocation)

| Item | Value |
| --- | --- |
| Endpoint | `GET http://ip-api.com/json/{ip}?fields=status,message,country,countryCode,region,regionName,city,zip,lat,lon,timezone,isp,org,as,mobile,proxy,hosting,query` |
| Auth | None |
| Rate limit | 45 requests/minute (free tier) — mitigated by a 24-hour cache |
| Caching | `@Cacheable(ipInfo)` unless the result status is not `success` |
| Failure | `status=fail` with the message; dashboard still renders |
| Usage | Failed-login map and per-row enrichment (only for the newest 300 points) |

## 8. Google Identity (SSO)

| Item | Value |
| --- | --- |
| Mechanism | `GoogleIdTokenVerifier` with the configured client id as audience |
| Checks | Signature, audience, verified email |
| Config | `app.google.sso.client-id` (defaults to `GMAIL_CLIENT_ID`) |
| Failure | `400` "Invalid Google ID token" / "Google email is not verified" / "Unable to verify Google ID token" |

## 9. Supabase Storage (S3 API)

| Item | Value |
| --- | --- |
| Endpoint | `app.supabase.storage.endpoint` (path-style S3) |
| Region | `app.supabase.storage.region` (default `ap-southeast-1`) |
| Bucket | `app.supabase.storage.bucket` (default `golden-greetings`) |
| Auth | Static access key / secret |
| Object layout | `Wish Images/{date}/{uuid}{ext}`, `Chat/{date}/{uuid}{ext}`, `Email Images/{type}/{date}/{uuid}{ext}`, profile pictures by user id, `signature.png` |
| Write | `putObject` with detected `Content-Type` and, when gzip is on, `Content-Encoding: gzip` |
| Read | `getSignedUrl` (presigned GET) or public URL when `app.supabase.storage.public-base-url` is set |
| Delete | Best effort; a missing object does not fail the caller |
| Config | `SUPABASE_STORAGE_ENDPOINT`, `SUPABASE_STORAGE_REGION`, `SUPABASE_STORAGE_BUCKET`, `SUPABASE_STORAGE_ACCESS_KEY_ID`, `SUPABASE_STORAGE_SECRET_ACCESS_KEY`, `SUPABASE_STORAGE_PUBLIC_BASE_URL` |

## 10. Instagram

| Item | Value |
| --- | --- |
| Graph base | `instagram.graph-url` (default `https://graph.facebook.com/v18.0`) |
| Page token | `instagram.page-access-token` (placeholder `YOUR_PAGE_ACCESS_TOKEN` in the repository) |
| Webhook verify | `GET /instagram/webhook?hub.mode&hub.challenge&hub.verify_token` compared with `INSTA_VERIFY_TOKEN` |
| Webhook receive | `POST /instagram/webhook` logs the payload and returns `EVENT_RECEIVED` |
| Callback | `GET /instagram/callback?code` prints the code and returns a message |
| Outbound messages | `MessageDispatcher` sends and records `MessageLog` (`PENDING` → `SENT`/`FAILED`, retried by `RetryScheduler`) |
| Status | `GET /api/dashboard/insta`, `/api/dashboard/chart/insta` |

## 11. Google OAuth helper endpoints

`GoogleOAuthController` exposes a manual OAuth flow used to obtain a refresh token:

| Endpoint | Purpose |
| --- | --- |
| `GET /google/login` | Redirect to Google consent with the `gmail.send` scope, `access_type=offline`, `prompt=consent` |
| `GET /oauth2callback?code` | Exchange the code for tokens and return the provider response |
| `POST /gmail/send?accessToken` | Send a fixed test email (diagnostic only) |
| `POST /gmail/refresh?refreshToken` | Exchange a refresh token for an access token |

These endpoints are public (permit-all) and diagnostic; see
[../security/threat-model.md](../security/threat-model.md) T-06.

## 12. Integration resilience matrix

| Scenario | Behaviour | Recovery |
| --- | --- | --- |
| Gemini 429 on primary model | Next model tried | Automatic |
| Gemini all models failing | `503` to the caller; scheduler logs per recipient | Next scheduled run |
| Pollinations down | Email without image | Next wish gets a new attempt |
| Gmail 5xx | `FAILED` row | `retryFailed`, delivery-ops agent |
| Gmail refresh token revoked | `FAILED` with a clear log message | Re-authorise, update `GMAIL_REFRESH_TOKEN`, restart |
| Calendarific down | No festival change | Monthly job or next on-demand request |
| ip-api.com 429 | `status=fail`, not cached | Next lookup after the window |
| Supabase Storage down | Upload returns null → `400` for explicit uploads, email without image | Retry the operation |
| Chat DB down | Chat endpoints fail | Other endpoints unaffected |

## 13. Contract expectations

* All outbound calls use `RestTemplate` with default timeouts; a slow provider can delay the calling
  thread (email sends are `@Async`, wish generation runs in scheduler threads).
* Responses are parsed leniently: missing nodes produce empty results rather than exceptions where
  possible (`fetchHolidays`, `IpInfoService`).
* No provider SDK stores state on disk; credentials live only in memory.
