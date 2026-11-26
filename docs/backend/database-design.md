# Database design

Two PostgreSQL databases:

1. **Primary** (Neon) — business data plus the Quartz tables, managed by Hibernate
   (`ddl-auto=update`).
2. **Chat** (Supabase) — `chat_conversations`, `chat_messages`, `chat_message_reactions`, created by
   `ChatSchemaInitializer`.

## 1. Conventions

| Convention | Detail |
| --- | --- |
| Identifier | `BIGSERIAL` / `GenerationType.IDENTITY` |
| Booleans | `SMALLINT` (0/1) via `BooleanToZeroOneConverter`, not PostgreSQL `boolean` |
| Enums | `SMALLINT` with `EnumType.ORDINAL` (except chat, which uses `TEXT`) |
| Audit columns | `created_at`, `updated_at` (`TIMESTAMP`, set in IST by `AuditableEntity`) |
| Soft delete | `users.deleted` (0/1); `users.active` for enable/disable |
| Naming | snake_case tables and columns; `uq_*` unique constraints, `idx_*` indexes |
| FKs | `<entity>_id` with `@JoinColumn`; eager for small references, lazy for optional links |
| Text | `TEXT` for bodies and messages, `VARCHAR` with explicit lengths for names and URLs |
| URLs | `length = 500` for wishes links, `1000` for object keys, `TEXT` for picture URLs |
| Time zone | All timestamps written in Asia/Kolkata (no `timestamptz`) |

## 2. Primary database — tables

### 2.1 `users`

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `id` | BIGSERIAL | PK | |
| `name` | VARCHAR(255) | NOT NULL | |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Login identifier |
| `password` | VARCHAR(255) | NOT NULL | BCrypt hash |
| `gender` | SMALLINT | NOT NULL | `Gender` ordinal |
| `role` | SMALLINT | NOT NULL | `Role` ordinal |
| `deleted` | SMALLINT | NOT NULL | Soft delete |
| `failed_login_attempts` | INTEGER | NOT NULL | Lockout counter |
| `mfa_enabled` | SMALLINT | NOT NULL | Second factor |
| `concurrent_login_allowed` | SMALLINT | NOT NULL, DEFAULT 1 | Single-session switch |
| `active_session_id` | VARCHAR(64) | NULL | Current `sid` |
| `relationship_seed_id` | BIGINT | NOT NULL, FK → `relationship_seeds.id` | |
| `birthday` | DATE | NULL | |
| `last_birthday_wish_sent` | DATE | NULL | Duplicate guard |
| `birthday_wishes_url` | VARCHAR(500) | NULL | |
| `online` | SMALLINT | NULL | Presence flag |
| `last_seen_at` | TIMESTAMP | NULL | Presence timestamp |
| `profile_picture_url` | TEXT | NULL | Public URL or object key |
| `active` | SMALLINT | NOT NULL | From `ActivatableEntity` |
| `created_at`, `updated_at` | TIMESTAMP | NOT NULL | |

### 2.2 Reference tables

| Table | Columns | Notes |
| --- | --- | --- |
| `event_type_seeds` | `id`, `code` UNIQUE, `display_name`, `active`, audit | `ANNIVERSARY`, `ENGAGEMENT`, `FESTIVAL`, … |
| `relationship_seeds` | `id`, `code` UNIQUE, `display_name`, `active`, audit | `WIFE`, `HUSBAND`, … |
| `template_type_seeds` | `id`, `code` UNIQUE, `display_name`, `active`, audit | `GOOD_MORNING`, `FESTIVAL` |
| `theme_seeds` | `id`, `code` UNIQUE, `display_name`, `active`, audit | 11 themes; second-level cached |

### 2.3 `events`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `event_type_seed_id` | BIGINT | NOT NULL, FK → `event_type_seeds.id` |
| `event_date` | DATE | NULL |
| `wishes_url` | VARCHAR(500) | NULL |
| `image_object_key` | VARCHAR(1000) | NULL |
| `recurring` | SMALLINT | NOT NULL |
| `user_id` | BIGINT | NOT NULL, FK → `users.id` |
| `active` | SMALLINT | NOT NULL |
| audit | TIMESTAMP | NOT NULL |

### 2.4 `seed_special_events`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `event_name` | VARCHAR(255) | Festival name |
| `event_date` | DATE | Date in a specific year |
| `message` | VARCHAR(1000) | Default wish text |
| `active` | SMALLINT | Inactive when removed from a sync |
| audit | TIMESTAMP | |

### 2.5 `festival_wish_mappings`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `special_event_id` | BIGINT | NOT NULL, FK |
| `user_id` | BIGINT | NOT NULL, FK |
| `custom_message` | VARCHAR(1000) | NULL |
| `image_object_key` | VARCHAR(500) | NULL |
| `wishes_url` | VARCHAR(500) | NULL |
| `last_wish_sent_on` | DATE | NULL |
| `is_error` | SMALLINT | NOT NULL |
| `active` | SMALLINT | NOT NULL |
| audit | TIMESTAMP | |

Constraint `uk_festival_user_mapping (special_event_id, user_id)`.

### 2.6 `wish_images`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | NULL, FK (null = for everyone) |
| `event_type` | VARCHAR(150) | NOT NULL |
| `object_key` | VARCHAR(1000) | NOT NULL, UNIQUE |
| `active` | SMALLINT | NOT NULL |
| audit | TIMESTAMP | |

Indexes: `idx_wish_images_event_active (event_type, active)`, `idx_wish_images_user_event (user_id,
event_type)`.

### 2.7 `email_logs`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `recipient_user_id` | BIGINT | NULL, FK → `users.id` |
| `subject` | VARCHAR(255) | NOT NULL |
| `status` | SMALLINT | `EmailStatus` ordinal |
| `email_type` | SMALLINT | `EmailType` ordinal, defaulted to `EVENT` when null |
| `retry_count` | INTEGER | NOT NULL |
| `body` | TEXT | HTML actually sent |
| `error_message` | TEXT | Provider error |
| `sent_at` | TIMESTAMP | NULL |
| `image_url` | TEXT | Object key of the image |
| audit | TIMESTAMP | |

### 2.8 `circles`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `requester_id` | BIGINT | NOT NULL, FK → `users.id` |
| `addressee_id` | BIGINT | NOT NULL, FK → `users.id` |
| `status` | SMALLINT | NOT NULL (`PENDING`/`ACCEPTED`/`REJECTED`) |
| `responded_at` | TIMESTAMP | NULL |
| `last_reminder_sent_on` | DATE | NULL |
| audit | TIMESTAMP | |

`uq_circle_pair (requester_id, addressee_id)`, indexes on both columns.

### 2.9 `notifications`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `title` | VARCHAR(150) | NOT NULL |
| `message` | TEXT | NOT NULL |
| `published` | BOOLEAN | NOT NULL, default false |
| `can_send_email` | BOOLEAN | NOT NULL, default false |
| `scheduled_from`, `scheduled_to` | TIMESTAMP | NULL |
| `published_at` | TIMESTAMP | NULL |
| `created_by`, `updated_by` | VARCHAR(255) | NOT NULL |
| audit | TIMESTAMP | |

### 2.10 Token and security tables

| Table | Columns | Constraints |
| --- | --- | --- |
| `otp_codes` | `id`, `user_id` FK, `code`, `expires_at`, `used` SMALLINT, `resend_count`, audit | — |
| `password_reset_tokens` | `id`, `token` UNIQUE, `user_id` FK, `expires_at`, `used` SMALLINT, `resend_count`, audit | — |
| `refresh_tokens` | `id`, `token` UNIQUE TEXT, `user_id` FK, `expires_at`, `revoked` SMALLINT, audit | Pruned daily |
| `login_tokens` | `id`, `token` UNIQUE, `user_id` FK, `expires_at`, `used` SMALLINT, audit | Single use |
| `login_location_events` | `id`, `user_id` FK, `login_location` VARCHAR(255), `logged_in_at`, `ip_address` VARCHAR(64), `latitude`, `longitude`, audit | Grows unbounded |
| `violated_users` | `id`, `email`, `password`, `login_location` VARCHAR(255), `logged_in_at`, `ip_address` VARCHAR(64), `latitude`, `longitude` | No audit columns; grows unbounded |

> `violated_users.password` stores the attempted password in clear text. Treat the table as highly
> sensitive (see [../security/threat-model.md](../security/threat-model.md) T-07).

### 2.11 Preference tables

| Table | Columns | Constraints |
| --- | --- | --- |
| `user_wish_settings` | `id`, `good_morning_enabled`, `good_night_enabled`, `birthday_enabled` (SMALLINT), `user_id` FK, audit | One row per user |
| `user_theme_preferences` | `id`, `theme` VARCHAR(50) default `default`, `user_id` FK UNIQUE, audit | One row per user |

### 2.12 Instagram bridge tables

| Table | Columns |
| --- | --- |
| `instagram_user` | `id`, `instagram_user_id`, `name`, `birthday`, `last_user_message_time`, audit |
| `message_log` | `id`, `instagram_user_id`, `message` VARCHAR(1000), `status` SMALLINT, `message_type` SMALLINT, `retry_count`, `last_attempt_time`, `error_message`, audit |

### 2.13 Quartz tables (`QRTZ_*`)

Created automatically because `spring.quartz.jdbc.initialize-schema=always`. Notable ones:
`QRTZ_JOB_DETAILS`, `QRTZ_TRIGGERS`, `QRTZ_CRON_TRIGGERS`, `QRTZ_SIMPLE_TRIGGERS`, `QRTZ_FIRED_TRIGGERS`,
`QRTZ_PAUSED_TRIGGER_GRPS`, `QRTZ_SCHEDULER_STATE`, `QRTZ_LOCKS`. The job store uses
`PostgreSQLDelegate`, `isClustered=true`, `tablePrefix=QRTZ_`, `useProperties=false`.

## 3. Chat database — tables

### 3.1 `chat_conversations`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `user_a_id` | BIGINT | NOT NULL |
| `user_b_id` | BIGINT | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL |

`uq_chat_conversation (user_a_id, user_b_id)`. Created with an upsert
(`ON CONFLICT … DO UPDATE`) so a repeated send reuses the row.

### 3.2 `chat_messages`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `conversation_id` | BIGINT | NOT NULL |
| `sender_id` | BIGINT | NOT NULL |
| `receiver_id` | BIGINT | NOT NULL |
| `message_text` | TEXT | NULL |
| `attachment_key` | TEXT | NULL |
| `attachment_file_name` | TEXT | NULL |
| `attachment_content_type` | TEXT | NULL |
| `sent_at` | TIMESTAMP | NOT NULL |
| `seen_at` | TIMESTAMP | NULL |
| `reply_to_message_id` | BIGINT | NULL (added by `ALTER`) |
| `encrypted_message` | TEXT | NULL |
| `encryption_algorithm` | TEXT | NULL |
| `encryption_key_id` | TEXT | NULL |
| `message_type` | TEXT | NOT NULL DEFAULT `'TEXT'` |
| `voice_duration_seconds` | INTEGER | NULL |

Indexes: `idx_chat_messages_conv_sent (conversation_id, sent_at DESC)`,
`idx_chat_messages_receiver_seen (receiver_id, seen_at)`.

### 3.3 `chat_message_reactions`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `message_id` | BIGINT | NOT NULL |
| `user_id` | BIGINT | NOT NULL |
| `emoji` | TEXT | NOT NULL |
| `reacted_at` | TIMESTAMP | NOT NULL |

`uq_chat_message_reaction (message_id, user_id, emoji)`, index `idx_chat_reactions_message
(message_id)`.

## 4. Relationships

| Child | Parent | Cardinality | On delete |
| --- | --- | --- | --- |
| `users.relationship_seed_id` | `relationship_seeds.id` | many-to-one | Restricted by FK |
| `events.user_id` | `users.id` | many-to-one | Application deletes the event |
| `events.event_type_seed_id` | `event_type_seeds.id` | many-to-one | Restricted |
| `festival_wish_mappings.special_event_id` | `seed_special_events.id` | many-to-one | Restricted |
| `festival_wish_mappings.user_id` | `users.id` | many-to-one | Restricted |
| `wish_images.user_id` | `users.id` | many-to-one (nullable) | Restricted |
| `email_logs.recipient_user_id` | `users.id` | many-to-one (nullable) | Restricted |
| `circles.requester_id`, `circles.addressee_id` | `users.id` | many-to-one | Restricted |
| `otp_codes`, `password_reset_tokens`, `refresh_tokens`, `login_tokens`, `login_location_events` | `users.id` | many-to-one | Restricted |
| `user_wish_settings.user_id`, `user_theme_preferences.user_id` | `users.id` | one-to-one | Cascade from `User` for wish settings |
| `chat_messages.conversation_id` | `chat_conversations.id` | many-to-one | No FK (plain SQL) |

## 5. Indexes

| Index | Table | Purpose |
| --- | --- | --- |
| `users.email` unique | `users` | Login lookup |
| `event_type_seeds.code` unique | `event_type_seeds` | Type resolution by code |
| `relationship_seeds.code` unique | `relationship_seeds` | Relationship resolution |
| `template_type_seeds.code` unique | `template_type_seeds` | Template resolution |
| `theme_seeds.code` unique | `theme_seeds` | Theme validation |
| `wish_images.object_key` unique | `wish_images` | Object identity |
| `idx_wish_images_event_active` | `wish_images` | Image lookup order |
| `idx_wish_images_user_event` | `wish_images` | Per-user lookup |
| `uq_circle_pair` | `circles` | One row per direction pair |
| `idx_circles_requester`, `idx_circles_addressee` | `circles` | Circle list queries |
| `uk_festival_user_mapping` | `festival_wish_mappings` | Upsert key |
| `password_reset_tokens.token` unique | | Token lookup |
| `refresh_tokens.token` unique | | Refresh lookup |
| `login_tokens.token` unique | | Direct login |
| `idx_chat_messages_conv_sent` | chat | Conversation paging |
| `idx_chat_messages_receiver_seen` | chat | Unread counts |
| `idx_chat_reactions_message` | chat | Reaction listing |

## 6. Integrity rules enforced by the application

| Rule | Where |
| --- | --- |
| Unique email per user | Database unique constraint; no pre-check (a duplicate yields 500 today) |
| One circle row per pair | `uq_circle_pair` plus `findBetween` logic |
| One festival mapping per (festival, user) | `uk_festival_user_mapping` plus upsert logic |
| One wish image object key | Unique constraint |
| Tokens consumed once | `used` flag set in the same transaction |
| Only one published notification | Service unpublishes the previous row |
| Birthday wish once per day | `users.last_birthday_wish_sent` |
| Festival wish once per day unless failed | `last_wish_sent_on` + `is_error` |

## 7. Data volumes and growth

| Table | Expected rows | Growth driver |
| --- | --- | --- |
| `users` | tens | New family members |
| `events` | hundreds | Anniversaries per user |
| `seed_special_events` | hundreds (per year) | Calendarific sync and browsed years |
| `festival_wish_mappings` | hundreds | Festival × user |
| `email_logs` | thousands per year | Daily wishes, OTP, resets, chat digests |
| `login_location_events` | thousands per year | Every sign-in |
| `violated_users` | unbounded | Failed attempts (including bots) |
| `refresh_tokens` | tens | Pruned daily |
| `chat_messages` | tens of thousands | Conversation volume |

## 8. Retention and cleanup

| Data | Cleanup |
| --- | --- |
| Expired refresh tokens | `RefreshTokenCleanupScheduler` daily at 00:00 IST |
| Failed emails | Retried up to 3 times; rows kept |
| OTP / reset / login tokens | Not pruned; expired rows remain |
| `violated_users`, `login_location_events` | Not pruned |
| Email images | Object kept while the row exists; deleting the row through the admin API leaves the object unless storage delete succeeds |
| Users | Soft delete only |

## 9. Schema change procedure

1. Change the entity (or `ChatSchemaInitializer` for chat).
2. Start against a copy of production data and inspect the generated DDL.
3. Back up before deploying (`pg_dump`).
4. Deploy; Hibernate applies additive changes at startup.
5. For destructive changes (drop, rename, type change), write and review explicit SQL — Hibernate
   will not do it.

See [../deployment/database-migration.md](../deployment/database-migration.md).
