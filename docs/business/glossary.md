# Glossary

## Business terms

| Term | Meaning in this product |
| --- | --- |
| **Circle** | A mutually accepted trust relationship between two users. It unlocks calendar sharing and (by default) chat. Stored one row per pair in `circles`, with a direction until accepted. |
| **Circle request** | An invitation from one user to another. States: `PENDING`, `ACCEPTED`, `REJECTED`. |
| **Addressee** | The user a circle request is sent to; the only user allowed to accept or reject it. |
| **Requester** | The user who sent the circle request. |
| **Wish** | A generated greeting: a subject and an HTML body, optionally with an image and a link, sent by email. |
| **Wish image** | An administrator-uploaded greeting image, optionally scoped to one user, used instead of a generated image. |
| **Event** | A dated occasion for one user (anniversary, engagement, other seeded type) that produces a yearly or one-time wish. |
| **Festival** | A public holiday from Calendarific, stored in `seed_special_events` with a date per year. |
| **Festival wish mapping** | A link between one festival and one user that causes a festival wish to be sent. |
| **Special event seed** | The festival template row (`seed_special_events`): name, date, message, active flag. |
| **Seed data** | Small reference tables managed by administrators: event types, relationships, template types, themes, special events. |
| **Wishes URL** | An optional `http`/`https` link to a personal wishes page, appended to a birthday, event or festival email. |
| **Personal note** | An optional text on a festival mapping, appended to that festival's email. |
| **Signature** | The shared PNG appended to every email (inline when it can be loaded from storage). |
| **Delivery ops** | The agent that triages failed emails, retries transient failures and alerts the administrator. |
| **Copilot** | The read-only natural-language assistant for administrators. |
| **Impersonation** | An admin signing in as a non-admin user; the token carries `impersonatedBy`. |
| **Single-session sign-in** | A per-user setting that makes a new sign-in invalidate earlier sessions. |
| **Direct login** | Signing in from an emailed one-click link using a single-use `login_tokens` row. |
| **Violated user** | A record of a failed sign-in attempt, used for security analytics. |
| **Login location** | The place reported (or resolved) for a successful sign-in. |
| **Dashboard summary** | The four counters: total users, upcoming events, emails sent today, failed emails. |

## Technical terms and abbreviations

| Term | Meaning |
| --- | --- |
| **JWT** | JSON Web Token. HS256-signed access token carrying `sub`, `scope`, `sid` and optionally `impersonatedBy`. |
| **Access token** | Short-lived bearer token (15 minutes) used on every API call. |
| **Refresh token** | Long-lived token (7 days) stored in `refresh_tokens`, used only at `/api/auth/refresh`, revocable. |
| **`sid` claim** | The user's current `active_session_id`, used to reject superseded sessions. |
| **OTP** | One-time password: 6 digits, 5-minute lifetime, resend-capped. |
| **MFA** | Multi-factor authentication flag on a user; forces OTP after a password or SSO sign-in. |
| **SSO** | Single sign-on; here, Google ID-token verification. |
| **BCrypt** | Password hashing algorithm used for every stored password. |
| **Rate limiting** | Sliding-window request cap per client IP and endpoint. |
| **STOMP** | Simple Text Oriented Messaging Protocol, used over WebSocket for chat. |
| **SockJS** | WebSocket fallback transport used by the chat endpoint. |
| **Quartz** | Scheduler with a JDBC job store; jobs survive restarts and run clustered. |
| **Misfire** | A scheduled trigger that was missed; handled per trigger (`FireAndProceed` or `DoNothing`). |
| **Ehcache / JCache** | In-process cache provider configured in `ehcache.xml`. |
| **Second-level cache** | Hibernate's shared entity cache (`seedData` region). |
| **Query cache** | Hibernate's cache of query results (`default-query-results-region`). |
| **HikariCP** | JDBC connection pool used for both datasources. |
| **Flyway / Liquibase** | Schema migration tools — **not used** here (`ddl-auto=update` instead). |
| **Gemini** | Google's generative model API used for text. |
| **Model fallback** | Trying the next available Gemini model when one is rate limited, missing or failing. |
| **Pollinations** | Image generation provider used for greeting cards. |
| **Calendarific** | Holiday data provider. |
| **ip-api.com** | IP geolocation provider (45 requests/minute on the free tier). |
| **Supabase** | Provider of the object-storage bucket (S3-compatible) and the chat database. |
| **S3 presigned URL** | Time-limited URL granting access to one object. |
| **Gzip at rest** | Storing media compressed with `Content-Encoding: gzip`. |
| **Byte sniffing** | Detecting a file's real type from its leading bytes. |
| **Multipart** | `multipart/form-data` request used for uploads. |
| **DTO** | Data transfer object; here Java `record` types in `com.familywishes.dto`. |
| **PagedResponse** | Shared list envelope: `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`. |
| **IST** | Indian Standard Time, `Asia/Kolkata`; the time zone for all scheduling and timestamps. |
| **Render** | Hosting platform for the production container. |
| **Neon** | Hosted PostgreSQL used as the primary database. |
| **UptimeRobot** | External uptime monitor polling `/health`. |
| **Spotless** | Formatting plugin (google-java-format 1.23.0). |
| **Toolchain** | Gradle's pinned Java version (17) used to compile regardless of the launching JDK. |
| **Agent** | A tool-calling loop over the Gemini endpoint: `WishComposerAgent`, `DeliveryOpsAgent`, `AdminCopilotAgent`. |
| **Tool** | A function an agent may call (for example `submit_wish`, `retry_email`, `get_email_counts`). |
| **Validator tool** | A terminal tool whose `accepted` result ends the agent loop (`submit_wish`). |
| **Dry run** | A mode where the delivery-ops agent describes actions without performing them. |

## Enum values

| Enum | Values |
| --- | --- |
| `Role` | `ROLE_ADMIN`, `ROLE_USER` |
| `Gender` | `MALE`, `FEMALE`, `OTHER` |
| `RelationShip` | `WIFE`, `HUSBAND`, `FRIEND`, `MOTHER`, `SISTER`, `BROTHER`, `SON`, `DAUGHTER`, `DAUGHTER_IN_LAW`, `ADMIN` |
| `EventType` (legacy enum) | `ANNIVERSARY`, `ENGAGEMENT`, `FESTIVAL` |
| `EmailStatus` | `PENDING`, `SENT`, `FAILED` |
| `EmailType` | `OTP`, `FORGOT_PASSWORD`, `GOOD_MORNING`, `GOOD_NIGHT`, `BIRTHDAY`, `EVENT`, `FESTIVAL_WISH`, `UNREAD_CHAT_MESSAGE`, `CIRCLE_REQUEST` |
| `CircleStatus` | `PENDING`, `ACCEPTED`, `REJECTED` |
| `MessageStatus` | `PENDING`, `SENT`, `FAILED`, `RETRYING` |
| `MessageType` | `BIRTHDAY`, `FESTIVAL` |
| `SeedTemplateType` | `GOOD_MORNING`, `FESTIVAL` |
| `UITheme` | `skeuomorphism`, `neomorphism`, `glassmorphism`, `claymorphism`, `minimalism`, `maximalism`, `brutalism`, `liquid-glass`, `bento-grid`, `spatial-ui`, `default` |

## UI theme display names

| Code | Display name |
| --- | --- |
| `skeuomorphism` | Realistic Design |
| `neomorphism` | Soft UI |
| `glassmorphism` | Frosted Glass |
| `claymorphism` | Soft 3D Design |
| `minimalism` | Clean & Simple |
| `maximalism` | Bold & Vibrant |
| `brutalism` | Bold & Raw |
| `liquid-glass` | Liquid Glass |
| `bento-grid` | Modular Grid |
| `spatial-ui` | 3D Spatial Design |
| `default` | default |

## Related documents

* [project-overview.md](project-overview.md)
* [functional-requirements.md](functional-requirements.md)
* [../backend/data-dictionary.md](../backend/data-dictionary.md)
