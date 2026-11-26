# Sequence diagrams

Interactions for the workflows that carry the most risk. Participants are abbreviated: **C** client,
**F** filters, **Ctl** controller, **S** service, **R** repository/DB, **X** external provider.

## 1. Password sign-in

```
C    F(RateLimit)  F(Jwt)   Ctl(Auth)   S(Auth)        R(primary)      X(Gmail)
│        │            │         │          │               │              │
├─POST──►│            │         │          │               │              │
│        ├─check(ip|path)        │          │               │              │
│        ├───ok──────►│         │          │               │              │
│        │            ├─no token►│         │               │              │
│        │            │         ├─login()─►│               │              │
│        │            │         │          ├─findByEmail──►│              │
│        │            │         │          │◄──user────────┤              │
│        │            │         │          ├─BCrypt verify │              │
│        │            │         │          ├─MFA? → 400 MFA_OTP_REQUIRED  │
│        │            │         │          ├─rotate sid / revoke refresh ►│
│        │            │         │          ├─generate tokens (no I/O)     │
│        │            │         │          ├─save refresh──►│              │
│        │            │         │          ├─save location─►│              │
│        │            │         │          ├─count unread──►│ (chat DB)    │
│        │            │         │◄─AuthResponse           │              │
│◄─200───────────────────────────┤          │               │              │
```

Alternate: bad password → increment `failed_login_attempts`; at 5 → email owner (**X**) and `400`.
Unknown email → insert `violated_users`; at 5 rows for that email → email admin (**X**) and `404`.

## 2. OTP sign-in

```
C            Ctl(Auth)          S(Auth)              R(primary)        X(Gmail)
├─POST /otp/send─►│                │                    │                │
│                 ├─sendOtp()─────►│                    │                │
│                 │                ├─find valid OTP────►│                │
│                 │                ├─create/reuse + resend cap           │
│                 │                ├─save OTP──────────►│                │
│                 │                └─send code──────────────────────────►│
├─POST /otp/verify-login─►│        │                    │                │
│                 ├─verifyLoginOtp►│                    │                │
│                 │                ├─validate/consume──►│                │
│                 │                ├─enforceSingleSession►│               │
│                 │                ├─tokens + refresh───►│                │
│                 │                ├─record location────►│                │
│◄─200 AuthResponse────────────────┤                    │                │
```

## 3. Forgot / reset password

```
C              Ctl(Auth)         S(Auth)                 R(primary)      X(Gmail)
├─POST /forgot-password─►│           │                       │              │
│                ├─forgotPassword──►│                       │              │
│                │                  ├─find valid reset token►│              │
│                │                  ├─create/reuse + resend cap            │
│                │                  ├─save token────────────►│              │
│                │                  └─email link (token+email)────────────►│
│                │                  │                       │              │
│ (user opens link with ?token&email)│                       │              │
├─POST /password-reset/confirm─►│    │                       │              │
│                ├─resetPasswordWithEmail►│                  │              │
│                │                  ├─find token+email+unused►│             │
│                │                  ├─BCrypt hash + save────►│              │
│                │                  └─invalidate other tokens►│             │
│◄─200 (no body)────────────────────┤                       │              │
```

## 4. Direct login from a circle-request email

```
C                 Ctl(Auth)        S(Auth)             R(primary)
├─POST /direct-login {token,email}─►│                   │
│                  ├─directLogin──►│                   │
│                  │               ├─find token+unused─►│
│                  │               ├─check expiry / email match
│                  │               ├─mark used─────────►│
│                  │               ├─enforceSingleSession►│
│                  │               ├─tokens + refresh───►│
│                  │               └─record location────►│
│◄─200 AuthResponse─────────────────┤                   │
```

## 5. Admin creates a user and an event

```
Admin        Ctl(User)     S(User)        Ctl(Event)     S(Event)      R(primary)   X(Storage)
 │              │             │               │              │              │            │
 ├─POST /api/users─►          │               │              │              │            │
 │              ├─create()───►│               │              │              │            │
 │              │             ├─resolve relationship ──────────────────────►│            │
 │              │             ├─validate wishes URL (WishesUrls)            │            │
 │              │             ├─BCrypt("Test") + save user + settings ─────►│            │
 │◄─UserResponse───────────────┤               │              │              │            │
 ├─POST /api/events/with-image (multipart) ────►              │              │            │
 │              │             │               ├─create(req,image)►          │            │
 │              │             │               │              ├─resolve type►│            │
 │              │             │               │              ├─MediaValidation
 │              │             │               │              ├─upload──────────────►│
 │              │             │               │              ├─save event───►│            │
 │◄─EventResponse (signed image URL)───────────────────────────┤            │
```

## 6. Daily event wish

```
Quartz   S(Wishes)   S(Ai/Agentic)   X(Gemini/Pollinations)   S(Gmail)   R(primary)   X(Gmail API)
  │          │              │                  │                  │          │            │
  ├─execute─►│              │                  │                  │          │            │
  │          ├─findTodaysEvents──────────────────────────────────────────►│            │
  │          ├─filter recurring/date                                     │            │
  │          ├─generate(req)►│                  │                  │          │            │
  │          │              ├─(agent or prompt)──►│                  │          │            │
  │          │              │◄─subject+html─────┤                  │          │            │
  │          │              ├─findRandomImage──────────────────────────────►│            │
  │          │              ├─(or generate image)─►│                  │          │            │
  │          │              ├─append wishes link                        │            │
  │          │              ├─sendEmailWithAttachments(@Async)─────────►│          │            │
  │          │              │                  │                  ├─save PENDING────►│            │
  │          │              │                  │                  ├─signature (storage)│          │
  │          │              │                  │                  ├─send──────────────────────►│
  │          │              │                  │                  ├─upload image (storage)      │
  │          │              │                  │                  └─save SENT/FAILED─►│            │
  │          ├─retryFailed (retryCount < 3)                             │            │
```

Per-recipient failures are caught and logged; the loop continues.

## 7. Circle request, reminder and acceptance

```
User A      Ctl(Circle)   S(Circle)    Notifier    S(Auth)   R(primary)   X(Gmail)
  │             │             │            │          │          │           │
  ├─POST /requests {userId:B}►│            │          │          │           │
  │             ├─sendRequest►│            │          │          │           │
  │             │             ├─findBetween ──────────────────►│           │
  │             │             ├─save PENDING ──────────────────►│           │
  │             │             ├─notifyNewRequest►│            │           │
  │             │             │            ├─issueDirectLoginToken►│        │
  │             │             │            │          ├─login_tokens►│       │
  │             │             │            └─email(link)───────────────────►│
  │◄─CircleRequestResponse─────┤            │          │          │           │
  │             │             │            │          │          │           │
(Quartz circleRequestReminderJob, daily 09:00, requests older than 7 days)
  │             │             ├─sendPendingReminders(days)      │           │
  │             │             ├─findPendingDueForReminder ──────►│           │
  │             │             ├─notifyPendingReminder ─────────►│───────────►│
  │             │             └─stamp lastReminderSentOn ───────►│           │
  │             │             │            │          │          │           │
User B      Ctl(Circle)   S(Circle)       │          │          │           │
  ├─POST /requests/accept {circleId}►│     │          │          │           │
  │             ├─accept───►│            │          │          │           │
  │             │          ├─requirePendingForAddressee ────────►│           │
  │             │          ├─status=ACCEPTED ───────────────────►│           │
  │             │          └─notifyRequestAccepted ──────────────┼──────────►│ (to A)
  │◄─CircleRequestResponse─┤            │          │          │           │
```

## 8. Chat message (HTTP + realtime)

```
Sender      Ctl(Chat)    S(Chat)     S(Circle)   R(chat DB)   X(Storage)   Broker
  │            │            │            │            │            │           │
  ├─POST /api/chat (multipart: payload, attachment)►│            │           │
  │            ├─sendMessage►│            │            │            │           │
  │            │            ├─ensureCanMessage►│       │            │           │
  │            │            │            ├─areInCircle►│            │           │
  │            │            ├─validateAttachmentAndDetect          │           │
  │            │            ├─uploadChatAttachment────────────────►│           │
  │            │            ├─createConversation (upsert)─────────►│           │
  │            │            ├─insertMessage──────────────────────►│           │
  │            │            └─publishMessage──────────────────────────────────►│
  │◄─MessageResponse────────┤            │            │            │           │
  │            │            │            │            │            │           │
Receiver     Ctl(Socket)  S(Chat)        │            │            │           │
  ├─SEND /app/chat.seen►│   │            │            │            │           │
  │            ├─markConversationSeen───►│            │            │           │
  │            │            ├─UPDATE chat_messages.seen_at────────►│           │
  │            │            └─convertAndSend /topic/chat/{id} SEEN────────────►│
```

Presence: `POST /api/chat/presence/heartbeat` (or `/app/chat.presence`) updates `users.online` and
`last_seen_at`; a user is reported offline when `last_seen_at` is older than 2 minutes.

## 9. Notification publish with schedule

```
Admin       Ctl(Notif)   S(Notif)      Scheduler(in-memory)   R(primary)   X(Gmail)
  │            │            │                  │                  │           │
  ├─POST /{id}/publish─►    │                  │                  │           │
  │            ├─publish───►│                  │                  │           │
  │            │            ├─validateFutureSchedule              │           │
  │            │            ├─unpublish previous ────────────────►│           │
  │            │            ├─publishedAt = scheduledFrom ?: now  │           │
  │            │            ├─if future: schedule(task)──────────►│           │
  │            │            │                  │  (at time) ──► publish + push│
  │            │            ├─if now: sendNotificationEmailIfEnabled────────►│
  │            │            └─publishNotification (topic)          │           │
  │◄─NotificationResponse───┤                  │                  │           │
```

## 10. Manual scheduler trigger

```
Admin      Ctl(Scheduler)   S(SchedulerMgmt)   Quartz        Job            Tracking
  │            │                 │               │            │                │
  ├─GET /api/schedulers►│        │               │            │                │
  │            ├─getAllStatuses►│               │            │                │
  │            │                ├─getJobKeys────►│            │                │
  │            │                ├─next/prev fire►│            │                │
  │            │                ├─stats (in-memory)───────────────────────────►│
  │◄─PagedResponse──────────────┤               │            │                │
  ├─POST /{job}/trigger►│        │               │            │                │
  │            ├─trigger───────►│               │            │                │
  │            │                ├─triggerJob────►│            │                │
  │            │                │               ├─execute───►│                │
  │            │                │               │            ├─onStart───────►│
  │            │                │               │            ├─onComplete────►│
  │◄─SchedulerTriggerResponse───┤               │            │                │
```

## 11. Festival monthly sync

```
Quartz   S(Festival)   X(Calendarific)   R(primary)   Ehcache
  │          │                │              │           │
  ├─execute─►│                │              │           │
  │          ├─GET holidays ─►│              │           │
  │          │◄─holidays──────┤              │           │
  │          ├─find existing by name+year ──►│           │
  │          ├─upsert rows ─────────────────►│           │
  │          ├─deactivate removed rows ─────►│           │
  │          └─evict festivalsByMonth ───────────────────►│
```

## 12. Delivery-ops triage (agent)

```
Scheduler/Admin   Ctl(Agent)   DeliveryOpsAgent   AgentRunner   Gemini   R(email_logs)  X(Gmail)
      │              │                │                │           │          │            │
      ├─POST /delivery-ops/run─►      │                │           │          │            │
      │              ├─run()────────►│                │           │          │            │
      │              │                ├─failedCount ──────────────────────►│            │
      │              │                ├─run(spec)─────►│           │          │            │
      │              │                │                ├─prompt───►│          │            │
      │              │                │                │◄─tool call list_failed_emails      │
      │              │                │                ├─execute ────────────►│            │
      │              │                │                ├─retry_email ────────────────────►│
      │              │                │                ├─send_admin_alert ─────────────────►│
      │              │                │◄─final report──┤           │          │            │
      │◄─{ran, report, dryRun}────────┤                │           │          │            │
```

Guardrails: maximum 8 steps, retry budget `app.agent.delivery-ops.max-retries-per-run`, at most one
alert per run, OTP and password-reset emails are never retried, and dry-run mode describes actions
without executing them.
