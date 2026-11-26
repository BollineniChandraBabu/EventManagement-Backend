# API specification

Base URL: `https://eventmanagement-backend-ka9x.onrender.com` (production) or
`http://localhost:8080` (local). Swagger UI: `/swagger-ui.html`. OpenAPI JSON: `/v3/api-docs`.

## 1. Conventions

| Item | Value |
| --- | --- |
| Content type | `application/json` unless stated; uploads use `multipart/form-data` |
| Authentication | `Authorization: Bearer <accessToken>` on everything except the public paths in §2 |
| Date format | `yyyy-MM-dd`; date-times ISO-8601 without offset, in IST |
| Pagination | `page` (0-based), `size`; response `PagedResponse` |
| Sorting | `sortBy`, `sortDir=asc|desc` |
| Errors | `{ "timestamp": "...", "status": 400, "message": "..." }` |
| Success with no body | `200` with an empty body (`void` handlers) |

`PagedResponse<T>`:

```json
{ "content": [], "page": 0, "size": 10, "totalElements": 0,
  "totalPages": 0, "hasNext": false, "hasPrevious": false }
```

Common status codes: `200` OK, `201` Created (wish-image create only), `204` No Content (wish-image
delete only), `400` validation/business rule, `401` missing/invalid token or superseded session,
`403` role not allowed, `404` missing or hidden resource, `409` concurrent agent run, `413` upload too
large, `429` rate limited, `500` unhandled, `503` AI unavailable or agent disabled.

## 2. Public paths (no token required)

`/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/health`, `/actuator/**`,
`/google/**`, `/oauth2callback`, `/gmail/**`, `/instagram/**`, `/ws-chat/**`,
`/api/notifications/**`.

> `/api/notifications/**` is permit-all at the URL level; admin methods still require
> `ROLE_ADMIN` via `@PreAuthorize`. Only `GET /api/notifications/published` is truly public.

## 3. Authentication — `/api/auth`

| Method | Path | Auth | Body / params | Response |
| --- | --- | --- | --- | --- |
| POST | `/login` | public | `{email, password, loginLocation?, ipAddress?, latitude?, longitude?}` | `AuthResponse` |
| POST | `/refresh` | public | `{refreshToken}` | `AuthResponse` |
| POST | `/admin/login-as-user` | admin | `{email}` | `AuthResponse` |
| GET | `/admin/switch-back` | authenticated | `Authorization` header | `AuthResponse` |
| POST | `/otp/send` | public | `{email}` | empty |
| POST | `/otp/verify` | public | `{email, otp}` | `AuthResponse` |
| POST | `/otp/verify-login` | public | `{email, otp, loginLocation?, ipAddress?, latitude?, longitude?}` | `AuthResponse` |
| POST | `/forgot-password` | public | `{email}` | empty |
| POST | `/password-reset/confirm` | public | `{email, token, newPassword}` | empty |
| POST | `/change-password` | authenticated | `{currentPassword, newPassword}` | empty |
| POST | `/sso/google` | public | `{idToken}` | `AuthResponse` |
| POST | `/direct-login` | public | `{token, email?, loginLocation?, ipAddress?, latitude?, longitude?}` | `AuthResponse` |
| GET | `/sso/token` | public | — | `{clientId}` |

`AuthResponse`:

```json
{ "accessToken": "eyJ…", "refreshToken": "eyJ…", "tokenType": "Bearer",
  "role": "ROLE_USER", "expiresIn": 900, "unreadChatMessagesCount": 3 }
```

Examples:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"secret","loginLocation":"Hyderabad"}'
```

Failure modes: `400` `Invalid email or password`, `400` lockout message, `400` `MFA_OTP_REQUIRED`,
`404` `User not found`, `401` `CONCURRENT_LOGIN: …`, `429` rate limited.

## 4. Users — `/api/users`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `` | admin | `UserRequest` → `UserResponse`; initial password `Test` |
| PUT | `` | admin | Updates by `email` in the body |
| GET | `/{id}` | authenticated | Full `UserResponse` (not restricted to admin/self — see known issues) |
| GET | `/directory` | authenticated | `page,size,searchKey,sortBy,sortDir` → `DirectoryUserResponse` page |
| GET | `/filter-values` | admin | `{roles, genders, active}` |
| GET | `` | admin | `page,size,searchKey,roles,?genders,?relationships,?active,sortBy,sortDir` |
| GET | `/me` | authenticated | Current user |
| PATCH | `/me/wish-settings` | authenticated | `{isGoodMorningEnabled?, isGoodNightEnabled?, isBirthdayEnabled?}` |
| PATCH | `/me/mfa` | authenticated | `{mfaEnabled}` |
| PATCH | `/me/concurrent-login` | authenticated | `{concurrentLoginAllowed}` |
| PATCH | `/{id}/concurrent-login` | admin or self | `{concurrentLoginAllowed}` |
| GET | `/me/wish-preview` | authenticated | `WishPreviewResponse{showMessage,wishType,subject,htmlMessage,imageData}` |
| POST | `/me/profile-picture/presigned-url` | authenticated | `{fileName?, contentType?}` → `{uploadUrl,publicUrl,objectKey,method,expiresInSeconds}` |
| POST | `/me/profile-picture` | authenticated | multipart `file` (image only) |
| DELETE | `/me/profile-picture` | authenticated | Removes the picture |
| POST | `/{id}/deactivate` | admin | Soft delete |
| DELETE | `` | admin | `?ids=1,2` soft delete |
| PATCH | `/{id}/status` | admin | `{active}` |

`UserRequest`:

```json
{ "name":"Chandra", "email":"c@example.com", "role":"ROLE_USER", "gender":"MALE",
  "dateOfBirth":"1990-05-14", "relationShip":"FRIEND",
  "isGoodMorningEnabled":true, "isGoodNightEnabled":false, "isBirthdayEnabled":true,
  "birthdayWishesUrl":"https://example.com/wishes", "mfaEnabled":false,
  "concurrentLoginAllowed":true }
```

`UserResponse` fields: `id, name, email, role, gender, dateOfBirth, active, relationShip,
isGoodMorningEnabled, isGoodNightEnabled, isBirthdayEnabled, online, lastSeenAt, profilePictureUrl,
birthdayWishesUrl, mfaEnabled, concurrentLoginAllowed`.

`DirectoryUserResponse` fields: `id, name, email, relationShip, online, lastSeenAt, profilePictureUrl`.

## 5. Events — `/api/events`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `` | admin | `EventRequest` |
| POST | `/with-image` | admin | multipart: `eventType, eventDate?, recurring=false, userId, wishesUrl?, image?` |
| PUT | `/{id}` | admin | `EventRequest` |
| PUT | `/{id}/with-image` | admin | multipart as above |
| GET | `/{id}` | admin or owner | `404` for anyone else |
| GET | `/filter-values` | authenticated | `{active:[true,false]}` |
| GET | `` | authenticated | `page,size,searchKey,?eventTypes,?userIds,?active,sortBy,sortDir`; non-admin sees own |
| DELETE | `/{id}` | admin | Deletes the event and its image |
| DELETE | `` | admin | `?ids=1,2` |

`EventRequest`: `{eventType, eventDate?, recurring, userId, wishesUrl?}`.

`EventResponse`: `{id, eventType, eventDate, recurring, userName, active, userId, wishesUrl, imageUrl}`.
`eventType` is the seed **code** in list/create responses and the display name in calendar responses.

## 6. Festivals and mappings

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/api/festivals` | authenticated | `month` (required), `year?`, `page,size,searchKey,sortBy,sortDir` |
| POST | `/api/festival-wish-mappings` | admin | `{specialEventId, userId, active, customMessage?, wishesUrl?}` |
| POST | `/api/festival-wish-mappings/with-image` | admin | multipart with the same fields + `image?` |
| GET | `/api/festival-wish-mappings/filter-values` | admin | `{statuses:[ALL,ACTIVE,INACTIVE,FAILED]}` |
| GET | `/api/festival-wish-mappings` | admin | `status?` or `specialEventIds?, userIds?, statuses?` + paging |
| POST | `/api/festival-wish-mappings/bulk` | admin | `{specialEventId, userIds[], active, customMessage?}` → `{created, updated}` |
| GET | `/api/festival-wish-mappings/summary` | admin | `{total, active, inactive, upcoming, failed}` |
| GET | `/api/festival-wish-mappings/me` | authenticated | Caller's mappings, paged |
| DELETE | `/api/festival-wish-mappings/{id}` | admin | |
| DELETE | `/api/festival-wish-mappings` | admin | `?ids=1,2` |

`FestivalResponse`: `{id, eventName, eventDate, active}`.
`FestivalWishMappingResponse`: `{id, specialEventId, festivalName, eventDate, userId, userName, active,
customMessage, imageUrl, lastWishSentOn, error, wishesUrl}`.

## 7. Calendar — `/api/calendar`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `` | authenticated | `month?`, `year?` (both optional; send `month` — when omitted the month filters match nothing) |

`CalendarResponse`: `{id, eventType, eventName, eventDate, active, recurring, ownerName, ownerUserId,
fromCircle}` where `eventType` is `Festival`, `Anniversary` or `Birthday`.

## 8. Circles — `/api/circles`

| Method | Path | Auth | Body | Response |
| --- | --- | --- | --- | --- |
| GET | `` | authenticated | — | `{circles[], incomingRequests[], outgoingRequests[]}` |
| POST | `/requests` | authenticated | `{userId}` | `CircleRequestResponse` |
| POST | `/requests/accept` | authenticated (addressee) | `{circleId}` | `CircleRequestResponse` |
| POST | `/requests/reject` | authenticated (addressee) | `{circleId}` | empty |
| DELETE | `` | authenticated (participant) | `{circleId}` | empty |
| GET | `/status/{userId}` | authenticated | — | `{userId, status, inCircle, circleId}` |

`CircleResponse`: `{circleId, userId, name, email, profilePictureUrl, online, lastSeenAt}`.
`CircleRequestResponse`: `{circleId, userId, name, email, profilePictureUrl, incoming, status,
createdAt}`.

## 9. Chat — `/api/chat`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/users` | authenticated | `page,size,searchKey,sortBy,sortDir` → `ChatUserResponse` page |
| GET | `/users/active` | authenticated | Same, only online users |
| GET | `/conversations` | authenticated | `page,size,searchKey` → `ConversationResponse` page |
| POST | `` | authenticated | multipart: part `payload` (JSON) + optional part `attachment` |
| GET | `/messages/{otherUserId}` | authenticated | `page=0,size=30,markSeen=true` → `MessagePageResponse` |
| GET | `/messages/{messageId}/attachment` | participant | Binary body with content type and `Content-Disposition: inline` |
| DELETE | `/messages/last/{otherUserId}` | authenticated | Deletes the caller's last message in that conversation |
| DELETE | `/messages/{messageId}` | participant | Deletes one message |
| DELETE | `/messages` | participant | `?ids=1,2` |
| PATCH | `/messages/{messageId}` | sender | `{messageText}` |
| GET | `/messages/{messageId}/reactions` | participant | `MessageReactionsResponse` |
| POST | `/messages/{messageId}/reactions` | participant | `{emoji}` |
| POST | `/messages/{messageId}/like` | participant | `{emoji}` (adds a reaction) |
| DELETE | `/messages/{messageId}/like` | participant | `{emoji}` (removes it) |
| POST | `/presence/heartbeat` | authenticated | Marks the caller online |
| POST | `/presence/offline` | authenticated | Marks the caller offline |
| GET | `/messages/all` | authenticated | `page,size,searchKey` → `GlobalMessagePageResponse` |
| GET | `/unread-count` | authenticated | `{unreadCount}` |

`SendMessageRequest` (the `payload` part):

```json
{ "receiverId": 2, "messageText": "hello", "encryptedMessage": null,
  "encryptionAlgorithm": null, "encryptionKeyId": null,
  "messageType": "TEXT", "voiceDurationSeconds": null, "replyToMessageId": null }
```

`MessageResponse`: `{messageId, conversationId, senderId, receiverId, replyToMessageId,
replyMessageText, messageText, encryptedMessage, encryptionAlgorithm, encryptionKeyId, messageType,
voiceDurationSeconds, attachmentKey, attachmentFileName, attachmentContentType, sentAt, seenAt, mine}`.

`ConversationResponse`: `{conversationId, otherUserId, otherUserName, otherUserEmail,
otherUserOnline, otherUserLastSeenAt, otherUserProfilePictureUrl, lastMessage, lastMessageAt,
lastSeenMessageAt, unreadCount}`.

WebSocket (STOMP over SockJS at `/ws-chat`):

| Destination | Payload |
| --- | --- |
| `/app/chat.send` | `{senderId, receiverId, messageText, encryptedMessage?, encryptionAlgorithm?, encryptionKeyId?, messageType?, voiceDurationSeconds?}` |
| `/app/chat.seen` | `{viewerId, otherUserId, conversationId}` |
| `/app/chat.presence` | `{userId, online}` |
| `/app/chat.typing` | `{userId, conversationId, typing}` |
| Subscribe `/topic/chat/{conversationId}` | messages, `SEEN`, `TYPING`, presence events |

## 10. Notifications — `/api/notifications`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `` | admin | `{title(≤150), message, canSendEmail?, scheduledFrom?, scheduledTo?}` |
| GET | `/{id}` | admin | |
| GET | `/filter-values` | admin | Filter values for the UI |
| GET | `` | admin | `page,size,searchKey,sortBy,sortDir` |
| PUT | `/{id}` | admin | Update while unpublished |
| DELETE | `/{id}` | admin | |
| DELETE | `` | admin | `?ids=1,2` |
| POST | `/{id}/publish` | admin | Unpublishes the previous one; schedules when in the future |
| POST | `/{id}/unpublish` | admin | |
| GET | `/published` | **public** | Currently published notification |

`NotificationResponse`: `{id, title, message, published, canSendEmail, scheduledFrom, scheduledTo,
publishedAt, createdBy, updatedBy, createdAt, updatedAt}`.

## 11. Emails — `/api/email` and `/api/emails`

Both prefixes map to the same controller.

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/test` | authenticated | Sends a test email to the caller |
| POST | `/send-now` | admin | `{userEmail, subject, body, eventTypeCode}` |
| GET | `/status/{id}` | recipient or admin | `EmailStatusResponse`; sensitive types are admin-only |
| GET | `/status/{id}/image` | recipient or admin | Image bytes, `Cache-Control: private, max-age=600` |
| GET | `/filter-values` | authenticated | `{statuses, emailTypes}` |
| GET | `/status` | authenticated | `page,size,searchKey,mailTab=ALL,?statuses,?emailTypes,sortBy,sortDir` |
| GET | `/status/admin/otp` | admin | OTP emails only |
| GET | `/status/admin/forgot-password` | admin | Password-reset emails only |
| GET | `/status/festival-wishes` | authenticated | Festival wishes (own, or all for admin) |
| DELETE | `/status/{id}` | admin | |
| DELETE | `/status` | admin | `?ids=1,2` |
| GET | `/status/unread-chat-messages` | authenticated | Unread-chat digests |

`EmailStatusResponse`: `{id, toEmail, subject, body, hasImage, status, emailType, sentAt}`.

`mailTab` values: `ALL`, `OTP`, `FORGOT_PASSWORD`, `GOOD_MORNING`, `GOOD_NIGHT`, `BIRTHDAY`, `EVENT`
(unknown values fall back to the non-sensitive set).

## 12. Dashboards — `/api/dashboard`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/mail` | authenticated | `{totalUsers, upcomingEvents, emailsSentToday, failedEmails}` |
| GET | `/insta` | authenticated | Instagram counters |
| GET | `/chart` | authenticated | Alias of the Instagram chart |
| GET | `/chart/mail` | authenticated | `startDate`, `endDate` (different days required) |
| GET | `/chart/insta` | authenticated | `startDate`, `endDate` |
| GET | `/mail/otp` | admin | OTP counters |
| GET | `/mail/forgot-password` | admin | Reset counters |
| GET | `/chart/mail/otp` | admin | OTP chart |
| GET | `/chart/mail/forgot-password` | admin | Reset chart |
| GET | `/chart/login-locations` | authenticated | `userId?`, `startDate?`, `endDate?`, `userIds?` |
| GET | `/violated-users` | admin | `startDate`, `endDate`, `page=0`, `size=50` (1–500), `includeIpInfo=true`, `searchKey` (≤100) |
| GET | `/chat/messages` | authenticated | Alias of `/api/chat/messages/all` |
| GET | `/chat/users/active` | authenticated | Alias of `/api/chat/users/active` |

`DashboardGraphResponse`: `{days, points:[{date, sent, failed, total}]}`.
`LoginLocationChartResponse`: `{userId, fromDate, toDate, points:[{email, userName, location,
ipAddress, latitude, longitude, loggedInAt}], truncated}`.
`ViolatedUsersDashboardResponse`: `{startDate, endDate, totalAttempts, chartPoints[], mapPoints[],
users[], page, size, totalElements, totalPages, hasNext, hasPrevious, uniqueIps, uniqueEmails,
topIps[], topEmails[]}`.

## 13. Themes — `/api/themes`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `` | authenticated | `[{id, code, displayName}]` |
| GET | `/me` | authenticated | `{theme}` |
| PUT | `/me` | authenticated | `{theme:"liquid-glass"}` → `{theme}`; unknown code → `400` |

## 14. Seed data

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST/GET | `/api/seed/event-types`, `/api/seed/event-types/{id}` | admin for writes | `{code, displayName, active}` |
| PUT | `/api/seed/event-types/{code}` | admin | Update by code |
| GET | `/api/seed/event-types/filter-values` | authenticated | Filter values |
| DELETE | `/api/seed/event-types/{id}`, `?ids=` | admin | |
| POST/PUT/GET/DELETE | `/api/seed/relationships` (same shape) | admin | `{code, displayName, active}` |
| POST/PUT/GET | `/api/seed/template-types` | admin | No delete endpoint |
| POST/GET/PUT/DELETE | `/api/seed/special-events` | admin | `{eventName, eventDate, message, active}` |
| GET | `/api/seed/special-events/today` | admin | Active seeds for today (IST) |

`EnumSeedResponse`: `{id, category, code, displayName, active}`.
`SpecialEventSeedResponse`: `{id, eventName, eventDate, message, active}`.

## 15. Wish images — `/api/admin/wish-images` (admin)

| Method | Path | Notes |
| --- | --- | --- |
| POST | `` | multipart: `userId?`, `eventType`, `active=true`, `file` → `201` |
| PUT | `/{id}` | multipart: `userId?`, `clearUser=false`, `eventType?`, `active?`, `file?` |
| GET | `/filter-values` | `{active:[true,false]}` |
| GET | `` | `page,size,searchKey,?eventType,?userId,?eventTypes,?userIds,?active,sortBy,sortDir` |
| DELETE | `/{id}` | `204` |
| DELETE | `` | `?ids=1,2` → `204` |

`WishImageResponse`: `{id, userId, userName, eventType, imageUrl, active, createdAt, updatedAt}`.

## 16. AI — `/api/ai`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/generate-wish` | authenticated | `{name, relation, event?, festival?, tone, language, userId?}` → `{subject, htmlMessage}` |
| GET | `/pollinations/balance` | authenticated | Provider balance JSON |

## 17. Agents — `/api/agent` (admin)

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/status` | `{wishComposer:{enabled}, deliveryOps:{enabled,dryRun,failedEmails}, copilot:{enabled}}` |
| POST | `/admin/ask` | `{question(≤500)}` → `{answer}`; `503` when disabled |
| POST | `/delivery-ops/run` | `{dryRun, ran, report}`; `409` when already running; `503` when disabled |

## 18. Schedulers — `/api/schedulers`

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/{jobName}` | `SchedulerStatusResponse` |
| GET | `` | `page,size,searchKey,sortBy,sortDir` |
| POST | `/{jobName}/trigger` | `{name, message, triggeredAt}` |

`SchedulerStatusResponse`: `{name, type, running, totalRuns, successRuns, failedRuns, lastStartedAt,
lastCompletedAt, lastDurationMs, lastStatus, lastError, nextFireTime, previousFireTime}`.

Known job names: `dailyWishJob`, `goodMorningJob`, `birthdayJob`, `festivalWishScheduler`,
`goodNightJob`, `refreshTokenCleanupJob`, `festivalMonthlySyncScheduler`, `unreadChatEmailJob`,
`circleRequestReminderJob`.

## 19. Health, Instagram and OAuth helper

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/health` | public | `{"status":"UP","time":"…"}` |
| GET | `/instagram/webhook` | public | Verifies `hub.verify_token`; returns the challenge |
| POST | `/instagram/webhook` | public | Logs the payload; returns `EVENT_RECEIVED` |
| GET | `/instagram/callback` | public | Diagnostic; prints the code |
| GET | `/google/login` | public | Redirects to Google consent |
| GET | `/oauth2callback` | public | Exchanges the code for tokens |
| POST | `/gmail/send` | public | Diagnostic send with an access token |
| POST | `/gmail/refresh` | public | Diagnostic token refresh |

## 20. Endpoint count

Approximately 160 request mappings are declared (161 `@GetMapping`/`@PostMapping`/
`@PutMapping`/`@PatchMapping`/`@DeleteMapping` annotations). Per area:

| Area | Endpoints |
| --- | --- |
| Auth | 13 |
| Users | 16 |
| Events | 9 |
| Festivals/mappings | 10 |
| Calendar | 1 |
| Circles | 6 |
| Chat | 18 |
| Notifications | 11 |
| Emails | 14 |
| Dashboards | 14 |
| Themes | 3 |
| Seeds | 20 |
| Wish images | 6 |
| AI | 2 |
| Agents | 3 |
| Schedulers | 3 |
| Health/Instagram/OAuth | 8 |
