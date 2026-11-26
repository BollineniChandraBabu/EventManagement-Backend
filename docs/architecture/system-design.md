# System design

This document describes how the modules are built and how they interact. For the class-level view
see [low-level-design.md](low-level-design.md); for the running system see
[deployment-architecture.md](deployment-architecture.md).

## 1. Module map

```
com.familywishes
├── FamilyWishesApplication        @SpringBootApplication @EnableScheduling @EnableAsync @EnableCaching
├── config/                        SecurityConfig, AppConfig (CORS, RestTemplate), GmailConfig,
│                                  WebSocketConfig, OpenApiConfig, PrimaryDataSourceConfig,
│                                  CacheNames, WishTemplateSeedDataInitializer
├── security/                      JwtService, JwtAuthFilter, RateLimitFilter, RateLimiter,
│                                  AppUserDetails, CustomUserDetailsService
├── controller/                    22 REST controllers
├── service/                       interfaces (AuthService … WishImageService)
│   └── impl/                      implementations + notifiers, dispatchers, storage, IP lookup
├── repository/                    22 Spring Data JPA repositories
├── entity/                        JPA entities, AuditableEntity/ActivatableEntity, enums, converter
├── dto/                           request/response records (incl. PagedResponse)
├── exception/                     NotFound, BadRequest, RateLimitExceeded, ConcurrentLogin,
│                                  AiUnavailable, GlobalExceptionHandler
├── scheduler/                     Quartz jobs + WishesSchedulerConfig + tracking listener
├── chat/                          chat datasource, SQL repository, schema initializer, STOMP
├── ai/                            GeminiClient, GeminiModelCatalog, GeminiEndpoint
├── agent/                         AgentRunner, AgentTool, AgentSpec, three agents, GeminiAgentClient
└── util/                          MediaContentTypes, MediaValidation, GzipUtil, WishesUrls,
                                   WishImageKeys, WishEmailExtras, EmailTemplateBuilder, TimeUtil
```

## 2. Identity design

**Token model**

| Token | Format | TTL | Storage | Purpose |
| --- | --- | --- | --- | --- |
| Access | JWT HS256, claims `sub`, `scope`, `sid`, optional `impersonatedBy` | 15 min | none | Authorise API calls |
| Refresh | JWT HS256, `scope=REFRESH` | 7 days | `refresh_tokens` (revocable) | Mint a new access token |
| OTP | 6-digit numeric | 5 min | `otp_codes` | Second factor or passwordless login |
| Password reset | UUID | 30 min | `password_reset_tokens` | Reset a forgotten password |
| Direct login | UUID | 60 min | `login_tokens` | Sign in from an emailed link |

**Single-session enforcement.** Every sign-in mints a new `users.active_session_id` (UUID) and puts
it in the access token as `sid`. When `concurrent_login_allowed=false`, earlier refresh tokens are
revoked. `JwtAuthFilter` compares the token's `sid` with the current value and returns
`401 CONCURRENT_LOGIN: …` when they differ. The same signal is produced by `refresh()` when the
refresh token was revoked by a newer single-session sign-in.

**Sign-in paths** all converge on the same steps: authenticate → check MFA → `enforceSingleSession`
→ mint tokens → store refresh token → record login location → build `AuthResponse`.

**Impersonation.** `adminLoginAsUser` mints a token for the target user with `impersonatedBy` set to
the admin's email. `switchBackToAdmin` reads that claim and mints an admin token again.

**Lockout.** `users.failed_login_attempts` increments per bad password and resets on success. At the
threshold (5) the account is refused and the owner is emailed. Unknown-email attempts are written to
`violated_users` and, at the threshold, reported to the administrator.

## 3. Request/response design

* **DTOs** are immutable records in `com.familywishes.dto`; controllers never expose entities.
* **Validation** uses Jakarta Bean Validation (`@NotBlank`, `@Email`, `@NotNull`, `@Size`).
  `MethodArgumentNotValidException` is mapped to `400` with the first field message.
* **Lists** use `PagedResponse<T>` and accept `page`, `size`, `searchKey`, `sortBy`, `sortDir` and
  feature-specific filters. `filter-values` endpoints expose the legal filter values.
* **Ownership rules** are applied in services: a non-admin listing events is filtered by their email;
  reading another user's event returns `404`.
* **Media responses** stream bytes with the stored content type (`/api/emails/status/{id}/image`,
  `/api/chat/messages/{messageId}/attachment`) instead of embedding base64 in JSON.

## 4. Wish generation design

```
AiWishRequest(name, relation, event, festival, tone, language, userId)
        │
        ▼
AgenticAiService (@Primary, app.agent.wish.enabled)
        │ enabled ──► WishComposerAgent ──► AgentRunner ──► GeminiAgentClient
        │                ├─ tool get_recipient_profile(userId)
        │                ├─ tool get_recent_wishes(userId)
        │                └─ terminal tool submit_wish(subject, htmlMessage)
        │                     └─ rejects placeholders / duplicates → model retries
        │ disabled or failed
        ▼
AiServiceImpl.generate
        ├─ prompt: name, sender, relation, festival, tone, language, JSON-only instruction
        ├─ GeminiClient.generateContent (model fallback, cooldown, 503 on exhaustion)
        └─ parse JSON → AiWishResponse(subject, htmlMessage)

AiServiceImpl.callGeminiImage
        ├─ WishImageService.findRandomImage(userId, WishImageKeys.forWish(event, festival))
        └─ else GET {POLLINATIONS_IMAGE_URL}{prompt}?model={model}  → bytes or null
```

Both the wish text and the image are produced before the email is sent. The image is optional; the
text is not. `EmailServiceImpl`/`GmailEmailServiceImpl` then render the shared template, attach the
image inline (`cid:birthdayImage`) when it is an image, and log the attempt.

## 5. Email delivery design

| Aspect | Design |
| --- | --- |
| Transport | `GmailEmailServiceImpl` builds a MIME message, base64url-encodes it and calls
  `gmail.users().messages().send("me", …)`; `EmailServiceImpl` uses `JavaMailSender`; `BrevoEmailServiceImpl` uses the Brevo HTTP API |
| Threading | `@Async` on the send method so the caller is not blocked |
| Template | `EmailTemplateBuilder` wraps the generated HTML with the shared layout and signature (`cid:emailSignature` or a URL) |
| Logging | `email_logs` row per attempt: recipient, subject, body, status, type, retry count, error, sent time, image key |
| Type | Explicit `EmailType` when the caller passes one, otherwise keyword classification |
| Retry | `retryFailed()` re-sends rows with `status=FAILED` and `retryCount<3` |
| Sensitive types | `OTP`, `FORGOT_PASSWORD`, `CIRCLE_REQUEST` are hidden from non-admin readers |
| Images | Stored in Supabase Storage; the log keeps the object key in `image_url`; `EmailImageService` loads it on demand and caches it for 15 minutes |

## 6. Scheduling design

Two scheduling mechanisms coexist:

* **Quartz (JDBC, clustered)** for durable business jobs declared as `JobDetail`/`Trigger` beans in
  `WishesSchedulerConfig`: `dailyWishJob`, `goodMorningJob`, `birthdayJob`, `festivalWishScheduler`,
  `goodNightJob`, `refreshTokenCleanupJob`, `festivalMonthlySyncScheduler`, `unreadChatEmailJob`,
  `circleRequestReminderJob`. Misfires are handled with `FireAndProceed` (catch up) or `DoNothing`
  (skip), depending on the job.
* **Spring `@Scheduled`** for the Instagram bridge and the delivery-ops agent:
  `instagramBirthdayScheduler` (07:00), `instagramRetryScheduler` (every 15 min),
  `instagramEmailAlertScheduler` (every 30 min), `deliveryOpsAgent` (at :15 and :45).

`QuartzJobTrackingListener` records start/completion in `SchedulerTrackingService`, which
`SchedulerManagementService` exposes through `/api/schedulers`. The tracking map is in memory, so it
resets on restart and is per instance.

## 7. Chat design

| Aspect | Design |
| --- | --- |
| Storage | Separate PostgreSQL (`app.supabase.db.*`), plain SQL through `NamedParameterJdbcTemplate` |
| Schema | Created by `ChatSchemaInitializer` (`CREATE TABLE IF NOT EXISTS` + `ALTER TABLE … ADD COLUMN IF NOT EXISTS` + indexes) |
| Conversations | One row per unordered pair, enforced by `uq_chat_conversation(user_a_id, user_b_id)`; `createConversation` is an upsert |
| Messages | Text, encrypted payload + algorithm + key id, message type (`TEXT`/voice/etc.), voice duration, attachment key/name/type, reply id, sent/seen timestamps |
| Reactions | `chat_message_reactions` unique per (message, user, emoji) |
| Authorisation | `ensureCanMessage` consults `CircleService.areInCircle` when `app.chat.circles-only=true`; attachment and message access are checked against participation |
| Realtime | `SimpMessagingTemplate` publishes to `/topic/chat/{conversationId}`; clients send to `/app/chat.send`, `/app/chat.seen`, `/app/chat.presence`, `/app/chat.typing` |
| Presence | `heartbeat`/`offline` update `users.online` and `last_seen_at`; a user is effectively online only if seen within 2 minutes; a query marks stale users offline |
| Unread | Counted in SQL per receiver; included in `AuthResponse` and `/api/chat/unread-count`; emailed nightly |

## 8. Calendar design

`CalendarServiceImpl.listByMonth(month, year, isAdmin, userEmail)`:

1. Festivals for the month (year-scoped fetch when a year is given).
2. Resolve the caller and `circleService.acceptedCircleMemberIds(meId)`.
3. Admin: all events and birthdays of the month. User: own entries, plus circle members' entries when
   the circle is non-empty.
4. Merge by id (`LinkedHashMap.putIfAbsent`) so own entries win over duplicates.
5. Map each entry to `CalendarResponse` with `eventType` (`Festival`/`Anniversary`/`Birthday`),
   `recurring` (year-scoped festivals `false`, birthdays `true`, events from their flag),
   `ownerUserId` and `fromCircle`.

## 9. Security design summary

* Stateless bearer authentication; CSRF disabled because no cookie session exists.
* CORS allow-list: localhost:4200, `*.firebaseapp.com`, `chandu-goldengreetings.web.app`, `*.web.app`.
* Method-level authorisation on admin endpoints; ownership checks inside services.
* Rate limiting before authentication on sensitive paths.
* Uploads validated by bytes and size; storage keys generated server-side (UUID + detected extension).
* All secrets injected from environment; none in the repository.
* See [../security/security-design.md](../security/security-design.md).

## 10. Design trade-offs recorded

| Trade-off | Chosen because | Cost |
| --- | --- | --- |
| One datasource per concern instead of one | Chat writes are high-volume and independent | No joins, no distributed transactions |
| SQL for chat instead of JPA | Precise control of upserts, `DISTINCT ON`, counters | Manual mapping |
| In-process cache instead of Redis | No extra service | Per-instance, cold starts |
| Fallback across Gemini models instead of a fixed model | Provider deprecations and rate limits | Extra latency on failure, more API calls |
| Agent with validator instead of one prompt | Output quality and no placeholders | More tokens, optional feature flag |
| Hibernate schema update | Speed of change for a small team | No migration history |

## 11. Related documents

* [high-level-design.md](high-level-design.md) · [low-level-design.md](low-level-design.md)
* [design-patterns.md](design-patterns.md) · [data-flow.md](data-flow.md) ·
  [sequence-diagrams.md](sequence-diagrams.md)
* [../backend/backend-design.md](../backend/backend-design.md) ·
  [../backend/business-logic.md](../backend/business-logic.md)
