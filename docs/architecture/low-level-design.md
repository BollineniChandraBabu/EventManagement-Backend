# Low-level design

Class- and method-level design. Method lists are the public surface; private helpers are named where
the behaviour matters.

## 1. Application and configuration

| Class | Responsibility | Key members |
| --- | --- | --- |
| `FamilyWishesApplication` | Boot entry point; enables scheduling, async and caching | `main(String[])` |
| `SecurityConfig` | Stateless filter chain, CORS, password encoder, DAO provider | `securityFilterChain(HttpSecurity)`, `authenticationProvider()`, `passwordEncoder()`, `authenticationManager(AuthenticationConfiguration)` |
| `AppConfig` | Shared `RestTemplate` and CORS mappings | `restTemplate()`, `corsConfigurer()` |
| `GmailConfig` | Builds the `Gmail` client from OAuth credentials | `gmailService()`, `resolveAccessToken(UserCredentials)`, `isInvalidGrant(IOException)`, `extractHttpResponseException(Throwable)` |
| `WebSocketConfig` | STOMP endpoint `/ws-chat`, simple broker `/topic`, prefix `/app` | `registerStompEndpoints`, `configureMessageBroker` |
| `OpenApiConfig` | API metadata (title, contact, licence) | `familyWishesOpenApi()` |
| `PrimaryDataSourceConfig` | `@Primary` Hikari datasource from `spring.datasource.*` | `primaryDataSourceProperties()`, `primaryDataSource(...)` |
| `CacheNames` | Constants for every cache name | `SEED_DATA`, `EMAIL_IMAGES`, `IP_INFO`, `FESTIVALS_BY_MONTH`, `DASHBOARD_SUMMARY` |
| `WishTemplateSeedDataInitializer` | Seeds reference data at startup | `run(...)` |

## 2. Security

| Class | Responsibility | Key members |
| --- | --- | --- |
| `JwtService` | Sign and parse JWTs | `generateAccessToken(User)`, `generateAccessToken(User, String impersonatedBy)`, `generateRefreshToken(User)`, `generateRefreshToken(User, String)`, `extractSubject`, `extractImpersonatedBy`, `extractSessionId`, `isValid`, `getAccessTokenTtlSeconds` |
| `JwtAuthFilter` | Authenticate bearer tokens; enforce `sid` | `doFilterInternal`, `writeConcurrentLogin(HttpServletResponse)` |
| `RateLimitFilter` | Apply the limiter to ten auth paths | `doFilterInternal`, `writeTooManyRequests`, `clientIp(HttpServletRequest)` |
| `RateLimiter` | Sliding-window counters per key | `check(String key)`, `prune(Instant)` |
| `AppUserDetails` | Principal carrying `userId`, `concurrentLoginAllowed`, `activeSessionId` | constructor, getters |
| `CustomUserDetailsService` | Load a user by email into `AppUserDetails` | `loadUserByUsername(String)` |

**Constants that matter:** access TTL 900 s, refresh TTL 604 800 s, `sid` claim, `CONCURRENT_LOGIN`
marker, limited paths set, `MAX_TRACKED_KEYS = 100_000`.

## 3. Identity and people services

### `AuthServiceImpl implements AuthService`

| Method | Behaviour |
| --- | --- |
| `login(LoginRequest, HttpServletRequest)` | Load user → lock check → authenticate → reset counter → MFA gate → `enforceSingleSession` → tokens → store refresh → record location → `AuthResponse`; unknown email writes `violated_users` and may alert |
| `login(LoginRequest)` | Convenience overload without HTTP context |
| `adminLoginAsUser(AdminLoginAsUserRequest)` | Admin-only impersonation; rejects admin targets and inactive users; stamps `impersonatedBy` |
| `switchBackToAdmin(String authorizationHeader)` | Requires an `impersonatedBy` claim; mints an admin token |
| `refresh(RefreshRequest)` | Validates a non-revoked, unexpired refresh token; new access token; surfaces concurrent-login rejections |
| `sendOtp(OtpSendRequest)` | Reuses or creates a 6-digit OTP; resend cap; emails the code |
| `verifyOtp(OtpVerifyRequest)` | Validates and consumes an OTP; issues tokens |
| `verifyLoginOtp(LoginOtpVerifyRequest, HttpServletRequest)` | Same as above plus login-location recording |
| `forgotPassword(ForgotPasswordRequest)` | Reuses or creates a reset token; resend cap; emails the link |
| `resetPassword(ResetPasswordRequest)` | Consumes the token; sets the hash; invalidates other tokens |
| `resetPasswordWithEmail(ResetPasswordWithEmailRequest)` | Token + email variant |
| `changePassword(ChangePasswordRequest)` | Verifies the current password, rejects reuse, updates and invalidates reset tokens |
| `googleSsoLogin(GoogleSsoLoginRequest)` | Verifies the Google ID token, requires a verified email and an active account |
| `getSSOAuthToken()` | Returns the configured client id |
| `issueDirectLoginToken(Long userId)` | Reuses or creates a single-use login token |
| `directLogin(DirectLoginRequest, HttpServletRequest)` | Consumes the token, checks the optional email, issues tokens |

Private helpers: `authenticateUser`, `enforceSingleSession`, `recordLoginLocation`,
`resolveClientIp`, `buildAuthResponse`, `refreshRejection`, `verifyGoogleIdTokenAndExtractEmail`,
`insertViolatedUserInfo`, `buildOtpEmailBody`, `buildFailedLoginThresholdEmailBody`,
`buildUnknownUserLoginAttemptEmailBody`.

### `UserServiceImpl implements UserService`

| Method | Behaviour |
| --- | --- |
| `create(UserRequest)` | Validates relationship and wishes URL, encodes `Test`, creates user + wish settings |
| `update(UserRequest)` | Updates by email |
| `list(...)` | Admin listing with search/role/gender/relationship/active filters, paged |
| `directory(...)` | Reduced projection for circle search |
| `listByMonth(Integer)`, `listByMonthAndUserEmail(...)`, `listByMonthForUsers(Integer, List<Long>)` | Calendar support |
| `getById(Long)`, `getCurrentUser()` | Reads |
| `updateCurrentUserWishSettings(...)`, `updateCurrentUserMfa(...)`, `updateCurrentUserConcurrentLogin(...)`, `updateConcurrentLogin(Long, ...)` | Self-service settings; the last allows an admin or the same user |
| `getCurrentUserWishPreview()` | Generates a preview wish (text + image bytes) |
| `getCurrentUserProfilePictureUploadUrl(...)`, `uploadCurrentUserProfilePicture(...)`, `removeCurrentUserProfilePicture()` | Profile pictures, direct or presigned |
| `deactivate(Long)`, `deactivateByIds(List<Long>)`, `updateStatus(Long, boolean)` | Soft delete and activation |

Private helpers: `normalizeWishesUrl`, `toUserResponse`, `resolveProfilePictureUrl`,
`resolveRelationship`, `findAuthenticatedUser`, `isAdminAuthenticated`, `isSelf`,
`resolveDirectorySort`, `toDirectoryUserResponse`.

## 4. Date services

### `EventServiceImpl implements EventService`

`create(EventRequest)`, `create(EventRequest, MultipartFile)`, `update(Long, EventRequest)`,
`update(Long, EventRequest, MultipartFile)`, `getById(Long)`, `getById(Long, boolean isAdmin, String requesterEmail)`,
`deleteById(Long)`, `deleteByIds(List<Long>)`,
`list(page, size, searchKey, eventTypes, userIds, active, sortBy, sortDir, isAdmin, userEmail)`,
`listByMonth(Integer)`, `listByMonthAndUserEmail(Integer, String)`,
`listByMonthForUsers(Integer, List<Long>)`.

Private helpers: `resolveEventType` (upper-cases and requires an active seed), `normalizeWishesUrl`,
`uploadImage` (validates and stores), `imageUrl` (signed URL), `toResponse`, `toMonthResponse`,
`findEvent`.

### `FestivalServiceImpl implements FestivalService`

`listByMonth(Integer)` (`@Cacheable`), `listByMonthForYear(Integer, int)`,
`listByMonth(month, page, size, searchKey, sortBy, sortDir)`,
`listByMonthForYear(month, year, page, size, searchKey, sortBy, sortDir)`,
`syncCalendarificFestivals()` (`@CacheEvict(allEntries)`).

Private helpers: `ensureFestivalsForYear`, `fetchHolidays`, `upsertFestivals`, `extractDate`,
`normalizeFestivalName`, `toFestivalResponse`, `resolveSortBy`.

### `FestivalWishMappingServiceImpl implements FestivalWishMappingService`

`upsert`, `upsertWithImage`, `upsertBulk`, `list(...)` (single status or multi-filter),
`listForCurrentUser`, `summary`, `delete`, `deleteByIds`.

### `CalendarServiceImpl implements CalendarService`

`listByMonth(Integer month, Integer year, boolean isAdmin, String userEmail)` plus
`toCalendar(FestivalResponse, Integer)`, `toCalendar(EventResponse, Set<Long>)`,
`toCalendar(UserResponse, Set<Long>)`, `isFromCircle`, `mergeById`.

## 5. Social and communication services

### `CircleServiceImpl implements CircleService`

| Method | Behaviour |
| --- | --- |
| `listCircles()` | Returns `circles`, `incomingRequests`, `outgoingRequests` for the caller |
| `sendRequest(SendCircleRequest)` | Validates target; accepts when the target already asked; rejects duplicates; reuses rejected rows |
| `accept(CircleRequestAction)` / `reject(CircleRequestAction)` | Addressee-only; pending-only |
| `remove(CircleRequestAction)` | Participant-only |
| `statusWith(Long userId)` | `CircleStatusResponse(userId, status, inCircle, circleId)` |
| `areInCircle(Long, Long)` | Chat gate |
| `acceptedCircleMemberIds(Long)` | Calendar sharing |
| `sendPendingReminders(int days)` | Emails due reminders and stamps `lastReminderSentOn` |

Helpers: `acceptExisting`, `notifyNewRequest`, `notifyAccepted`, `requirePendingForAddressee`,
`isParticipant`, `otherUser`, `toCircleResponse`, `toRequestResponse`, `resolveProfilePictureUrl`.

### `CircleRequestNotifier`

`notifyNewRequest(Circle, User)`, `notifyPendingReminder(Circle)`, `notifyRequestAccepted(Circle)`,
`send(...)`, `buildLink(User)` (falls back to the plain circles URL when a token cannot be issued),
`buildRequestBody`, `buildReminderBody`, `buildAcceptedBody`, `ctaButton`, `escape`.

### `ChatServiceImpl implements ChatService`

`listAvailableUsers(...)`, `listActiveUsers(...)`, `sendMessage(SendMessageRequest, MultipartFile)`,
`sendRealtimeMessage(...)`, `getConversationMessages(...)`, `listConversations(...)`,
`listAllMessages(...)`, `heartbeat()`, `heartbeat(Long)`, `markOffline()`, `markOffline(Long)`,
`markConversationSeen(...)`, `unreadCount()`, `downloadAttachment(Long)`,
`downloadAttachmentWithMeta(Long)`, `deleteLastSentMessage(Long)`, `deleteMessageById(Long)`,
`deleteMessagesByIds(List<Long>)`, `editMessage(Long, EditMessageRequest)`, `listReactions(Long)`,
`reactToMessage(...)`, `likeMessage(...)`, `unlikeMessage(...)`.

Helpers: `ensureCanMessage`, `ensureCanAccessMessage`, `resolveConversationId`, `resolveMessageType`,
`updatePresence`, `isEffectivelyOnline`, `markStaleUsersOffline`, `nowIst`, `currentUser`,
`resolveProfilePictureUrl`, `resolveUserSort`.

### `ChatMessageRepository` (plain SQL)

Records: `MessageMeta`, `MessageReactionSummary`, `ConversationSummary`, `AttachmentMeta`.
Methods: `findConversationId`, `createConversation` (upsert), `insertMessage`,
`markConversationSeen`, `findConversationMessages`, `findConversationSummaries` (two overloads),
`countConversationSummaries`, `deleteLastSentMessage`, `deleteMessageById`, `findLastSentMessageMeta`,
`findMessageMetaForParticipant`, `updateMessageText`, `findAttachmentKeyByMessageIdForUser`,
`findAttachmentByMessageIdForUser`, `findUnreadCountsByReceiver`, `addReaction`, `removeReaction`,
`removeAllReactions`, `findReactions`, `findGlobalMessages`, `countUnreadMessagesForReceiver`.

### `NotificationServiceImpl implements NotificationService`

`create`, `update`, `getById`, `getAll` (two overloads), `delete`, `deleteByIds`, `publish`,
`unpublish`, `getPublished`.

Helpers: `pushNotificationToQueue`, `pushUnPublishNotificationToQueue`, `cancelNotification`,
`sendNotificationEmailIfEnabled`, `validateScheduleWindow`, `validateFutureSchedule`, `escapeHtml`,
`currentActor`, `ensureNotPublished`, `resolveSortBy`, `toResponse`. Scheduling uses a
`ConcurrentTaskScheduler` and a `Map<String, ScheduledFuture<?>>` keyed by `id + "Publish"` /
`id + "UnPublish"`.

## 6. Content and insight services

### `WishImageServiceImpl implements WishImageService`

`create(userId, eventType, active, file)`, `update(id, userId, clearUser, eventType, active, file)`,
`list(...)` (two overloads), `delete`, `deleteByIds`, `findRandomImage(String, Long)`,
`findRandomImage(Long, List<String>)`.

Helpers: `userCandidates`, `downloadAny`, `normalizeEvent` (upper-case, `_`→space variants, 150-char
cap), `validateImage` (image-only), `upload`, `toResponse`.

### `ThemeServiceImpl implements ThemeService`

`listThemes()`, `getMyTheme()`, `saveMyTheme(String code)`; validates the code against
`theme_seeds`/`UITheme` and upserts `user_theme_preferences`.

### `DashboardServiceImpl implements DashboardService`

`getDashboard(String requesterEmail, boolean isAdmin)` (`@Cacheable(dashboardSummary)`),
`getIGDashboard(...)`, `getMailChart(...)`, `getInstaChart(...)`, `getOtpDashboard()`,
`getForgotPasswordDashboard()`, `getOtpChart(...)`, `getForgotPasswordChart(...)`,
`getLoginLocationChart(...)`, `getViolatedUsersDashboard(...)`.

Helpers: `getSensitiveDashboard`, `getSensitiveChart`, `buildGraphResponse`, `toLoginLocationPoint`,
`topIps`, `topEmails`, `toViolatedUserInfo`, `toViolatedUserMapPoint`, `resolveIpInfo`,
`resolveEffectiveUserId`, `toDateCountMap`, `convertToLocalDate`. Constants:
`MAX_VIOLATED_MAP_POINTS = 300`, `TOP_VIOLATION_LIMIT = 5`, `MAX_LOGIN_LOCATION_POINTS = 1000`.

### `ViolatedUserAuditService`

`insertViolatedUserInfo(email, password, location, ip, latitude, longitude, loggedInAt)` — writes a
`violated_users` row. See the security caveat in [../security/threat-model.md](../security/threat-model.md).

## 7. AI and agents

| Class | Responsibility | Key members |
| --- | --- | --- |
| `AiServiceImpl` | Prompt building, text parsing, image selection/generation | `generate(AiWishRequest)`, `callGeminiImage(AiWishRequest)`, `getPollinationsBalance()`, `getWishPrompt`, `getImagePrompt` |
| `GeminiClient` | Call `generateContent` with model fallback | `generateContent(Map<String,Object>)`, `pickNextModel`, `callModel`, `isRetryable`, `markUnavailable`, `isAvailable` |
| `GeminiModelCatalog` | Discover text-capable models, cache, filter | `availableModels()`, `fetchModels`, `canGenerateText`, `isNotExcluded` |
| `GeminiEndpoint` | Parse the configured API URL into base URL + model | `parse(String)` |
| `AgenticAiService` | `@Primary` decorator over `AiServiceImpl` | `generate`, `callGeminiImage`, `getPollinationsBalance` |
| `AgentRunner` | Tool-calling loop with guardrails | `run(AgentSpec)`, `buildBody`, `firstCandidateContent`, `toResponseMap` |
| `WishComposerAgent` | Agentic wish writer with validator | `compose(AiWishRequest)`, `profileTool`, `recentWishesTool`, `submitTool` |
| `DeliveryOpsAgent` | Triage failed emails | `failedCount()`, `run()`, `failedEmails()`, `retry`, `alert` |
| `AdminCopilotAgent` | Read-only Q&A | `ask(String)`, `failedLogins`, `birthdays` |
| `AgentTool` / `AgentSpec` / `AgentResult` / `ToolSchema` | Tool contract, run spec, result, JSON schema builder | `of(...)`, `execute(...)` |
| `GeminiAgentClient` | Minimal Gemini call used by the runner | `generate(Map<String,Object>)` |

## 8. Delivery and storage

| Class | Responsibility | Key members |
| --- | --- | --- |
| `GmailEmailServiceImpl` | Primary email transport via Gmail API | `sendEmailWithAttachments(...)` (two overloads, `@Async`), `retryFailed`, `getStatusWithFilters`, `getStatus`, `getOtpStatus`, `getForgotPasswordStatus`, `getFestivalWishStatus`, `getUnreadChatMessageStatus`, `getStatusById`, `getImage`, `deleteEmailStatus`, `deleteEmailStatuses`, `sendTestEmail`, `sendEmailNow` |
| `EmailServiceImpl` | SMTP transport and failure alert | `sendHtmlEmail`, `retryFailed`, `sendFailureAlert` |
| `BrevoEmailServiceImpl` | Brevo HTTP transport | send methods |
| `EmailImageService` | Load an email image, cached | `load(Long id, String imageUrl)` |
| `EmailTemplateBuilder` | Wrap HTML with layout and signature | `build(String html, String signatureSource)` |
| `SupabaseStorageService` | S3-compatible storage | `uploadEmailImage`, `uploadWishImage`, `uploadChatAttachment`, `uploadUserProfilePicture`, `generateUserProfilePictureUploadUrl`, `getPublicUrl`, `getSignedUrl`, `resolveProfilePictureUrl`, `getDefaultProfilePictureUrl`, `getEmailSignatureUrl`, `getEmailSignatureImage`, `downloadImage`, `deleteObject`, `deleteByPublicUrl` |
| `MessageDispatcher` | Async Instagram message send with retry | `sendAsync(MessageLog)` |
| `IpInfoService` | Geolocation with cache | `lookup(String ipAddress)` |

## 9. Scheduling classes

| Class | Type | Trigger |
| --- | --- | --- |
| `WishesScheduler` | Quartz `Job` | `dailyWishTrigger` 06:10 |
| `GoodMorningScheduler` | Quartz `Job` | `goodMorningTrigger` 06:00 |
| `BirthdayMailScheduler` | Quartz `Job` | `birthdayTrigger` 06:30 |
| `FestivalScheduler` | Quartz `Job` | `festivalWishTrigger` 07:30 |
| `GoodNightScheduler` | Quartz `Job` | `goodNightTrigger` 22:00 |
| `RefreshTokenCleanupScheduler` | Quartz `Job` | `refreshTokenCleanupTrigger` 00:00 |
| `FestivalSyncScheduler` | Quartz `Job` | `festivalMonthlySyncTrigger` 1st 02:00 |
| `UnreadChatEmailScheduler` | Quartz `Job` | `unreadChatEmailTrigger` 02:00 |
| `CircleRequestReminderScheduler` | Quartz `Job` | `circleRequestReminderTrigger` 09:00 |
| `BirthdayScheduler` | Spring `@Scheduled` | `0 0 7 * * ?` (Instagram) |
| `RetryScheduler` | Spring `@Scheduled` | `0 */15 * * * ?` (Instagram retry) |
| `EmailAlertScheduler` | Spring `@Scheduled` | `0 */30 * * * ?` (failure alert > 5) |
| `DeliveryOpsAgentScheduler` | Spring `@Scheduled` | `0 15,45 * * * ?` (flagged) |
| `QuartzJobTrackingListener` | Quartz listener | start/complete hooks |
| `WishesSchedulerConfig` | Configuration | declares all Quartz `JobDetail`/`Trigger` beans |

## 10. Utilities

| Class | Key methods |
| --- | --- |
| `MediaContentTypes` | `detect(byte[])`, `isImage(String)`, `fileExtension(String)` |
| `MediaValidation` | `validate(MultipartFile)`, `validateAndDetect`, `validateImage`, `validateAttachmentAndDetect` |
| `GzipUtil` | `compress`, `decompress`, `isGzipped` |
| `WishesUrls` | `normalizeOrNull(String)` |
| `WishImageKeys` | `forWish(String event, String festival)` |
| `WishEmailExtras` | `appendWishesLink`, `appendPersonalNote`, `escape`, `insertAtEnd` |
| `EmailTemplateBuilder` | `build(html, signatureSource)` |
| `TimeUtil` | Instagram send-window check (`canSend(LocalDateTime)`) |

## 11. Entity hierarchy

```
AuditableEntity (createdAt, updatedAt in IST)
├── ActivatableEntity (active flag, toActiveFilter(Boolean))
│   ├── User, Event, EventTypeSeed, RelationshipSeed, TemplateTypeSeed, ThemeSeed,
│   │   SpecialEvent, WishImage, FestivalWishMapping
├── EmailLog, Circle, Notification, OtpCode, PasswordResetToken, RefreshToken,
│   LoginToken, LoginLocationEvent, UserThemePreference, UserWishSettings, InstagramUser,
│   MessageLog
ViolatedUser (standalone, no audit columns)
```

## 12. DTO inventory

`AuthDtos` (13 records), `UserDtos` (8), `EventDtos` (2), `CalendarDtos` (1), `CircleDtos` (6),
`ChatDtos` (11), `EmailDtos` (2), `FestivalDtos` (6), `NotificationDtos` (2), `SchedulerDtos` (2),
`ThemeDtos` (3), `WishImageDtos` (1), `SeedDtos` (4), `DashboardDtos` (12), `CommonDtos`
(`PagedResponse<T>`), `AiWishRequest`, `AiWishResponse`, `EmailImage`. All are immutable records;
the cached ones (`FestivalResponse`, `DashboardResponse`, `EmailImage`, `ViolatedUserIpInfo`)
implement `Serializable`.
