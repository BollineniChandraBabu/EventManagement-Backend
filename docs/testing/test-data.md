# Test data

## 1. Principles

1. **Never test against production data.** Use a local or QA database.
2. **Deterministic dates.** Use fixed dates (birthdays, anniversaries, festivals) so month/day
   filters are predictable.
3. **Unique emails per run.** Suffix test accounts (for example `test.user1+<run>@example.com`) so
   the unique constraint does not block repeated runs.
4. **A dedicated mailbox.** Emails really are sent; use a mailbox you own and can read.

## 2. Seed data required before any test

| Data | Values | Created by |
| --- | --- | --- |
| Relationship seeds | `WIFE`, `HUSBAND`, `FRIEND`, `MOTHER`, `SISTER`, `BROTHER`, `SON`, `DAUGHTER`, `DAUGHTER_IN_LAW`, `ADMIN` | `WishTemplateSeedDataInitializer` / `/api/seed/relationships` |
| Event type seeds | `ANNIVERSARY`, `ENGAGEMENT`, `FESTIVAL` (and any others used) | `/api/seed/event-types` |
| Template types | `GOOD_MORNING`, `FESTIVAL` | Initializer |
| Theme seeds | 11 themes | Initializer |
| Admin user | `admin@example.com` with `ROLE_ADMIN` | Manual SQL or API as an existing admin |

## 3. Standard test accounts

| Alias | Role | Purpose | Notable fields |
| --- | --- | --- | --- |
| `admin` | ROLE_ADMIN | Admin operations, impersonation | `concurrentLoginAllowed=true` |
| `alice` | ROLE_USER | Primary member | Birthday today + 5 days, `birthdayEnabled=true` |
| `bob` | ROLE_USER | Circle partner of Alice | Birthday in the same month |
| `carol` | ROLE_USER | Non-circle user (negative tests) | `birthdayEnabled=false` |
| `dave` | ROLE_USER | Single-session tests | `concurrentLoginAllowed=false` |
| `erin` | ROLE_USER | MFA tests | `mfaEnabled=true` |

All start with password `Test` (the value the API sets on creation).

## 4. Standard test objects

| Object | Setup | Used by |
| --- | --- | --- |
| Recurring anniversary | Alice, `eventDate` = today's month/day 3 years ago, `recurring=true` | Wish scheduling, calendar |
| One-time event | Alice, `eventDate` = today, `recurring=false` | Recurrence filter |
| Past one-time event | Alice, `eventDate` = last year | Recurrence exclusion |
| Event with image | Bob, PNG uploaded | Image lifecycle |
| Event with wishes URL | Alice, `https://example.com/wishes` | Wishes link rendering |
| Festival mapping | Today's festival → Alice, `active=true`, note `With love` | Festival wish |
| Wish image (user) | Alice + `Birthday`, JPEG | Image priority |
| Wish image (global) | `Birthday`, PNG | Fallback priority |
| Accepted circle | Alice ↔ Bob | Calendar sharing, chat |
| Pending circle | Carol → Alice, created 8 days ago | Reminders |
| Unread chat | Bob → Alice, 2 messages | Unread count, digest |
| Published notification | Title + message | Public read |
| Failed email row | `status=FAILED`, `retryCount=0` | Retry, delivery-ops |

## 5. Test files

| File | Purpose |
| --- | --- |
| `tiny.png` (1×1, ~70 bytes) | Minimum valid image |
| `photo.jpg` (≈200 KB) | Realistic image |
| `large.png` (6 MB) | Image size rejection |
| `doc.pdf` (≈1 MB) | PDF acceptance |
| `clip.mp4` (≈2 MB) | Video acceptance |
| `huge.mp4` (≈60 MB) | Servlet limit |
| `fake.png` (SVG content) | Byte-sniffing rejection |
| `empty.bin` (0 bytes) | Empty-file rejection |

Generate them locally; do not commit large binaries.

## 6. Date fixtures

| Scenario | How to set up |
| --- | --- |
| Birthday today | Set `users.birthday` to today's month/day in a past year |
| Event today | `events.event_date = today` |
| Festival today | `seed_special_events.event_date = today`, active, mapped |
| Reminder due | `circles.created_at = now - 8 days`, `status=PENDING` |
| Expired token | `refresh_tokens.expires_at = now - 1 day` |
| Year browsing | Request `year = current + 1` |

## 7. Database reset

```sql
-- Order matters because of foreign keys
DELETE FROM festival_wish_mappings;
DELETE FROM events;
DELETE FROM wish_images;
DELETE FROM circles;
DELETE FROM user_wish_settings;
DELETE FROM user_theme_preferences;
DELETE FROM email_logs;
DELETE FROM otp_codes;
DELETE FROM password_reset_tokens;
DELETE FROM refresh_tokens;
DELETE FROM login_tokens;
DELETE FROM login_location_events;
DELETE FROM violated_users;
DELETE FROM users WHERE email LIKE '%@example.com';
```

For chat:

```sql
DELETE FROM chat_message_reactions;
DELETE FROM chat_messages;
DELETE FROM chat_conversations;
```

## 8. Data hygiene rules

* Never copy production personal data into a test database.
* Never commit real credentials, tokens or phone numbers.
* Use `@example.com` addresses.
* Delete test accounts after a run when using a shared environment.
* Keep the storage test bucket separate from production; delete uploaded objects after runs.
