# Configuration reference

All configuration comes from `src/main/resources/application.properties` (and
`application-prod.properties` for the `prod` profile), with values overridden by environment
variables. Precedence: environment variable → profile property → base property → built-in default.

Legend: **R** = required (startup fails or the feature is unusable without it), **C** = required only
for that feature, **O** = optional with a working default.

## 1. Server and application

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `server.port` | `SERVER_PORT` | `8080` (prod only; 8080 by default) | O | HTTP port |
| `scheduler.time-zone` | `SCHEDULER_TIME_ZONE` | `Asia/Kolkata` | O | Zone for all jobs, timestamps and date math |
| `alert.failure.threshold` | `ALERT_FAILURE_THRESHOLD` | `5` | O | Instagram failure alert threshold |
| `alert.email.to` | `ALERT_EMAIL_TO` | `${MAIL_USERNAME}` | C | Recipient of operational alerts |
| `management.endpoints.web.exposure.include` | — | `*` | O | Actuator exposure (restrict in production) |
| `springdoc.api-docs.path` | — | `/v3/api-docs` | O | OpenAPI JSON path |
| `springdoc.swagger-ui.path` | — | `/swagger-ui.html` | O | Swagger UI path |
| `springdoc.swagger-ui.operations-sorter` | — | `method` | O | UI ordering |
| `springdoc.swagger-ui.tags-sorter` | — | `alpha` | O | UI ordering |

## 2. Primary datasource (Neon)

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `spring.datasource.url` | `DB_URL` | — | **R** | JDBC URL, e.g. `jdbc:postgresql://host/db?sslmode=require` |
| `spring.datasource.username` | `DB_USERNAME` | — | **R** | Database user |
| `spring.datasource.password` | `DB_PASSWORD` | — | **R** | Database password |
| `spring.datasource.driver-class-name` | — | `org.postgresql.Driver` | O | Fixed |
| `spring.jpa.hibernate.ddl-auto` | — | `update` | O | Schema management (see ADR 0011) |
| `spring.jpa.open-in-view` | — | `false` | O | Avoids lazy loading in views |
| `spring.jpa.properties.hibernate.dialect` | — | `org.hibernate.dialect.PostgreSQLDialect` | O | Fixed |
| `spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation` | — | `true` | O | PostgreSQL LOB handling |
| `spring.jpa.properties.hibernate.connection.handling_mode` | — | `DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION` | O | Shorter connection holds |
| `spring.datasource.hikari.maximum-pool-size` | `DB_POOL_SIZE` | `10` | O | Max connections |
| `spring.datasource.hikari.minimum-idle` | — | `5` | O | Min idle |
| `spring.datasource.hikari.idle-timeout` | — | `30000` | O | Idle timeout (ms) |
| `spring.datasource.hikari.max-lifetime` | — | `60000` | O | Max connection lifetime (ms) |
| `spring.datasource.hikari.connection-timeout` | — | `20000` | O | Acquire timeout (ms) |
| `spring.datasource.hikari.validation-timeout` | — | `3000` | O | Validation timeout (ms) |
| `spring.datasource.hikari.keepalive-time` | — | `0` | O | Keepalive (disabled) |
| `spring.datasource.hikari.leak-detection-threshold` | — | `20000` | O | Leak warning (ms) |

## 3. Chat datasource (Supabase)

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `app.supabase.db.jdbc-url` | `SUPABASE_DB_URL` | — | **C** | Chat JDBC URL |
| `app.supabase.db.username` | `SUPABASE_DB_USERNAME` | — | **C** | Chat user |
| `app.supabase.db.password` | `SUPABASE_DB_PASSWORD` | — | **C** | Chat password |
| `app.supabase.db.driver-class-name` | — | `org.postgresql.Driver` | O | Fixed |
| `app.supabase.db.maximum-pool-size` | `SUPABASE_DB_POOL_SIZE` | `10` | O | Chat pool size |
| `app.supabase.db.minimum-idle` | — | `5` | O | Chat pool min idle |

## 4. JWT and security

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `app.jwt.secret` | `JWT_SECRET` | — | **R** | HS256 signing key, ≥32 characters |
| `app.login.failed-attempt-threshold` | `APP_LOGIN_FAILED_ATTEMPT_THRESHOLD` | `5` | O | Lockout threshold |
| `app.rate-limit.enabled` | `APP_RATE_LIMIT_ENABLED` | `true` | O | Enable the limiter |
| `app.rate-limit.max-requests` | `APP_RATE_LIMIT_MAX_REQUESTS` | `10` | O | Requests per window |
| `app.rate-limit.window-seconds` | `APP_RATE_LIMIT_WINDOW_SECONDS` | `60` | O | Window length |

## 5. OTP and password reset

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `app.otp.ttl-minutes` | `APP_OTP_TTL_MINUTES` | `5` | O | OTP lifetime |
| `app.otp.max-resend-attempts` | `APP_OTP_MAX_RESEND_ATTEMPTS` | `5` | O | Resends before the user must wait |
| `app.password-reset.ttl-minutes` | `PASSWORD_RESET_TTL_MINUTES` | `30` | O | Reset link lifetime |
| `app.password-reset.max-attempts` | `PASSWORD_RESET_MAX_ATTEMPTS` | `5` | O | Reset resends |
| `app.password-reset.ui-url` | `PASSWORD_RESET_UI_URL` | `${app.ui.base-url}/reset-password` | C | Reset page |
| `app.login.ui-url` | `LOGIN_UI_URL` | `${app.ui.base-url}/login` | C | Login page (used in emails) |

## 6. Emailed deep links and circles

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `app.ui.base-url` | `UI_BASE_URL` | `https://chandu-goldengreetings.web.app` | C | Base for every emailed link |
| `app.circle.request.login-url` | `CIRCLE_REQUEST_LOGIN_URL` | `${app.ui.base-url}/direct-login` | C | One-click link target |
| `app.circle.request.login-ttl-minutes` | `CIRCLE_REQUEST_LOGIN_TTL_MINUTES` | `60` | O | Direct-login token lifetime |
| `app.circle.request.reminder-after-days` | `CIRCLE_REQUEST_REMINDER_AFTER_DAYS` | `7` | O | Days before reminding |
| `app.circle.request.reminder-cron` | `CIRCLE_REQUEST_REMINDER_CRON` | `0 0 9 * * ?` | O | Reminder schedule |

## 7. Chat behaviour

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `app.chat.circles-only` | `APP_CHAT_CIRCLES_ONLY` | `true` | O | Restrict messaging to accepted circles |
| `app.chat.unread-mail.cron` | `APP_CHAT_UNREAD_MAIL_CRON` | `0 0 2 * * ?` | O | Unread digest schedule |

## 8. Media and uploads

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `spring.servlet.multipart.max-file-size` | — | `50MB` | O | Servlet upload cap |
| `spring.servlet.multipart.max-request-size` | — | `50MB` | O | Request cap |
| `app.media.gzip.enabled` | `APP_MEDIA_GZIP_ENABLED` | `true` | O | Gzip stored media |
| `app.default-profile-picture.male-path` | `DEFAULT_PROFILE_PICTURE_MALE_PATH` | `Profile Pictures/default/male.png` | O | Default male picture |
| `app.default-profile-picture.female-path` | `DEFAULT_PROFILE_PICTURE_FEMALE_PATH` | `Profile Pictures/default/female.png` | O | Default female picture |
| `app.default-profile-picture.other-path` | `DEFAULT_PROFILE_PICTURE_OTHER_PATH` | `Profile Pictures/default/male.png` | O | Default other picture |
| `app.email.signature.path` | `EMAIL_SIGNATURE_PATH` | `signature.png` | O | Signature object |
| `server.compression.enabled` | — | `true` | O | Gzip HTTP responses |
| `server.compression.min-response-size` | — | `1024` | O | Minimum size to compress |
| `server.compression.mime-types` | — | JSON, XML, HTML, text, CSS, JS | O | Compressed types |

## 9. Supabase Storage

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `app.supabase.storage.endpoint` | `SUPABASE_STORAGE_ENDPOINT` | `https://mzidaubueqnwdfzmktoq.storage.supabase.co/storage/v1/s3` | **C** | S3 endpoint |
| `app.supabase.storage.region` | `SUPABASE_STORAGE_REGION` | `ap-southeast-1` | O | Region |
| `app.supabase.storage.bucket` | `SUPABASE_STORAGE_BUCKET` | `golden-greetings` | O | Bucket |
| `app.supabase.storage.access-key-id` | `SUPABASE_STORAGE_ACCESS_KEY_ID` | — | **C** | Access key |
| `app.supabase.storage.secret-access-key` | `SUPABASE_STORAGE_SECRET_ACCESS_KEY` | — | **C** | Secret key |
| `app.supabase.storage.public-base-url` | `SUPABASE_STORAGE_PUBLIC_BASE_URL` | (empty) | O | Public URL base for objects |

## 10. Email transports

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `spring.mail.host` | `MAIL_HOST` | `smtp.gmail.com` | O | SMTP host |
| `spring.mail.port` | `MAIL_PORT` | `587` | O | SMTP port |
| `spring.mail.username` | `MAIL_USERNAME` | — | **C** | SMTP user; also default alert recipient |
| `spring.mail.password` | `MAIL_PASSWORD` | — | **C** | SMTP app password |
| `spring.mail.properties.mail.smtp.auth` | — | `true` | O | SMTP auth |
| `spring.mail.properties.mail.smtp.starttls.enable` | — | `true` | O | STARTTLS |
| `gmail.client-id` | `GMAIL_CLIENT_ID` | — | **C** | Gmail OAuth client |
| `gmail.client-secret` | `GMAIL_CLIENT_SECRET` | — | **C** | Gmail OAuth secret |
| `gmail.refresh-token` | `GMAIL_REFRESH_TOKEN` | — | **C** | Gmail OAuth refresh token |
| `gmail.from-email` | `GMAIL_FROM_EMAIL` | — | **C** | Sender address |
| `gmail.redirect-uri` | `GMAIL_REDIRECT_URI` | `https://eventmanagement-backend-ka9x.onrender.com/oauth2callback` | O | OAuth callback |
| `app.google.sso.client-id` | `GOOGLE_SSO_CLIENT_ID` | `${GMAIL_CLIENT_ID}` | C | Google SSO audience |
| `brevo.api.key` | `BREVO_API_KEY` | — | C | Brevo API key |
| `brevo.api.url` | `BREVO_API_URL` | — | C | Brevo endpoint |
| `brevo.sender.email` | `BREVO_SENDER_EMAIL` | `${MAIL_USERNAME}` | C | Brevo sender |
| `brevo.sender.name` | `BREVO_SENDER_NAME` | — | C | Brevo sender name |

## 11. AI providers

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `app.gemini.api.key` | `GEMINI_API_KEY` | — | **C** | Gemini key |
| `app.gemini.api.url` | `GEMINI_API_URL` | — | **C** | Base URL including the primary model |
| `app.gemini.fallback.max-retries` | `GEMINI_FALLBACK_MAX_RETRIES` | `5` | O | Extra models tried (0 disables fallback) |
| `app.gemini.fallback.cooldown-seconds` | `GEMINI_FALLBACK_COOLDOWN_SECONDS` | `60` | O | Skip a failed model for this long |
| `app.gemini.fallback.model-cache-minutes` | `GEMINI_FALLBACK_MODEL_CACHE_MINUTES` | `60` | O | Model catalogue cache |
| `app.gemini.fallback.excluded-model-pattern` | `GEMINI_FALLBACK_EXCLUDED_MODEL_PATTERN` | `embed|image|tts|audio|live|vision|veo|lyria|robotics|computer-use|aqa|preview|exp` | O | Regex of model names to skip |
| `app.pollinations.image.url` | `POLLINATIONS_IMAGE_URL` | `https://gen.pollinations.ai/image/` | C | Image endpoint |
| `app.pollinations.image.key` | `POLLINATIONS_IMAGE_KEY` | (empty) | O | Bearer key when required |
| `app.pollinations.image.model` | `POLLINATIONS_IMAGE_MODEL` | `imagen-4` | O | Image model |
| `app.pollinations.balance.url` | `POLLINATIONS_BALANCE_URL` | `https://gen.pollinations.ai/account/balance` | O | Balance endpoint |

## 12. AI agents

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `app.agent.sender-name` | `AGENT_SENDER_NAME` | `Chandra` | O | Sender named in the agent prompt |
| `app.agent.wish.enabled` | `AGENT_WISH_ENABLED` | `true` | O | Agentic wish composer |
| `app.agent.wish.max-steps` | `AGENT_WISH_MAX_STEPS` | `6` | O | Tool-calling step cap |
| `app.agent.delivery-ops.enabled` | `AGENT_DELIVERY_OPS_ENABLED` | `true` | O | Delivery-ops agent |
| `app.agent.delivery-ops.dry-run` | `AGENT_DELIVERY_OPS_DRY_RUN` | `false` | O | Describe actions instead of performing them |
| `app.agent.delivery-ops.max-retries-per-run` | `AGENT_DELIVERY_OPS_MAX_RETRIES` | `10` | O | Retry budget per run |
| `app.agent.copilot.enabled` | `AGENT_COPILOT_ENABLED` | `true` | O | Read-only admin copilot |

> The shipped `application.properties` enables
> them. Set the three flags to `false` to match the README description.

## 13. Festivals

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `calendarific.api.url` | `CALENDARIFIC_API_URL` | `https://calendarific.com/api/v2/holidays` | C | Provider endpoint |
| `calendarific.api.key` | `CALENDARIFIC_API_KEY` | (empty) | **C** | API key; without it sync is skipped |
| `calendarific.country` | `CALENDARIFIC_COUNTRY` | `IN` | O | Country filter |

## 14. Instagram

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `instagram.graph-url` | `INSTAGRAM_GRAPH_URL` | `https://graph.facebook.com/v18.0` | C | Graph base |
| `instagram.page-access-token` | `INSTAGRAM_PAGE_ACCESS_TOKEN` | `YOUR_PAGE_ACCESS_TOKEN` | C | Page token |
| `instagram.verify.token` | `INSTA_VERIFY_TOKEN` | — | C | Webhook verify token |

## 15. Scheduling and async

| Property | Env var | Default | Req | Meaning |
| --- | --- | --- | --- | --- |
| `spring.quartz.job-store-type` | — | `jdbc` | O | Durable job store |
| `spring.quartz.jdbc.initialize-schema` | — | `always` | O | Create `QRTZ_*` on start |
| `spring.quartz.properties.org.quartz.scheduler.instanceName` | — | `GoldenGreetingsScheduler` | O | Scheduler name |
| `spring.quartz.properties.org.quartz.scheduler.instanceId` | — | `AUTO` | O | Clustered instance id |
| `spring.quartz.properties.org.quartz.jobStore.class` | — | `LocalDataSourceJobStore` | O | Job store |
| `spring.quartz.properties.org.quartz.jobStore.driverDelegateClass` | — | `PostgreSQLDelegate` | O | SQL dialect |
| `spring.quartz.properties.org.quartz.jobStore.useProperties` | — | `false` | O | Store job data as serialised |
| `spring.quartz.properties.org.quartz.jobStore.tablePrefix` | — | `QRTZ_` | O | Table prefix |
| `spring.quartz.properties.org.quartz.jobStore.isClustered` | — | `true` | O | Clustering |
| `spring.quartz.properties.org.quartz.threadPool.threadCount` | — | `5` | O | Worker threads |
| `spring.task.execution.pool.core-size` | — | `5` | O | `@Async` core pool |
| `spring.task.execution.pool.max-size` | — | `10` | O | `@Async` max pool |
| `spring.task.execution.pool.queue-capacity` | — | `100` | O | `@Async` queue |

## 16. Cache

| Property | Value | Meaning |
| --- | --- | --- |
| `spring.cache.type` | `jcache` | JCache abstraction |
| `spring.cache.jcache.config` | `classpath:ehcache.xml` | Cache definitions |
| `spring.jpa.properties.hibernate.cache.use_second_level_cache` | `true` | Entity cache |
| `spring.jpa.properties.hibernate.cache.use_query_cache` | `true` | Query cache |
| `spring.jpa.properties.hibernate.cache.region.factory_class` | `JCacheRegionFactory` | Bridge |
| `spring.jpa.properties.javax.cache.provider` | `org.ehcache.jsr107.EhcacheCachingProvider` | Provider |
| `spring.jpa.properties.javax.cache.uri` | `classpath:ehcache.xml` | Hibernate's cache config |

## 17. Logging

| Property | Value |
| --- | --- |
| `logging.level.root` | `ERROR` |
| `logging.level.org.springframework` | `WARN` |
| `logging.level.org.hibernate` | `ERROR` |
| `logging.level.com.familywishes` | `INFO` |

## 18. Profile differences

`application-prod.properties` sets `server.port=8080` and repeats the datasource, mail, JWT, AI,
storage, Quartz, compression and circle settings. It does **not** repeat the agent flags, the Gemini
fallback settings or the rate-limit settings, so those come from `application.properties` defaults in
production too.

| Property | Base | `prod` |
| --- | --- | --- |
| `server.port` | not set (8080) | `8080` |
| Agent flags | present | inherited |
| Gemini fallback | present | inherited |
| Rate limit | present | inherited |
| `app.default-profile-picture.*` | present | absent (defaults apply) |
| `app.pollinations.image.model` | present | absent (default `imagen-4`) |

## 19. Environment variable quick list

**Required:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.
**Required for email:** `MAIL_USERNAME`, `MAIL_PASSWORD`, `GMAIL_CLIENT_ID`,
`GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN`, `GMAIL_FROM_EMAIL`.
**Required for AI:** `GEMINI_API_KEY`, `GEMINI_API_URL`.
**Required for media:** `SUPABASE_STORAGE_ACCESS_KEY_ID`, `SUPABASE_STORAGE_SECRET_ACCESS_KEY`.
**Required for chat:** `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD`.
**Feature toggles:** `APP_CHAT_CIRCLES_ONLY`, `APP_MEDIA_GZIP_ENABLED`, `APP_RATE_LIMIT_ENABLED`,
`AGENT_WISH_ENABLED`, `AGENT_DELIVERY_OPS_ENABLED`, `AGENT_DELIVERY_OPS_DRY_RUN`,
`AGENT_COPILOT_ENABLED`.
**Optional tuning:** `DB_POOL_SIZE`, `SUPABASE_DB_POOL_SIZE`, `UI_BASE_URL`, `LOGIN_UI_URL`,
`PASSWORD_RESET_UI_URL`, `CIRCLE_REQUEST_LOGIN_URL`, `CIRCLE_REQUEST_LOGIN_TTL_MINUTES`,
`CIRCLE_REQUEST_REMINDER_AFTER_DAYS`, `CIRCLE_REQUEST_REMINDER_CRON`, `APP_CHAT_UNREAD_MAIL_CRON`,
`APP_OTP_TTL_MINUTES`, `APP_OTP_MAX_RESEND_ATTEMPTS`, `PASSWORD_RESET_TTL_MINUTES`,
`PASSWORD_RESET_MAX_ATTEMPTS`, `APP_LOGIN_FAILED_ATTEMPT_THRESHOLD`,
`APP_RATE_LIMIT_MAX_REQUESTS`, `APP_RATE_LIMIT_WINDOW_SECONDS`, `GEMINI_FALLBACK_*`,
`POLLINATIONS_*`, `CALENDARIFIC_*`, `SUPABASE_STORAGE_REGION`, `SUPABASE_STORAGE_BUCKET`,
`SUPABASE_STORAGE_PUBLIC_BASE_URL`, `DEFAULT_PROFILE_PICTURE_*`, `EMAIL_SIGNATURE_PATH`,
`SCHEDULER_TIME_ZONE`, `ALERT_EMAIL_TO`, `ALERT_FAILURE_THRESHOLD`, `INSTA_VERIFY_TOKEN`,
`INSTAGRAM_PAGE_ACCESS_TOKEN`, `INSTAGRAM_GRAPH_URL`, `BREVO_*`.

## 20. Changing configuration safely

1. Change the environment variable in the hosting platform, not the repository, for anything secret.
2. Restart the service; most properties are read at startup.
3. Verify with `GET /health`, a smoke sign-in, and one affected feature.
4. Record non-obvious changes in [../project/changelog.md](../project/changelog.md) and update this
   table.
