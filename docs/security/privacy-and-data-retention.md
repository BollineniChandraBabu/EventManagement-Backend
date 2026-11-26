# Privacy and data retention

## 1. What personal data is held

| Data | Purpose | Where |
| --- | --- | --- |
| Name | Greeting text, calendar, chat | `users.name` |
| Email | Sign-in, email delivery | `users.email`, `email_logs.to_email` (via user id) |
| Gender | Default profile picture | `users.gender` |
| Date of birth | Birthday wishes, calendar | `users.birthday` |
| Relationship | Greeting tone and text | `users.relationship_seed_id` |
| Profile picture | UI, chat | `users.profile_picture_url` + storage |
| Wish preferences | Which wishes to receive | `user_wish_settings` |
| Wishes URL | Personal page linked in emails | `users.birthday_wishes_url`, `events.wishes_url`, `festival_wish_mappings.wishes_url` |
| Events (dates, types) | Wishes and calendar | `events` |
| Chat messages and attachments | Communication | chat database + storage |
| Sign-in location and IP | Security analytics | `login_location_events` |
| Failed sign-in attempts (email, password, IP) | Security analytics | `violated_users` |
| Theme preference | UI | `user_theme_preferences` |

## 2. Legal and ethical position

The product serves a single family group. The family administrator creates accounts; members can
disable wishes, leave circles and ask the administrator to deactivate their account. No data is sold,
shared for advertising, or transferred to third parties beyond the processors below.

## 3. Processors

| Processor | Data shared | Purpose |
| --- | --- | --- |
| Google (Gmail API) | Recipient address, subject, body, images | Email delivery |
| Google (Gemini) | Name, relationship, occasion, tone, language, recent wish subjects | Wish text generation |
| Pollinations | A generated image prompt (occasion, name style hints) | Greeting image |
| Supabase | Media objects, chat messages | Storage and chat database |
| Neon | All business data | Primary database |
| Calendarific | Country and year only | Festival dates |
| ip-api.com | An IP address | Geolocation of sign-ins |
| Render | Application logs (no personal content by design) | Hosting |

Notes:

* Emails inherently pass through the recipient's provider.
* The AI prompt includes the recipient's **name** and **relationship**. It does not include the email
  address, tokens or ids other than an internal user id used to look up preferences.
* The delivery-ops agent explicitly withholds recipient addresses from its tool output.

## 4. Retention schedule

| Data | Retention | Mechanism |
| --- | --- | --- |
| User record | Until deactivated, then kept (soft delete) | `users.deleted = true` |
| Wish settings, theme | While the account exists | Cascaded |
| Events | Until deleted by an admin | Manual |
| Festival mappings | Until deleted | Manual |
| Email logs (bodies, images) | Indefinite today | Admin delete endpoints |
| Refresh tokens | Until expiry (7 days), pruned daily | `RefreshTokenCleanupScheduler` |
| OTP codes | 5 minutes validity; rows not pruned | Manual |
| Password-reset tokens | 30 minutes validity; rows not pruned | Manual |
| Direct-login tokens | 60 minutes validity; rows not pruned | Manual |
| `login_location_events` | Indefinite today | Manual |
| `violated_users` | Indefinite today | Manual |
| Chat messages | Until deleted by a participant or an operator | Manual |
| Chat attachments | With the message | Manual |
| Storage objects | With the referencing row | Manual |
| Backups | Provider default (typically 7–30 days) | Provider policy |
| Application logs | Provider default | Provider policy |

## 5. Deletion

| Request | Current behaviour | Recommended |
| --- | --- | --- |
| "Remove me" | Admin deactivates the user (soft delete); data remains | Add a hard-delete routine for user-owned rows |
| Delete a chat message | Participant deletes it; reactions removed | Keep |
| Delete an email log | Admin deletes the row; the image object is deleted | Keep |
| Delete an event | Admin deletes it; the image is deleted | Keep |
| Right to erasure | Not automated | Implement a documented erasure job |

### Hard-delete order (for a user)

```sql
DELETE FROM chat_message_reactions WHERE user_id = :id;
DELETE FROM chat_messages WHERE sender_id = :id OR receiver_id = :id;
DELETE FROM chat_conversations WHERE user_a_id = :id OR user_b_id = :id;
DELETE FROM festival_wish_mappings WHERE user_id = :id;
DELETE FROM events WHERE user_id = :id;
DELETE FROM wish_images WHERE user_id = :id;
DELETE FROM circles WHERE requester_id = :id OR addressee_id = :id;
DELETE FROM email_logs WHERE recipient_user_id = :id;
DELETE FROM otp_codes WHERE user_id = :id;
DELETE FROM password_reset_tokens WHERE user_id = :id;
DELETE FROM refresh_tokens WHERE user_id = :id;
DELETE FROM login_tokens WHERE user_id = :id;
DELETE FROM login_location_events WHERE user_id = :id;
DELETE FROM user_wish_settings WHERE user_id = :id;
DELETE FROM user_theme_preferences WHERE user_id = :id;
DELETE FROM users WHERE id = :id;
```

Also delete the user's storage objects (profile picture, chat attachments, wish images) and any
`violated_users` rows with their email.

## 6. Security-event data

`violated_users` holds attempted emails, passwords and IPs. This is the most sensitive table in the
system:

1. The password column must be removed (see DEF-001).
2. Access should be restricted to the operator.
3. Retention should be capped (for example 90 days) once the analytics requirement is understood.
4. It should be excluded from any data export.

## 7. Data subject rights (as practised)

| Right | How it is honoured today |
| --- | --- |
| Access | A member can read their profile, events, calendar and emails through the API |
| Rectification | Admin updates the profile; members update their own settings |
| Erasure | Manual (see §5) |
| Restriction | Wish settings and circle removal limit processing |
| Portability | Not implemented; data can be exported with SQL |
| Objection | Members can leave circles and disable wishes |

## 8. Privacy by design in the code

1. **Minimum fields** — no address, phone, ID numbers or payment data.
2. **Reduced directory projection** — other members see names, emails, relationships and pictures
   only.
3. **Circle-gated chat** — messaging requires an accepted relationship.
4. **Sensitive email types** — OTP, reset and circle emails are admin-only.
5. **No personal data in logs** (by design; review each release).
6. **AI prompt minimisation** — no email addresses or tokens are sent to providers.

## 9. Recommendations

| # | Recommendation | Priority |
| --- | --- | --- |
| 1 | Drop `violated_users.password` | High |
| 2 | Add a retention job for security tables (90 days) | Medium |
| 3 | Add an admin "erase user" endpoint implementing §5 | Medium |
| 4 | Add a self-service account deactivation | Medium |
| 5 | Document the processor list in the UI privacy note | Low |
| 6 | Add data export (JSON) for a user | Low |
| 7 | Prune expired OTP/reset/login tokens | Low |

## 10. Review

Review this document when:

1. A new personal field is added.
2. A new processor is introduced.
3. A retention or deletion feature is implemented.
4. A member requests erasure.
