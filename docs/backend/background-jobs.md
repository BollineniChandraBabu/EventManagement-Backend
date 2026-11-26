# Background jobs

## 1. Two schedulers

| Mechanism | Where | Durability | Used for |
| --- | --- | --- | --- |
| Quartz (JDBC, clustered) | `WishesSchedulerConfig` | Survives restarts; stored in `QRTZ_*` | Business jobs that must not be lost |
| Spring `@Scheduled` | Annotations on the class | In-process only | Instagram bridge, delivery-ops agent |

Quartz configuration: `job-store-type=jdbc`, `initialize-schema=always`, `isClustered=true`,
`instanceId=AUTO`, `tablePrefix=QRTZ_`, `PostgreSQLDelegate`, `useProperties=false`, thread pool 5.
All times are Asia/Kolkata.

## 2. Quartz jobs

| Job name | Class | Cron | Misfire | What it does |
| --- | --- | --- | --- | --- |
| `dailyWishJob` | `WishesScheduler` | `0 10 6 * * ?` (06:10) | `FireAndProceed` | Wishes for today's events; then `retryFailed()` |
| `goodMorningJob` | `GoodMorningScheduler` | `0 0 6 * * ?` (06:00) | `FireAndProceed` | Good-morning wish for users with the switch on |
| `birthdayJob` | `BirthdayMailScheduler` | `0 30 6 * * ?` (06:30) | `FireAndProceed` | Birthday wishes, once per user per day |
| `festivalWishScheduler` | `FestivalScheduler` | `0 30 7 * * ?` (07:30) | `FireAndProceed` | Festival wishes for today's active mappings |
| `goodNightJob` | `GoodNightScheduler` | `0 0 22 * * ?` (22:00) | `FireAndProceed` | Good-night wish for users with the switch on |
| `refreshTokenCleanupJob` | `RefreshTokenCleanupScheduler` | `0 0 0 * * ?` (00:00) | `FireAndProceed` | Deletes expired refresh tokens |
| `festivalMonthlySyncScheduler` | `FestivalSyncScheduler` | `0 0 2 1 * ?` (1st, 02:00) | `FireAndProceed` | Calendarific sync for the current year |
| `unreadChatEmailJob` | `UnreadChatEmailScheduler` | `app.chat.unread-mail.cron` (default `0 0 2 * * ?`) | `DoNothing` | Emails users with unread chat messages |
| `circleRequestReminderJob` | `CircleRequestReminderScheduler` | `app.circle.request.reminder-cron` (default `0 0 9 * * ?`) | `DoNothing` | Reminds addressees of stale requests |

> The unread-mail cron is validated and patched to Quartz syntax when needed (a 6-field expression
> ending in `*` becomes `?`).

## 3. Spring `@Scheduled` tasks

| Name (tracking) | Class | Cron | Notes |
| --- | --- | --- | --- |
| `instagramBirthdayScheduler` | `BirthdayScheduler` | `0 0 7 * * ?` | Creates `MessageLog` rows for today's Instagram birthdays and dispatches them |
| `instagramRetryScheduler` | `RetryScheduler` | `0 */15 * * * ?` | Retries `FAILED` messages with `retryCount < 3` |
| `instagramEmailAlertScheduler` | `EmailAlertScheduler` | `0 */30 * * * ?` | Emails an alert when more than 5 Instagram messages failed today |
| `deliveryOpsAgent` | `DeliveryOpsAgentScheduler` | `0 15,45 * * * ?` | Runs only when enabled and there is at least one failed email |

## 4. Per-job behaviour detail

### `WishesScheduler`
1. `findTodaysEvents(month, day)`.
2. Keep events where `recurring` is true or `eventDate` equals today.
3. For each: generate text, choose an uploaded image or generate one, append the wishes link, send.
4. After the loop, `retryFailed()` for emails with `retryCount < 3`.
5. A failure for one event is logged; the loop continues.

### `BirthdayMailScheduler`
1. `findTodaysBirthdays(month, day)`.
2. Keep users whose `wishSettings.birthdayEnabled` is true.
3. Skip when `last_birthday_wish_sent` equals today.
4. Send, then stamp `last_birthday_wish_sent`. A failure sends an alert email and does not stamp.

### `FestivalScheduler`
1. `findBySpecialEvent_EventDateAndActiveTrue(today)`.
2. Skip mappings already sent today without an error.
3. Send; on success `is_error = false`, on failure `is_error = true`; always set
   `last_wish_sent_on = today`.
4. Failures also email the alert address.

### `GoodMorningScheduler` / `GoodNightScheduler`
Iterate `findByGoodMorningEnabledTrue()` / `findByGoodNightEnabledTrue()`, generate and send. Errors
email the alert address. Both still print to stdout in addition to logging.

### `RefreshTokenCleanupScheduler`
`deleteByExpiresAtBefore(now)` and logs the count.

### `FestivalSyncScheduler`
Delegates to `FestivalServiceImpl.syncCalendarificFestivals` through
`SchedulerTrackingService.track`.

### `UnreadChatEmailScheduler`
`findUnreadCountsByReceiver()` → for each receiver, one email with the count and a login link.

### `CircleRequestReminderScheduler`
`circleService.sendPendingReminders(reminderAfterDays)` → for each due pending request, email the
addressee and stamp `lastReminderSentOn`.

## 5. Asynchronous work (not scheduled)

| Mechanism | Where | Notes |
| --- | --- | --- |
| `@Async` email send | `GmailEmailServiceImpl.sendEmailWithAttachments` | Executor core 5, max 10, queue 100 |
| `MessageDispatcher.sendAsync` | Instagram messages | Retries recorded in `message_log` |
| `NotificationServiceImpl` scheduler | `ConcurrentTaskScheduler` | Publishes/unpublishes at scheduled times; tasks are in memory and lost on restart |
| `AgentController` guard | `AtomicBoolean` | Prevents overlapping delivery-ops runs |

## 6. Tracking and control

* `QuartzJobTrackingListener` calls `SchedulerTrackingService.onStart/onComplete` for every Quartz
  job.
* `GET /api/schedulers` lists all jobs with counters, last status/duration/error and next/previous
  fire times.
* `POST /api/schedulers/{jobName}/trigger` fires a job immediately (useful after a failed run).
* Statistics are in memory and per instance.

## 7. Operational guidance

| Situation | Action |
| --- | --- |
| A wish run failed | `POST /api/schedulers/dailyWishJob/trigger` after fixing the cause |
| Wishes duplicated | Check for a second instance or a local run against production data |
| Festival dates missing for a year | Trigger `festivalMonthlySyncScheduler`, or request that year (on-demand fill) |
| Emails stuck FAILED | Check `email_logs.error_message`; fix Gmail credentials; `retryFailed` runs with the daily job |
| Reminders not sent | Confirm `app.circle.request.reminder-after-days` and the cron |
| Job never fires | Check `QRTZ_*` tables and the scheduler status endpoint |

## 8. Limitations

1. Job times (except the two configurable crons) are fixed in code.
2. Quartz tables are re-initialised on every start.
3. Notification scheduling is in memory; a restart loses a pending publish.
4. No dead-letter queue for failed async emails; the email log is the record.
5. Instagram jobs are not durable and run on every instance.
