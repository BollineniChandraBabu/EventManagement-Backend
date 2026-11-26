# Design patterns

Only patterns that are actually present in the code are listed, with the class that shows them and
the reason they were chosen.

## 1. Layered architecture

**Where:** `controller` → `service` → `service.impl` → `repository` → `entity`.
**Why:** keeps HTTP concerns, business rules and persistence separate, which makes each layer
testable in isolation (`@WebMvcTest`-free unit tests with Mockito on services).

## 2. Repository

**Where:** 22 interfaces in `com.familywishes.repository` extending `JpaRepository`; derived queries
(`findByEmailAndDeletedFalse`), `@Query` JPQL (filters, month/day lookups) and `@Modifying` updates
(`markStaleUsersOffline`, `revokeAllByUserId`).
**Why:** removes boilerplate persistence code and keeps query definitions declarative.

## 3. Service layer / façade

**Where:** every controller depends on an interface (`AuthService`, `UserService`, …) whose only
implementation lives in `service.impl`.
**Why:** a stable contract for controllers and tests; implementations can change (for example
`AgenticAiService` replacing `AiServiceImpl`) without touching callers.

## 4. Data Transfer Object (record)

**Where:** all `dto` records, `PagedResponse<T>`, `AiWishRequest`/`AiWishResponse`.
**Why:** no entity leaks to the API, no lazy-loading surprises, explicit request validation, and
immutability for free.

## 5. Decorator (transparent replacement)

**Where:** `AgenticAiService` is `@Primary` and wraps `AiServiceImpl` (injected as
`@Qualifier("aiServiceImpl")`), adding agentic generation and falling back to the decorated service.
**Why:** every existing caller (schedulers, `UserServiceImpl`, `AiController`) gains the feature
without a single change, and the feature can be switched off with one property.

## 6. Strategy with fallback chain

**Where:** `GeminiClient.pickNextModel` iterates the primary model then `GeminiModelCatalog` models,
skipping models in cooldown; `MediaValidation` has three strategies (`validate`, `validateImage`,
`validateAttachmentAndDetect`) over shared helpers.
**Why:** provider deprecations and rate limits become an implementation detail instead of an outage.

## 7. Template method / shared base class

**Where:** `AuditableEntity` (`@PrePersist`/`@PreUpdate` set IST timestamps) and
`ActivatableEntity` (adds `active` and `toActiveFilter`) are `@MappedSuperclass` bases for entities.
**Why:** audit fields and activation semantics are defined once.

## 8. Chain of responsibility (filters)

**Where:** `RateLimitFilter` → `JwtAuthFilter` → Spring Security filter chain.
**Why:** each concern decides independently whether to stop the request (429, 401) or pass it on.

## 9. Circuit breaker (informal)

**Where:** `GeminiClient.unavailableUntil` marks a failed model unusable for
`app.gemini.fallback.cooldown-seconds`; `IpInfoService` caches successful lookups for 24 h and does
not cache failures.
**Why:** stops hammering a failing dependency and lets it recover.

## 10. Retry

**Where:** `EmailService.retryFailed` / `GmailEmailService.retryFailed` (`retryCount < 3`),
`RetryScheduler` for Instagram messages (`retryCount < 3`), `DeliveryOpsAgent.retry` with a per-run
budget.
**Why:** transient provider failures should not lose a wish; caps prevent infinite loops.

## 11. Idempotency key / guard

**Where:** `users.last_birthday_wish_sent`, `festival_wish_mappings.last_wish_sent_on` + `is_error`,
`chat_conversations` unique pair, `festival_wish_mappings` unique (festival, user), single-use
`otp_codes`/`password_reset_tokens`/`login_tokens` (`used` flag).
**Why:** repeated runs or retries must not duplicate side effects.

## 12. Observer / publish–subscribe

**Where:** STOMP topics `/topic/chat/{conversationId}` via `ChatRealtimePublisher` and
`ChatSocketController`; `NotificationRealtimePublisher` for announcements.
**Why:** many UI sessions can follow one conversation without polling.

## 13. Scheduler / job object

**Where:** Quartz `Job` implementations (`WishesScheduler`, `BirthdayMailScheduler`, …) with
`JobDetail`/`Trigger` beans; Spring `@Scheduled` methods.
**Why:** durable, clustered, independently triggerable units of work.

## 14. Builder

**Where:** Lombok `@SuperBuilder`/`@Builder` on entities (`User.builder()…`), `MimeMessageHelper`
construction, `ToolSchema.create().integer(...).build()`.
**Why:** readable construction of objects with many optional fields.

## 15. Adapter

**Where:** `SupabaseStorageService` adapts the S3 API to a storage interface used by services;
`GmailEmailServiceImpl` adapts the Gmail API to `GmailEmailService`; `IpInfoService` adapts the
ip-api.com JSON to `ViolatedUserIpInfo`.
**Why:** provider-specific code stays in one class.

## 16. Null object / graceful degradation

**Where:** `AiServiceImpl.callGeminiImage` returns `null` instead of throwing; `WishEmailExtras`
returns the HTML unchanged when a link or note is absent; `MediaContentTypes.detect` returns
`application/octet-stream` for unknown bytes.
**Why:** optional features stay optional in the call sites.

## 17. Tool-calling loop (agent pattern)

**Where:** `AgentRunner` with `AgentSpec`, `AgentTool`, `AgentResult`; terminal tools
(`submit_wish`) end the loop when they return `accepted=true`.
**Why:** a controlled, auditable way to let the model use application functions, with a hard step
cap and error feedback instead of exceptions.

## 18. Dependency injection (constructor)

**Where:** `@RequiredArgsConstructor` on every service, controller and component; explicit
constructors where `@Value` is needed.
**Why:** immutable collaborators and straightforward unit tests.

## 19. Aspect-style cross-cutting helpers

**Where:** `@Transactional`, `@Async`, `@Cacheable`/`@CacheEvict`, `@PreAuthorize`, `@Valid`.
**Why:** transaction, threading, caching and authorisation concerns are declared rather than coded.

## 20. Value object

**Where:** DTO records and small records inside `ChatMessageRepository`
(`ConversationSummary`, `AttachmentMeta`), `GeminiModelCatalog.ModelList`/`ModelInfo`,
`SchedulerDtos` records.
**Why:** groups related values without identity.

## 21. Patterns deliberately not used

| Pattern | Why not |
| --- | --- |
| Microservices | Family-scale load; operational cost would dominate |
| Event sourcing | No audit requirement beyond logs and email history |
| CQRS | Reads and writes are simple and share the same model |
| Saga / distributed transactions | The two databases hold independent data |
| Repository abstraction over chat SQL | Plain SQL with `DISTINCT ON` and counters is clearer |
| Message broker | Quartz and in-process `@Async` are sufficient |
