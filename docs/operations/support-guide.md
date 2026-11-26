# Support guide

How to investigate and resolve a problem reported by a family member.

## 1. First questions

1. **Who** is affected (one member, a few, everyone)?
2. **What** were they doing (sign-in, calendar, chat, a wish)?
3. **When** did it happen (date, time, time zone)?
4. **What did they see** (exact message, screenshot)?
5. **Which device/browser**?

The answers decide which section below applies.

## 2. Triage matrix

| Symptom | Likely area | First check |
| --- | --- | --- |
| Cannot sign in | Auth | Account exists, active, not locked ([troubleshooting-guide.md](troubleshooting-guide.md) §2) |
| Signed out unexpectedly | Single-session | `users.concurrent_login_allowed` |
| Did not receive a birthday wish | Wish settings or email | `user_wish_settings`, `last_birthday_wish_sent`, `email_logs` |
| Wish looks wrong | AI output | `email_logs.body` for the message |
| Wish has no image | Pollinations or wish images | `email_logs.image_url` |
| Calendar missing entries | Events or circle | Events exist, circle accepted |
| Festival dates wrong | Festival year | Request the correct year |
| Cannot send a chat message | Circle gate | `GET /api/circles/status/{userId}` |
| Chat attachment will not open | Storage | Object exists, content type |
| Cannot upload a picture | Media validation | File type and size |
| Announcement not visible | Publish state | `GET /api/notifications/published` |
| Dashboard numbers look wrong | Cache or scope | Wait 60 seconds, check the role |

## 3. Investigation steps

### 3.1 Identify the user

```sql
SELECT id, name, email, active, deleted, failed_login_attempts,
       concurrent_login_allowed, mfa_enabled, birthday, last_birthday_wish_sent,
       birthday_wishes_url
FROM users WHERE email = 'user@example.com';
```

### 3.2 Check their wish settings

```sql
SELECT * FROM user_wish_settings WHERE user_id = :id;
```

### 3.3 Check recent emails

```sql
SELECT id, subject, status, email_type, retry_count, sent_at, error_message
FROM email_logs WHERE recipient_user_id = :id
ORDER BY id DESC LIMIT 10;
```

Or through the API:

```bash
curl -s "$BASE/api/emails/status?page=0&size=10" -H "Authorization: Bearer $ADMIN"
```

### 3.4 Check the event or mapping

```sql
SELECT e.id, t.code, e.event_date, e.recurring, e.active
FROM events e JOIN event_type_seeds t ON t.id = e.event_type_seed_id
WHERE e.user_id = :id;

SELECT m.*, s.event_name, s.event_date
FROM festival_wish_mappings m JOIN seed_special_events s ON s.id = m.special_event_id
WHERE m.user_id = :id;
```

### 3.5 Check the circle

```sql
SELECT * FROM circles
WHERE (requester_id = :a AND addressee_id = :b)
   OR (requester_id = :b AND addressee_id = :a);
```

### 3.6 Check the scheduler

```bash
curl -s "$BASE/api/schedulers/birthdayJob" -H "Authorization: Bearer $ADMIN"
```

## 4. Common fixes

| Problem | Fix |
| --- | --- |
| Locked account | `UPDATE users SET failed_login_attempts = 0 WHERE id = :id;` or reset the password |
| User forgot the password | Admin resets it, or the user uses the forgot-password flow |
| Wishes disabled | `PATCH /api/users/me/wish-settings` (the user) or the admin UI |
| Birthday missing | Set `users.birthday` |
| Event missing or inactive | Create or reactivate the event |
| Wish already sent | Expected; the guard prevents duplicates |
| Email failed | Check `error_message`; retry by triggering the job |
| Circle not accepted | Ask the addressee to accept |
| Chat blocked | Accept the circle first |
| Picture too large | Resize below 5 MB |
| Announcement not published | Publish it (admin) |

## 5. When to escalate

| Condition | Escalate to |
| --- | --- |
| More than one user affected | Operator (possible incident) |
| Data missing or wrong | Operator (possible restore) |
| Security concern | Backend owner |
| Provider outage suspected | Operator; check status pages |

## 6. What not to do

1. Do not edit the database directly unless the documented fix requires it.
2. Do not trigger jobs repeatedly to "try again" — check the guard first.
3. Do not share one member's data with another.
4. Do not send screenshots containing tokens or OTPs.

## 7. Reply templates

**Wish not received**

> I checked your account: birthday wishes are enabled and the wish was scheduled for 06:30 IST. The
> email was sent to <address>; please check the spam folder. If it is not there, I will resend it
> today.

**Cannot sign in**

> Your account was temporarily locked after several incorrect passwords. I have unlocked it. Please
> try again, or use "Forgot password" to set a new one.

**Chat unavailable**

> Chat is limited to members of your circle. Please send a request from the Circles page and ask them
> to accept it; then messaging will work.

## 8. Logging the case

Record: date, member, symptom, checks performed, fix applied, and whether a defect should be opened
([../testing/defect-tracking.md](../testing/defect-tracking.md)).
