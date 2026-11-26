# External API integrations

Operational reference for each third-party service: what it is used for, how it is authenticated,
its limits, and what breaks when it fails. Design detail is in
[../architecture/integration-design.md](../architecture/integration-design.md).

## 1. Summary table

| Service | Used for | Auth | Free-tier limit | Failure impact |
| --- | --- | --- | --- | --- |
| Gmail API | Sending all email | OAuth 2.0 refresh token | Google account quota | No email; `FAILED` rows |
| SMTP (Gmail) | Alternative transport, failure alerts | Username + app password | Account quota | Alert email not sent |
| Brevo | Alternative transactional email | API key | Plan-based | Only if used |
| Google Gemini | Wish text, agents | `x-goog-api-key` | Per-model rate limits | `503`; wishes skipped |
| Pollinations | Greeting images | Optional bearer | Plan-based | Email without image |
| Calendarific | Festival dates | `api_key` query param | Free plan quota | Festival data stale |
| ip-api.com | IP geolocation | None | 45 requests/minute | Dashboard shows `fail` |
| Google Identity | SSO ID-token verification | Client id audience | — | SSO sign-in fails |
| Supabase Storage | All media | S3 access key/secret | Storage/egress quota | Uploads fail; emails without images |
| Instagram Graph | Birthday/festival messages | Page access token | Platform limits | Messages `FAILED` |
| UptimeRobot | Availability monitoring | n/a (external) | Free plan | No alerting |

## 2. Gmail API

| Item | Detail |
| --- | --- |
| Base | `https://gmail.googleapis.com/gmail/v1/users/me/messages/send` |
| Library | `google-api-services-gmail` + `google-auth-library-oauth2-http` |
| Credentials | `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN`, `GMAIL_FROM_EMAIL` |
| Scope | `https://www.googleapis.com/auth/gmail.send` |
| Call path | `GmailEmailServiceImpl.sendEmailWithAttachments` (`@Async`) |
| Body | MIME built with `MimeMessageHelper`: HTML, inline `birthdayImage`, inline `emailSignature`, attachments |
| Logging | One `email_logs` row per attempt |
| Failure | `status=FAILED`, `retry_count+1`, `error_message`; retried up to 3 times |
| Common error | `invalid_grant` — refresh token expired or revoked |
| Fix | Re-run consent, update `GMAIL_REFRESH_TOKEN`, restart (see [../operations/troubleshooting-guide.md](../operations/troubleshooting-guide.md)) |

## 3. SMTP (JavaMail)

| Item | Detail |
| --- | --- |
| Host / port | `smtp.gmail.com` / 587 with STARTTLS |
| Credentials | `MAIL_USERNAME`, `MAIL_PASSWORD` (app password) |
| Class | `EmailServiceImpl` |
| Uses | Alternative send path, `sendFailureAlert` for Instagram failures |
| Note | `MAIL_USERNAME` is also the default alert recipient and Brevo sender |

## 4. Brevo

| Item | Detail |
| --- | --- |
| Config | `brevo.api.key`, `brevo.api.url`, `brevo.sender.email`, `brevo.sender.name` |
| Class | `BrevoEmailServiceImpl` |
| Uses | Alternative transactional transport |
| Note | Not wired into the schedulers; kept as an option |

## 5. Google Gemini

| Item | Detail |
| --- | --- |
| Base URL | `GEMINI_API_URL` (base + primary model, parsed by `GeminiEndpoint`) |
| Auth | Header `x-goog-api-key: GEMINI_API_KEY` |
| Text call | `POST {base}/models/{model}:generateContent` |
| Model list | `GET {base}/models?pageSize=200` |
| Fallback | Up to `GEMINI_FALLBACK_MAX_RETRIES` (5) extra models |
| Cooldown | `GEMINI_FALLBACK_COOLDOWN_SECONDS` (60) per failed model |
| Catalogue cache | `GEMINI_FALLBACK_MODEL_CACHE_MINUTES` (60) |
| Exclusions | `GEMINI_FALLBACK_EXCLUDED_MODEL_PATTERN` (default excludes embedding/image/tts/audio/live/vision/veo/lyria/robotics/computer-use/aqa/preview/exp) |
| Retryable | 429, 404, 5xx, network errors |
| Non-retryable | 400, 401, 403 |
| Exhaustion | `AiUnavailableException` → 503 |
| Agents | Same endpoint with `systemInstruction`, `tools.functionDeclarations`, `toolConfig` |

## 6. Pollinations

| Item | Detail |
| --- | --- |
| Image URL | `POLLINATIONS_IMAGE_URL` (default `https://gen.pollinations.ai/image/`) |
| Model | `POLLINATIONS_IMAGE_MODEL` (default `imagen-4`) |
| Key | `POLLINATIONS_IMAGE_KEY` (optional) |
| Balance | `POLLINATIONS_BALANCE_URL` (default `https://gen.pollinations.ai/account/balance`) |
| Call | `GET {url}{prompt}?model={model}`, `Accept: image/png` |
| Failure | Warning logged; `null` returned; email sent without an image |
| Admin view | `GET /api/ai/pollinations/balance` |

## 7. Calendarific

| Item | Detail |
| --- | --- |
| URL | `CALENDARIFIC_API_URL` (default `https://calendarific.com/api/v2/holidays`) |
| Key | `CALENDARIFIC_API_KEY` |
| Country | `CALENDARIFIC_COUNTRY` (default `IN`) |
| Year | Current year for the monthly sync; requested year on demand |
| Response fields | `response.holidays[].name`, `date.iso` or `date.datetime` |
| Missing key | Warning; sync and on-demand fill skipped |
| Failure | Logged; existing rows untouched |

## 8. ip-api.com

| Item | Detail |
| --- | --- |
| Endpoint | `http://ip-api.com/json/{ip}?fields=status,message,country,countryCode,region,regionName,city,zip,lat,lon,timezone,isp,org,as,mobile,proxy,hosting,query` |
| Auth | None |
| Limit | 45 requests/minute (free tier) |
| Cache | `ipInfo`, 24 h, only successful results |
| Usage | Failed-login map (newest 300) and row enrichment |
| Failure | `status=fail` with the message; not cached |
| Note | Plain HTTP; no credentials or personal data are sent beyond the IP |

## 9. Google Identity (SSO)

| Item | Detail |
| --- | --- |
| Client id | `GOOGLE_SSO_CLIENT_ID` (defaults to `GMAIL_CLIENT_ID`) |
| Verification | `GoogleIdTokenVerifier` with the client id as audience |
| Requirements | Valid signature, matching audience, `emailVerified=true`, existing active user |
| Exposure | `GET /api/auth/sso/token` returns the client id to the UI |

## 10. Supabase Storage

| Item | Detail |
| --- | --- |
| Endpoint | `SUPABASE_STORAGE_ENDPOINT` (S3-compatible, path-style) |
| Region | `SUPABASE_STORAGE_REGION` (default `ap-southeast-1`) |
| Bucket | `SUPABASE_STORAGE_BUCKET` (default `golden-greetings`) |
| Keys | `SUPABASE_STORAGE_ACCESS_KEY_ID`, `SUPABASE_STORAGE_SECRET_ACCESS_KEY` |
| Public base | `SUPABASE_STORAGE_PUBLIC_BASE_URL` (optional) |
| Folders | `Wish Images/`, `Chat/`, email images by type, profile pictures, `signature.png` |
| Signed URLs | `S3Presigner` for private objects |
| Compression | `app.media.gzip.enabled` |
| Failure | Upload returns `null`; explicit uploads return `400` |

## 11. Supabase PostgreSQL (chat)

| Item | Detail |
| --- | --- |
| URL | `SUPABASE_DB_URL` |
| Credentials | `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD` |
| Pool | `SUPABASE_DB_POOL_SIZE` (10), min idle 5 |
| Schema | Created by `ChatSchemaInitializer` at startup |
| Failure | Chat endpoints fail; the rest of the API is unaffected |

## 12. Instagram

| Item | Detail |
| --- | --- |
| Graph base | `instagram.graph-url` (default `https://graph.facebook.com/v18.0`) |
| Page token | `instagram.page-access-token` |
| Verify token | `INSTA_VERIFY_TOKEN` |
| Webhook | `GET /instagram/webhook` (verification), `POST /instagram/webhook` (events, logged only) |
| Outbound | `MessageDispatcher` → `message_log` with retries |
| Status | `/api/dashboard/insta`, `/api/dashboard/chart/insta` |
| Note | The repository ships a placeholder page token; the feature is dormant until configured |

## 13. Google OAuth helper flow

Used once to obtain a refresh token:

1. `GET /google/login` → redirect to Google consent (`gmail.send`, `access_type=offline`,
   `prompt=consent`).
2. Google redirects to `gmail.redirect-uri` (`/oauth2callback`) → the endpoint exchanges the code and
   prints the tokens.
3. Copy the refresh token into `GMAIL_REFRESH_TOKEN` and restart.
4. `POST /gmail/send?accessToken=…` and `POST /gmail/refresh?refreshToken=…` are diagnostics.

These endpoints are public and should be removed or protected after setup.

## 14. Quota and cost controls in the code

| Control | Effect |
| --- | --- |
| Model fallback cap (5) | Bounds Gemini calls per request |
| Pollinations failure → `null` | Prevents retry storms |
| `ipInfo` 24 h cache | Keeps geolocation well under 45/min |
| `festivalsByMonth` 6 h cache | Fewer festival queries |
| Email retry cap (3) | Bounds Gmail calls |
| Delivery-ops retry budget (10/run) and one alert/run | Bounds agent-driven retries |
| `emailImages` 15 min / 30 entries | Avoids repeated storage downloads |

## 15. Integration checklist for a new environment

1. `JWT_SECRET` set (≥32 characters).
2. Primary and chat databases reachable; schemas created.
3. Storage bucket exists and the key can `PutObject`, `GetObject`, `DeleteObject`.
4. Gmail refresh token valid; send a test with `POST /api/email/test`.
5. `GEMINI_API_URL` + `GEMINI_API_KEY` valid; `POST /api/ai/generate-wish` returns JSON.
6. `CALENDARIFIC_API_KEY` set; trigger `festivalMonthlySyncScheduler`.
7. `UI_BASE_URL` points at the deployed UI so emailed links work.
8. `INSTA_VERIFY_TOKEN` set only if Instagram is used.
