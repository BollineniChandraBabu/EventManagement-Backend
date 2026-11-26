# Data flow

Each flow lists the steps and the data stores touched, so a change can be traced end to end.

## 1. Sign-in (password)

```
Client ──POST /api/auth/login {email,password,location,ip,lat,lon}──► RateLimitFilter
  └─► JwtAuthFilter (no token) ─► AuthController ─► AuthServiceImpl.login
        ├─ UserRepository.findByEmailAndDeletedFalse        [users]
        ├─ AuthenticationManager (BCrypt compare)           [users.password]
        ├─ enforceSingleSession                              [users.active_session_id,
        │                                                     refresh_tokens (revoke)]
        ├─ JwtService.generateAccessToken/RefreshToken       (no I/O)
        ├─ RefreshTokenRepository.save                       [refresh_tokens]
        ├─ LoginLocationEventRepository.save                 [login_location_events]
        └─ ChatMessageRepository.countUnreadMessagesForReceiver [chat DB]
  ◄── 200 AuthResponse {accessToken, refreshToken, role, expiresIn, unreadChatMessagesCount}
```

Failure branches write `users.failed_login_attempts` or `violated_users`, and may send an alert
email through the mail component.

## 2. Daily event wish (scheduled)

```
Quartz dailyWishTrigger (06:10 IST)
  └─► WishesScheduler.execute
        ├─ EventRepository.findTodaysEvents(month, day)              [events]
        ├─ filter(recurring || eventDate == today)
        ├─ for each event:
        │    ├─ AiService.generate(AiWishRequest) ──► GeminiClient ──► Gemini API
        │    │        └─ (optional) AgenticAiService → WishComposerAgent → tools
        │    ├─ WishImageService.findRandomImage(userId, keys)       [wish_images]
        │    ├─ (fallback) AiService.callGeminiImage ──► Pollinations
        │    ├─ WishEmailExtras.appendWishesLink(html, wishesUrl, label)
        │    └─ GmailEmailService.sendEmailWithAttachments(@Async)
        │           ├─ EmailLogRepository.save(PENDING)              [email_logs]
        │           ├─ SupabaseStorageService.getEmailSignatureImage [storage]
        │           ├─ EmailTemplateBuilder.build
        │           ├─ Gmail API messages().send                     [Gmail]
        │           ├─ SupabaseStorageService.uploadEmailImage       [storage]
        │           └─ EmailLogRepository.save(SENT|FAILED)          [email_logs]
        └─ GmailEmailService.retryFailed (retryCount < 3)
```

The same shape applies to `birthdayJob` (06:30, with `users.last_birthday_wish_sent`),
`goodMorningJob` (06:00), `goodNightJob` (22:00) and `festivalWishScheduler` (07:30, with
`festival_wish_mappings.last_wish_sent_on` and `is_error`).

## 3. Festival sync

```
Quartz festivalMonthlySyncTrigger (1st, 02:00) ─► FestivalSyncScheduler
  └─► FestivalServiceImpl.syncCalendarificFestivals
        ├─ GET Calendarific /holidays?api_key&country=IN&year=YYYY
        ├─ upsert seed_special_events (name + year)                  [seed_special_events]
        ├─ deactivate current-year rows no longer returned           [seed_special_events]
        └─ @CacheEvict(festivalsByMonth, allEntries)                 [Ehcache]
```

On-demand path: `CalendarServiceImpl.listByMonth(month, year, …)` →
`FestivalServiceImpl.listByMonthForYear(month, year)` → `ensureFestivalsForYear(year)` →
`existsByEventDateBetween` → fetch + upsert when the year is empty.

## 4. Calendar read

```
Client ──GET /api/calendar?month&year──► CalendarController ─► CalendarServiceImpl
  ├─ FestivalService.listByMonth / listByMonthForYear   [seed_special_events, Ehcache]
  ├─ UserService.getCurrentUser()                       [users]
  ├─ CircleService.acceptedCircleMemberIds(me)          [circles]
  ├─ admin: EventService.listByMonth, UserService.listByMonth
  │  user : listByMonthAndUserEmail + listByMonthForUsers(memberIds)
  ├─ mergeById(own, fromCircle)  (own wins)
  └─ map to CalendarResponse(id, eventType, eventName, eventDate, active, recurring,
                             ownerName, ownerUserId, fromCircle)
  ◄── 200 [CalendarResponse]
```

## 5. Circle request → chat

```
POST /api/circles/requests {userId}
  └─► CircleServiceImpl.sendRequest
        ├─ validate target / existing circle                    [circles, users]
        ├─ save PENDING circle                                  [circles]
        └─ CircleRequestNotifier.notifyNewRequest (best effort)
              ├─ AuthService.issueDirectLoginToken(addressee)   [login_tokens]
              └─ GmailEmailService.sendEmailWithAttachments     [email_logs, Gmail]

POST /api/auth/direct-login {token,email}
  └─► AuthServiceImpl.directLogin → consume token [login_tokens] → tokens [refresh_tokens]

POST /api/circles/requests/accept {circleId}
  └─► CircleServiceImpl.accept → status ACCEPTED [circles] → notifyRequestAccepted [email_logs]

POST /api/chat (multipart payload + attachment)
  └─► ChatServiceImpl.sendMessage
        ├─ ensureCanMessage → CircleService.areInCircle          [circles]
        ├─ MediaValidation.validateAttachmentAndDetect
        ├─ SupabaseStorageService.uploadChatAttachment           [storage]
        ├─ ChatMessageRepository.createConversation (upsert)     [chat DB]
        ├─ ChatMessageRepository.insertMessage                   [chat DB]
        └─ ChatRealtimePublisher.publishMessage → /topic/chat/{id}
```

## 6. Upload and read media

```
Upload (event image, wish image, profile picture, chat attachment)
  ├─ MediaValidation: size + byte sniffing + declared-type agreement
  ├─ SupabaseStorageService.upload…: GzipUtil.compress (when enabled)
  │    └─ S3 PUT {folder}/{date}/{uuid}{ext}  (Content-Type, Content-Encoding: gzip)
  └─ object key stored in the entity column

Read
  ├─ Email image : GET /api/emails/status/{id}/image → EmailImageService (cache 15 min)
  │                → storage download → GzipUtil.decompress → bytes + content type
  ├─ Chat file   : GET /api/chat/messages/{id}/attachment → storage → bytes + type + file name
  └─ Signed URL  : SupabaseStorageService.getSignedUrl(key) for events, wish images, pictures
```

## 7. Notification publish

```
POST /api/notifications/{id}/publish
  └─► NotificationServiceImpl.publish
        ├─ validate window (must be in the future)              [notifications]
        ├─ unpublish previous published row                     [notifications]
        ├─ publishedAt = scheduledFrom ?: now                   [notifications]
        ├─ future  → schedule task (ConcurrentTaskScheduler)     [in-memory]
        └─ now     → sendNotificationEmailIfEnabled             [email_logs, Gmail, users]
                     + NotificationRealtimePublisher → /topic/notifications
```

## 8. Dashboard read

```
GET /api/dashboard/violated-users?startDate&endDate&page&size&searchKey
  └─► DashboardServiceImpl.getViolatedUsersDashboard
        ├─ ViolatedUserRepository queries (totals, chart, page, top IPs/emails) [violated_users]
        ├─ IpInfoService.lookup(ip) for the newest 300 map points
        │     └─ @Cacheable(ipInfo, 24h) unless status != success → ip-api.com
        └─ assemble ViolatedUsersDashboardResponse
```

## 9. Data ownership summary

| Data | Owner | Read by |
| --- | --- | --- |
| Users, settings, themes | Primary DB | Auth, User, Calendar, Chat (presence), Dashboard |
| Events, types | Primary DB | Event, Calendar, Wishes scheduler |
| Festivals, mappings | Primary DB | Festival, Calendar, Festival scheduler |
| Email logs | Primary DB | Email, Dashboard, Delivery ops |
| Security events | Primary DB | Auth, Dashboard, Copilot |
| Circles | Primary DB | Circle, Calendar, Chat gate |
| Chat messages | Chat DB | Chat, Unread digest |
| Media | Supabase Storage | Email, Event, Wish image, Chat, Profile |
