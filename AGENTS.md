# AGENTS.md

## Build and test

Spring Boot 3.3.3, Java 17, Gradle. The build pins a Java 17 toolchain
(`java.toolchain.languageVersion = JavaLanguageVersion.of(17)`), so a JDK 17 must be
available to Gradle even if you launch it with a newer JDK.

```bash
# Point Gradle at a JDK 17 (put this in ~/.gradle/gradle.properties, not the repo):
#   org.gradle.java.installations.paths=/path/to/jdk-17
JAVA_HOME=/path/to/jdk-21 ./gradlew compileJava   # Gradle still compiles with the 17 toolchain
./gradlew test
./gradlew spotlessApply                            # google-java-format
```

Notes:
- The repo is not fully spotless-clean; `spotlessCheck` reports pre-existing violations. Format
  only the files you touch (`spotlessApply` reformats the whole tree, so revert unrelated files).
- `src/test` runs with JUnit 5 + Mockito. `spring.servlet.multipart.max-file-size` (50 MB) caps
  uploads before application validation runs.

## Media uploads

- `util/MediaContentTypes` detects a media type from the first bytes (images, video, audio, PDF).
- `util/MediaValidation` validates uploads (byte sniffing + declared-type agreement + size limits):
  `validate` (any supported media), `validateImage` (image-only), `validateAttachmentAndDetect`
  (any file, for chat).
- `SupabaseStorageService` stores the detected content type and a matching extension.
- `util/GzipUtil` gzips stored media. `SupabaseStorageService` compresses on write (labelling the
  object `Content-Encoding: gzip`) and expands on read. Toggle with `app.media.gzip.enabled`.

## Festivals and the calendar

- `FestivalServiceImpl.syncCalendarificFestivals` (run monthly by `FestivalSyncScheduler`) fetches
  Calendarific holidays for `Year.now()` and upserts them, keyed by name and year, so each year
  keeps its own dates.
- The monthly sync only covers the current year. A calendar request for any other year is filled on
  demand: `FestivalServiceImpl.listByMonthForYear` calls `ensureFestivalsForYear`, which fetches that
  year from Calendarific once, stores it, and is served from the database on later visits.
- `CalendarController` takes an optional `year`; `CalendarServiceImpl.listByMonth(month, year, ...)`
  asks the festival service for that year and marks a year-scoped festival `recurring=false`, so a
  later year shows its own festivals instead of repeating one year's dates. Without a year the older
  month-only behaviour is kept (festival repeats yearly on its month/day).
- Only a one-time anniversary is date-exact: `toCalendar(EventResponse)` passes through
  `event.recurring()`.

## Circles and concurrent login

- Circles live in `circles` (entity `Circle`, `CircleRepository`,
  `CircleServiceImpl`, `CircleController` under `/api/circles`). Chat allows only
  accepted circles. Chat is restricted to circle members by default
  (`app.chat.circles-only`, default `true`); set `APP_CHAT_CIRCLES_ONLY=false` to open chat to everyone.
- A new circle request emails the addressee a one-click direct-login link (`EmailType.CIRCLE_REQUEST`,
  entity `LoginToken`). `CircleRequestNotifier` composes and sends the mail (initial note and the
  daily reminder) and issues the token; accepting a request also emails the requester a confirmation
  (`notifyRequestAccepted`), sent from `CircleServiceImpl.acceptExisting`. `AuthServiceImpl.directLogin`
  (endpoint `POST /api/auth/direct-login`) consumes it and returns JWT tokens. Link base is
  `app.circle.request.login-url` (default `${app.ui.base-url}/direct-login`); TTL is
  `app.circle.request.login-ttl-minutes`.
- A pending request is reminded daily once it is older than `app.circle.request.reminder-after-days`
  (default 7). `CircleRequestReminderScheduler` (Quartz, cron `app.circle.request.reminder-cron`,
  default `0 0 9 * * ?`) calls `CircleService.sendPendingReminders`, which emails each request that
  has not been reminded today and stamps `Circle.lastReminderSentOn`. Accept/reject stops it.
- Emailed deep links all derive from one property, `app.ui.base-url` (env `UI_BASE_URL`):
  `app.login.ui-url`, `app.password-reset.ui-url`, `app.circle.request.login-url`. Each can be
  overridden individually (e.g. `PASSWORD_RESET_UI_URL`).
- `User.concurrentLoginAllowed` (default true) gates single-session sign-in. `AuthServiceImpl`
  calls `enforceSingleSession`, which revokes earlier refresh tokens when the flag is false and
  mints a fresh `User.activeSessionId`. That id rides in the access token as the `sid` claim, and
  `JwtAuthFilter` rejects a request whose `sid` no longer matches the user's current session (401
  with a `CONCURRENT_LOGIN:` message). `refresh()` surfaces the same signal when the refresh token
  was revoked by a newer single-session sign-in. `UserServiceImpl.updateConcurrentLogin` allows an
  admin or the user themselves.
