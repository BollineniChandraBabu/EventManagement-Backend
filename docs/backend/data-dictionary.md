# Data dictionary

Field-level meaning for every persisted column. "Derived" means the value is computed by the
application rather than supplied by a client.

## 1. `users`

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | BIGINT | no | Surrogate key |
| `name` | VARCHAR | no | Display name used in greetings |
| `email` | VARCHAR | no | Unique login identifier and email recipient |
| `password` | VARCHAR | no | BCrypt hash; never the clear text |
| `gender` | SMALLINT | no | `Gender` ordinal: 0 MALE, 1 FEMALE, 2 OTHER |
| `role` | SMALLINT | no | `Role` ordinal: 0 ROLE_ADMIN, 1 ROLE_USER |
| `deleted` | SMALLINT | no | 1 = soft-deleted; excluded from lookups and directory |
| `failed_login_attempts` | INTEGER | no | Consecutive bad passwords; reset on success |
| `mfa_enabled` | SMALLINT | no | 1 = password/SSO sign-in must be followed by OTP |
| `concurrent_login_allowed` | SMALLINT | no | 1 = multiple sessions allowed (default) |
| `active_session_id` | VARCHAR(64) | yes | Current session UUID, mirrored in the `sid` claim |
| `relationship_seed_id` | BIGINT | no | FK to `relationship_seeds` |
| `birthday` | DATE | yes | Used for birthday wishes and the calendar |
| `last_birthday_wish_sent` | DATE | yes | Date of the last birthday wish; prevents duplicates |
| `birthday_wishes_url` | VARCHAR(500) | yes | Optional personal wishes page |
| `online` | SMALLINT | yes | Presence flag from heartbeat/offline calls |
| `last_seen_at` | TIMESTAMP | yes | Last heartbeat; older than 2 minutes = offline |
| `profile_picture_url` | TEXT | yes | Public URL or storage object key |
| `active` | SMALLINT | no | 1 = account enabled; 0 = disabled by admin |
| `created_at` | TIMESTAMP | no | IST creation time |
| `updated_at` | TIMESTAMP | no | IST last update |

## 2. Reference tables

### `event_type_seeds`
| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | BIGINT | no | Key |
| `code` | VARCHAR | no | Unique technical code (`ANNIVERSARY`, `ENGAGEMENT`, `FESTIVAL`, …); matched case-insensitively |
| `display_name` | VARCHAR | no | Human label used in emails and the calendar |
| `active` | SMALLINT | no | Inactive types cannot be used for new events |
| audit | TIMESTAMP | no | |

### `relationship_seeds`
| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `code` | VARCHAR | no | Unique code (`WIFE`, `HUSBAND`, `FRIEND`, `MOTHER`, `SISTER`, `BROTHER`, `SON`, `DAUGHTER`, `DAUGHTER_IN_LAW`, `ADMIN`) |
| `display_name` | VARCHAR | no | Label |
| `active` | SMALLINT | no | |

### `template_type_seeds`
| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `code` | VARCHAR | no | `GOOD_MORNING` or `FESTIVAL` |
| `display_name` | VARCHAR | no | Label |
| `active` | SMALLINT | no | |

### `theme_seeds`
| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `code` | VARCHAR | no | Unique technical theme name (`liquid-glass`, `bento-grid`, …) |
| `display_name` | VARCHAR | no | Friendly name shown in the UI |
| `active` | SMALLINT | no | Only active themes are offered |

## 3. `events`

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | BIGINT | no | Key |
| `event_type_seed_id` | BIGINT | no | Type of occasion |
| `event_date` | DATE | yes | Original date; for recurring events only month/day matter |
| `wishes_url` | VARCHAR(500) | yes | Optional custom wishes page added to the email |
| `image_object_key` | VARCHAR(1000) | yes | Storage key of an uploaded greeting image |
| `recurring` | SMALLINT | no | 1 = every year on month/day; 0 = only on `event_date` |
| `user_id` | BIGINT | no | Owner |
| `active` | SMALLINT | no | Inactive events are not wished |
| audit | TIMESTAMP | no | |

## 4. `seed_special_events` (festivals)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | BIGINT | no | Key |
| `event_name` | VARCHAR | yes | Festival name from Calendarific |
| `event_date` | DATE | yes | Date in a specific year |
| `message` | VARCHAR(1000) | yes | Default message; auto-filled as `Happy {name}! …` |
| `active` | SMALLINT | no | 0 when a sync no longer returns the festival for that year |
| audit | TIMESTAMP | no | |

## 5. `festival_wish_mappings`

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | BIGINT | no | Key |
| `special_event_id` | BIGINT | no | Festival |
| `user_id` | BIGINT | no | Recipient |
| `custom_message` | VARCHAR(1000) | yes | Personal note appended to the festival email |
| `image_object_key` | VARCHAR(500) | yes | Optional image for this mapping |
| `wishes_url` | VARCHAR(500) | yes | Optional wishes page |
| `last_wish_sent_on` | DATE | yes | Last attempt date (set on success and failure) |
| `is_error` | SMALLINT | no | 1 = last attempt failed; cleared on success |
| `active` | SMALLINT | no | Only active mappings are sent |
| audit | TIMESTAMP | no | |

## 6. `wish_images`

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | BIGINT | no | Key |
| `user_id` | BIGINT | yes | Null = available to everyone |
| `event_type` | VARCHAR(150) | no | Event type or festival name; matching is case-insensitive and `_`/space tolerant |
| `object_key` | VARCHAR(1000) | no | Unique storage key |
| `active` | SMALLINT | no | Inactive images are ignored |
| audit | TIMESTAMP | no | |

## 7. `email_logs`

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | BIGINT | no | Key |
| `recipient_user_id` | BIGINT | yes | Matched user, null when the address is not a registered user |
| `subject` | VARCHAR | no | Email subject |
| `status` | SMALLINT | no | `EmailStatus` ordinal: 0 PENDING, 1 SENT, 2 FAILED |
| `email_type` | SMALLINT | yes | `EmailType` ordinal; defaulted to `EVENT` when unset |
| `retry_count` | INTEGER | no | Incremented on each failed attempt |
| `body` | TEXT | yes | HTML body actually sent (without the outer template) |
| `error_message` | TEXT | yes | Provider error text |
| `sent_at` | TIMESTAMP | yes | Attempt time (set on success and failure) |
| `image_url` | TEXT | yes | Storage object key of the attached image |
| audit | TIMESTAMP | no | |

`EmailType` ordinals: 0 OTP, 1 FORGOT_PASSWORD, 2 GOOD_MORNING, 3 GOOD_NIGHT, 4 BIRTHDAY, 5 EVENT,
6 FESTIVAL_WISH, 7 UNREAD_CHAT_MESSAGE, 8 CIRCLE_REQUEST.

## 8. `circles`

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | BIGINT | no | Key |
| `requester_id` | BIGINT | no | User who sent the request |
| `addressee_id` | BIGINT | no | User who must respond |
| `status` | SMALLINT | no | 0 PENDING, 1 ACCEPTED, 2 REJECTED |
| `responded_at` | TIMESTAMP | yes | When the addressee accepted or rejected |
| `last_reminder_sent_on` | DATE | yes | Last reminder date; one per day |
| audit | TIMESTAMP | no | |

## 9. `notifications`

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| `id` | BIGINT | no | Key |
| `title` | VARCHAR(150) | no | Headline |
| `message` | TEXT | no | Body |
| `published` | BOOLEAN | no | Only one row is true at a time |
| `can_send_email` | BOOLEAN | no | Also email all active users |
| `scheduled_from` | TIMESTAMP | yes | Publish time; must be in the future at publish |
| `scheduled_to` | TIMESTAMP | yes | Automatic unpublish time |
| `published_at` | TIMESTAMP | yes | `scheduled_from` when set, else publish time |
| `created_by` | VARCHAR | no | Actor email |
| `updated_by` | VARCHAR | no | Last actor email |
| audit | TIMESTAMP | no | |

## 10. Token tables

| Table | Field | Meaning |
| --- | --- | --- |
| `otp_codes` | `code` | 6-digit numeric code |
| | `expires_at` | Creation + `app.otp.ttl-minutes` |
| | `used` | 1 once verified |
| | `resend_count` | Resends while still valid (cap 5) |
| `password_reset_tokens` | `token` | UUID in the reset link |
| | `expires_at` | Creation + `app.password-reset.ttl-minutes` (30) |
| | `used` | 1 once consumed |
| | `resend_count` | Resends while still valid (cap 5) |
| `refresh_tokens` | `token` | JWT refresh token string |
| | `expires_at` | Creation + 7 days |
| | `revoked` | 1 after single-session sign-in or explicit revocation |
| `login_tokens` | `token` | UUID in the one-click email link |
| | `expires_at` | Creation + `app.circle.request.login-ttl-minutes` (60) |
| | `used` | 1 once consumed (single use) |

## 11. Security event tables

### `login_location_events`
| Field | Meaning |
| --- | --- |
| `user_id` | Who signed in |
| `login_location` | Location text from the client, or `Unknown` |
| `logged_in_at` | IST sign-in time |
| `ip_address` | Client IP (`X-Forwarded-For`, `X-Real-IP` or remote address) |
| `latitude`, `longitude` | Optional coordinates from the client |

### `violated_users`
| Field | Meaning |
| --- | --- |
| `email` | Attempted email (may not exist) |
| `password` | Attempted password — **clear text**; treat as highly sensitive |
| `login_location` | Location text or `Unknown` |
| `logged_in_at` | IST attempt time |
| `ip_address` | Client IP |
| `latitude`, `longitude` | Optional coordinates |

## 12. Preference tables

| Table | Field | Meaning |
| --- | --- | --- |
| `user_wish_settings` | `good_morning_enabled` | Receives the 06:00 wish |
| | `good_night_enabled` | Receives the 22:00 wish |
| | `birthday_enabled` | Receives a birthday wish |
| | `user_id` | Owner |
| `user_theme_preferences` | `theme` | Selected theme code, default `default` |
| | `user_id` | Owner (unique) |

## 13. Instagram bridge

| Table | Field | Meaning |
| --- | --- | --- |
| `instagram_user` | `instagram_user_id` | Instagram account id |
| | `name` | Display name |
| | `birthday` | Birthday for wishes |
| | `last_user_message_time` | Last message time (send-window check) |
| `message_log` | `instagram_user_id` | Recipient |
| | `message` | Message text |
| | `status` | 0 PENDING, 1 SENT, 2 FAILED, 3 RETRYING |
| | `message_type` | 0 BIRTHDAY, 1 FESTIVAL |
| | `retry_count` | Attempts so far (max 3) |
| | `last_attempt_time` | Last attempt |
| | `error_message` | Provider error |

## 14. Chat tables

### `chat_conversations`
| Field | Meaning |
| --- | --- |
| `user_a_id`, `user_b_id` | The two participants; the pair is unique |
| `created_at` | Creation time |

### `chat_messages`
| Field | Meaning |
| --- | --- |
| `conversation_id` | Owning conversation |
| `sender_id`, `receiver_id` | Participants |
| `message_text` | Plain text (may be empty when encrypted or attachment-only) |
| `encrypted_message` | Client-side encrypted payload |
| `encryption_algorithm` | Algorithm identifier supplied by the client |
| `encryption_key_id` | Key identifier supplied by the client |
| `message_type` | `TEXT` (default), voice, or other client-defined value |
| `voice_duration_seconds` | Voice note length |
| `attachment_key` | Storage object key |
| `attachment_file_name` | Original file name |
| `attachment_content_type` | Detected media type |
| `reply_to_message_id` | Quoted message |
| `sent_at`, `seen_at` | Delivery and read timestamps (IST) |

### `chat_message_reactions`
| Field | Meaning |
| --- | --- |
| `message_id` | Reacted message |
| `user_id` | Who reacted |
| `emoji` | Reaction |
| `reacted_at` | When |

## 15. Derived and denormalised values

| Value | Source |
| --- | --- |
| `users.online` | Derived from heartbeat/offline calls; stale after 2 minutes |
| `users.last_birthday_wish_sent` | Set by `BirthdayMailScheduler` |
| `festival_wish_mappings.last_wish_sent_on`, `is_error` | Set by `FestivalScheduler` |
| `email_logs.status`, `retry_count`, `sent_at`, `error_message` | Set by the mail service |
| `notifications.published_at` | `scheduled_from` or publish time |
| `circles.last_reminder_sent_on` | Set by `sendPendingReminders` |
| `users.failed_login_attempts` | Incremented/reset by the auth service |
| `users.active_session_id` | Rotated on every sign-in |
