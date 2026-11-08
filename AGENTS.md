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

## Circles and concurrent login

- Circles live in `circles` (entity `Circle`, `CircleRepository`,
  `CircleServiceImpl`, `CircleController` under `/api/circles`). Chat allows only
  accepted circles when `app.chat.circles-only=true`.
- A new circle request emails the addressee a one-click direct-login link (`EmailType.CIRCLE_REQUEST`,
  entity `LoginToken`). `CircleServiceImpl.notifyAddressee` issues the token and mails the link;
  `AuthServiceImpl.directLogin` (endpoint `POST /api/auth/direct-login`) consumes it and returns JWT
  tokens. Link base is `app.circle.request.login-url` (default `${app.ui.base-url}/direct-login`); TTL is
  `app.circle.request.login-ttl-minutes`.
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
