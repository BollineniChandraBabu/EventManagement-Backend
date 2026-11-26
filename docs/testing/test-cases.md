# Test cases

Each case has an ID, the preconditions, the steps, and the expected result. IDs are referenced from
[test-plan.md](test-plan.md) and [defect-tracking.md](defect-tracking.md).

## 1. Authentication (TC-AUTH)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-AUTH-001 | Password sign-in succeeds | POST `/api/auth/login` with a valid email/password | 200; `accessToken`, `refreshToken`, `role`, `expiresIn=900`, unread count |
| TC-AUTH-002 | Wrong password | POST with a valid email and wrong password | 400 `Invalid email or password`; `failed_login_attempts` +1 |
| TC-AUTH-003 | Unknown email | POST with an unknown email | 404 `User not found`; a `violated_users` row exists |
| TC-AUTH-004 | Lockout at threshold | Fail the password 5 times, then try the correct password | 400 lockout message; owner receives one security email |
| TC-AUTH-005 | Counter reset | Fail twice, sign in successfully, fail twice | Counter is 2 (reset happened), no lockout |
| TC-AUTH-006 | MFA gate | Set `mfaEnabled=true`, then sign in with the password | 400 `MFA_OTP_REQUIRED`; no tokens |
| TC-AUTH-007 | OTP flow | POST `/otp/send`, read the code from the mailbox, POST `/otp/verify-login` | 200 tokens; the OTP is marked used |
| TC-AUTH-008 | OTP reuse | Verify the same OTP twice | Second attempt 400 `Invalid or expired OTP` |
| TC-AUTH-009 | OTP resend cap | Call `/otp/send` six times within 5 minutes | Sixth call 400 `OTP resend limit reached…` |
| TC-AUTH-010 | OTP expiry | Request an OTP, wait 6 minutes, verify | 400 `Invalid or expired OTP` |
| TC-AUTH-011 | Forgot password | POST `/forgot-password` | 200; one email with a `token=` and `email=` link |
| TC-AUTH-012 | Reset with token | POST `/password-reset/confirm` with the emailed token and a new password | 200; old password fails, new password works |
| TC-AUTH-013 | Reset token single use | Repeat TC-AUTH-012 with the same token | 400 `Invalid token` |
| TC-AUTH-014 | Reset resend cap | Request a reset six times | Sixth call 400 `Reset link request limit reached…` |
| TC-AUTH-015 | Change password | Sign in, POST `/change-password` with the correct current password | 200; new password works |
| TC-AUTH-016 | Change password, wrong current | POST with a wrong current password | 400 `Current password is incorrect` |
| TC-AUTH-017 | Change password, same value | POST with `newPassword == currentPassword` | 400 `New password must be different from current password` |
| TC-AUTH-018 | Refresh | POST `/api/auth/refresh` with a valid refresh token | 200 new access token, same refresh token |
| TC-AUTH-019 | Refresh, revoked | Revoke the token in the database, then refresh | 400 `Invalid refresh token` (or 401 `CONCURRENT_LOGIN:` if single-session caused it) |
| TC-AUTH-020 | Single session | Set `concurrentLoginAllowed=false`, sign in twice, use the first access token | 401 body starts `CONCURRENT_LOGIN:` |
| TC-AUTH-021 | Single session, refresh | Same setup, refresh with the first refresh token | 401 `CONCURRENT_LOGIN: …` |
| TC-AUTH-022 | Concurrent allowed | With the flag true, sign in twice and use both tokens | Both succeed |
| TC-AUTH-023 | Rate limit | Send 11 login requests within 60 seconds from one IP | 11th returns 429 with `{status:429, message}` |
| TC-AUTH-024 | Rate limit isolation | Exceed the limit for `/login`, then call `/otp/send` | `/otp/send` is unaffected (per-path key) |
| TC-AUTH-025 | Direct login | Request a circle (which emails a link), POST `/api/auth/direct-login` with token+email | 200 tokens |
| TC-AUTH-026 | Direct login replay | Repeat the same direct-login call | 400 `Invalid or expired login link` |
| TC-AUTH-027 | Direct login wrong email | Call with a token and a different email | 400 `Invalid or expired login link` |
| TC-AUTH-028 | Impersonation | Admin POSTs `/api/auth/admin/login-as-user` with a user email | 200 tokens; the token contains `impersonatedBy` |
| TC-AUTH-029 | Impersonation of admin | Admin targets another admin | 400 `Admin cannot login as another admin` |
| TC-AUTH-030 | Switch back | With an impersonated token, GET `/api/auth/admin/switch-back` | 200 admin tokens |
| TC-AUTH-031 | Switch back, normal token | GET `/switch-back` with a normal user token | 400 `Switch-back is allowed only for impersonated sessions` |
| TC-AUTH-032 | Google SSO, bad token | POST `/sso/google` with a junk id token | 400 `Invalid Google ID token` or `Unable to verify Google ID token` |
| TC-AUTH-033 | SSO client id | GET `/api/auth/sso/token` | `{clientId}` equals the configured value |

## 2. Users (TC-USER)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-USER-001 | Create user | Admin POST `/api/users` with all fields | 200; the response has the values; sign-in with `Test` works |
| TC-USER-002 | Create with a bad relationship | Use `relationShip="COUSIN"` | 404/400; no user created |
| TC-USER-003 | Create with a bad wishes URL | `birthdayWishesUrl="ftp://x"` | 400 with the scheme message |
| TC-USER-004 | Create with an over-long wishes URL | 501 characters | 400 `must be at most 500 characters` |
| TC-USER-005 | Duplicate email | Create the same email twice | Second call fails (500 from the unique constraint — known issue) |
| TC-USER-006 | Update user | Admin PUT `/api/users` with changed name/settings | 200; values changed |
| TC-USER-007 | List with filters | GET `/api/users?roles=ROLE_USER&active=true` | Only matching users, paged |
| TC-USER-008 | Non-admin list | Call `GET /api/users` as a user | 403 |
| TC-USER-009 | Directory projection | GET `/api/users/directory` | No `role`, `dateOfBirth`, `mfaEnabled` or settings fields |
| TC-USER-010 | Directory search | GET `/api/users/directory?searchKey=chan` | Matching names/emails only |
| TC-USER-011 | Me | GET `/api/users/me` | The caller's record |
| TC-USER-012 | Wish settings | PATCH `/api/users/me/wish-settings` `{isBirthdayEnabled:false}` | 200; `isBirthdayEnabled=false` |
| TC-USER-013 | MFA toggle | PATCH `/api/users/me/mfa` `{mfaEnabled:true}` | 200; next password sign-in requires OTP |
| TC-USER-014 | Concurrent toggle, self | PATCH `/api/users/me/concurrent-login` `{concurrentLoginAllowed:false}` | 200 |
| TC-USER-015 | Concurrent toggle, other | As a non-admin, PATCH `/{otherId}/concurrent-login` | 403 |
| TC-USER-016 | Concurrent toggle, admin | As admin, PATCH another user's flag | 200 |
| TC-USER-017 | Wish preview | GET `/api/users/me/wish-preview` | `showMessage=true` with subject/html (and image bytes when available) |
| TC-USER-018 | Profile picture | POST `/api/users/me/profile-picture` with a PNG | 200 with a picture URL |
| TC-USER-019 | Profile picture, PDF | Upload a PDF | 400 `Only PNG, JPEG, GIF, WebP or BMP images are allowed` |
| TC-USER-020 | Presigned upload | POST `/me/profile-picture/presigned-url`, PUT the bytes to the URL, then GET `/me` | Picture URL set |
| TC-USER-021 | Remove picture | DELETE `/me/profile-picture` | 200; default picture returned afterwards |
| TC-USER-022 | Deactivate | Admin POST `/{id}/deactivate` | 200; sign-in for that user fails; not in directory |
| TC-USER-023 | Reactivate | PATCH `/{id}/status` `{active:true}` | 200; sign-in works again |
| TC-USER-024 | Bulk deactivate | DELETE `/api/users?ids=2,3` | 200; both soft-deleted |
| TC-USER-025 | Filter values | GET `/api/users/filter-values` as admin | `roles`, `genders`, `active` |

## 3. Events and calendar (TC-EVT, TC-CAL)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-EVT-001 | Create event | Admin POST `/api/events` with `eventType="anniversary"` | 200; type resolves to `ANNIVERSARY` |
| TC-EVT-002 | Unknown type | `eventType="nope"` | 400 `Invalid event type` |
| TC-EVT-003 | Create with image | POST `/api/events/with-image` (multipart) | 200 with `imageUrl` |
| TC-EVT-004 | Invalid image | Upload an SVG named `.png` | 400 with the media message |
| TC-EVT-005 | Update event | PUT `/{id}` with new date | 200; date changed |
| TC-EVT-006 | Replace image | PUT `/{id}/with-image` with a new image | 200; new key; old object deleted |
| TC-EVT-007 | Owner read | As the owner, GET `/{id}` | 200 |
| TC-EVT-008 | Stranger read | As another non-admin, GET `/{id}` | 404 |
| TC-EVT-009 | Non-admin list | As a user, GET `/api/events` | Only own events |
| TC-EVT-010 | Admin list | As admin, GET `/api/events?eventTypes=ANNIVERSARY` | All matching events |
| TC-EVT-011 | Delete | Admin DELETE `/{id}` | 200; the event and its image are gone |
| TC-EVT-012 | Bulk delete | DELETE `/api/events?ids=…` | 200 |
| TC-CAL-001 | Own calendar | GET `/api/calendar?month=<current>` | Festivals, own birthdays and events |
| TC-CAL-002 | Circle sharing | Accept a circle, then GET the calendar | The member's birthday/events appear with `fromCircle=true` |
| TC-CAL-003 | No duplicates | A member's entry that is also own appears once | Single entry, `fromCircle=false` |
| TC-CAL-004 | Admin calendar | As admin, GET the calendar | Everyone's entries |
| TC-CAL-005 | Year scoping | GET `?month=11&year=<next>` | Festivals dated in that year, `recurring=false` |
| TC-CAL-006 | Birthday recurrence | Any month with a birthday | `eventType=Birthday`, `recurring=true` |

## 4. Festivals and mappings (TC-FEST)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-FEST-001 | Monthly sync | Trigger `festivalMonthlySyncScheduler` with a valid key | Rows created/updated for the current year; the calendar shows them |
| TC-FEST-002 | Sync without a key | Clear `CALENDARIFIC_API_KEY` and trigger | Warning logged; no data change |
| TC-FEST-003 | Removed festival deactivated | Remove a festival from the provider response and re-sync | That row's `active=0` |
| TC-FEST-004 | On-demand year | GET `/api/festivals?month=1&year=<future>` | One provider call; rows stored; a second request is served from the database |
| TC-FEST-005 | Festival list paging | GET `/api/festivals?month=11&page=0&size=5` | Paged response with at most 5 rows |
| TC-FEST-006 | Create mapping | Admin POST `/api/festival-wish-mappings` | 200 with the mapping |
| TC-FEST-007 | Upsert mapping | POST the same festival/user again with a new note | 200; one row, note updated |
| TC-FEST-008 | Bulk mapping | POST `/bulk` with 3 users | `{created:3, updated:0}`; repeat → `{created:0, updated:3}` |
| TC-FEST-009 | Filter by status | GET `?status=FAILED` | Only mappings with `error=true` |
| TC-FEST-010 | Summary | GET `/summary` | Totals for total/active/inactive/upcoming/failed |
| TC-FEST-011 | My mappings | GET `/me` as a user | Only the caller's mappings |
| TC-FEST-012 | Mapping with image | POST `/with-image` | 200 with `imageUrl` |
| TC-FEST-013 | Delete mapping | DELETE `/{id}` and `?ids=` | 200 |
| TC-FEST-014 | Non-admin mapping list | GET `/api/festival-wish-mappings` as a user | 403 |
| TC-FEST-015 | Today's seeds | GET `/api/seed/special-events/today` | Active seeds whose date is today (IST) |

## 5. Wishes and email (TC-WISH)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-WISH-001 | Generate wish | POST `/api/ai/generate-wish` with a name and event | 200 `{subject, htmlMessage}` mentioning the name, no placeholders |
| TC-WISH-002 | Model fallback | Force the primary model to 429 (temporary invalid name) | Request still succeeds using another model |
| TC-WISH-003 | AI exhausted | Use an invalid key | 503 `The AI service is busy right now…` |
| TC-WISH-004 | Image from upload | Create a wish image for a user and event type, then send a wish | That image is used |
| TC-WISH-005 | Global image | Create an image with no user for the type | Used when no user-specific image exists |
| TC-WISH-006 | Generated image | No uploaded image for the type | Pollinations image used (or none, with the email still sent) |
| TC-WISH-007 | Wishes link | Set a birthday wishes URL, send a wish | Email contains the button and the plain link |
| TC-WISH-008 | Personal note | Festival mapping with a note | The note appears escaped in the email |
| TC-WISH-009 | Escaping | Note/name containing `<script>` | Rendered as text, not markup |
| TC-WISH-010 | Email log | Send any email | One `email_logs` row with type, status and `sent_at` |
| TC-WISH-011 | Retry | Force a failure, then fix the provider and trigger the retry | The same row becomes `SENT`, `retry_count` incremented |
| TC-WISH-012 | Retry cap | Fail three times | No further retry |
| TC-WISH-013 | Sensitive visibility | As a non-admin, GET `/status/{id}` of an OTP email | 403 |
| TC-WISH-014 | Own email visibility | As the recipient, GET a birthday email status | 200 |
| TC-WISH-015 | Image endpoint | GET `/status/{id}/image` for an email with an image | Image bytes, correct content type, `Cache-Control: private, max-age=600` |
| TC-WISH-016 | Status tabs | GET `/status?mailTab=BIRTHDAY` | Only birthday emails (own, or all for admin) |
| TC-WISH-017 | Admin OTP list | GET `/status/admin/otp` as admin | Only OTP emails |
| TC-WISH-018 | Send now | Admin POST `/send-now` | The user receives the email; a log row is created |
| TC-WISH-019 | Test email | POST `/api/email/test` | The caller receives the test message |
| TC-WISH-020 | Delete email | Admin DELETE `/status/{id}` and `?ids=` | Rows removed; images removed from storage |
| TC-WISH-021 | Duplicate birthday guard | Run `birthdayJob` twice for the same day | One email; the second run skips |
| TC-WISH-022 | Festival retry same day | Force a festival send failure, then trigger again | The mapping is retried because `is_error=true` |
| TC-WISH-023 | Agent composer | With `AGENT_WISH_ENABLED=true`, request a wish | Accepted output passes the validator; no placeholders |
| TC-WISH-024 | Agent fallback | Force the agent to fail | The single-prompt path still returns a wish |

## 6. Circles and chat (TC-CIR, TC-CHAT)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-CIR-001 | Send request | User A POST `/requests` with user B | `PENDING`; B receives an email with a link |
| TC-CIR-002 | Request to self | POST with the caller's own id | 400 `You cannot send a circle request to yourself` |
| TC-CIR-003 | Duplicate request | Send the same request twice | 400 `A circle request is already pending` |
| TC-CIR-004 | Mutual request | A requests B, then B requests A | The circle becomes `ACCEPTED` |
| TC-CIR-005 | Accept | B POST `/requests/accept` | `ACCEPTED`; A receives a confirmation email |
| TC-CIR-006 | Accept by a stranger | C tries to accept B's request | 400 `Only the addressee can respond…` |
| TC-CIR-007 | Accept twice | Accept an already accepted circle | 400 `This circle request has already been handled` |
| TC-CIR-008 | Reject | B POST `/requests/reject` | `REJECTED`; no reminder afterwards |
| TC-CIR-009 | Re-request after rejection | A requests B again | The same row is reused, `PENDING`, direction flipped |
| TC-CIR-010 | List | GET `/api/circles` | `circles`, `incomingRequests`, `outgoingRequests` |
| TC-CIR-011 | Status | GET `/status/{userId}` | Correct `status` and `inCircle` |
| TC-CIR-012 | Remove | Either participant DELETE `/api/circles` | 200; no longer in each other's list |
| TC-CIR-013 | Reminder | Age a pending request past the threshold, trigger `circleRequestReminderJob` | One reminder email; a second run the same day sends nothing |
| TC-CIR-014 | Mail failure tolerated | Break Gmail credentials and send a request | Request still created; error logged |
| TC-CHAT-001 | Circle gate | With circles-only on, message a non-circle user | 400 `You can only message users in your circle` |
| TC-CHAT-002 | After acceptance | Accept the circle, then message | 200; the message is stored |
| TC-CHAT-003 | Empty message | POST with no text, no payload and no attachment | 400 `Message text, encrypted payload or attachment is required` |
| TC-CHAT-004 | Attachment | Send a 1 MB PDF | 200; attachment downloadable with `application/pdf` |
| TC-CHAT-005 | Oversized attachment | Send 60 MB | Rejected (413 or 400) before storage |
| TC-CHAT-006 | Spoofed attachment | Send an SVG named `.png` | 400 from media validation |
| TC-CHAT-007 | Conversation | GET `/messages/{otherUserId}` | Messages, paged, oldest/newest per the sort |
| TC-CHAT-008 | Mark seen | GET `/messages/{otherUserId}?markSeen=true`, then GET `/unread-count` | Unread count for that conversation is 0 |
| TC-CHAT-009 | Unread count in sign-in | Sign in with unread messages | `unreadChatMessagesCount > 0` |
| TC-CHAT-010 | Reply | Send with `replyToMessageId` | Response includes `replyMessageText` |
| TC-CHAT-011 | Edit | PATCH `/messages/{id}` as the sender | 200; text changed |
| TC-CHAT-012 | Edit by other | PATCH as the receiver | 403/400 |
| TC-CHAT-013 | Delete last | DELETE `/messages/last/{otherUserId}` | The caller's last message is gone |
| TC-CHAT-014 | Reactions | POST `/reactions` twice with the same emoji | One reaction (idempotent) |
| TC-CHAT-015 | Presence | POST `/presence/heartbeat`, then GET `/api/chat/users/active` | The caller appears online |
| TC-CHAT-016 | Stale presence | Wait more than 2 minutes without a heartbeat, then list | The user appears offline |
| TC-CHAT-017 | Realtime | Send via STOMP `/app/chat.send` | The message appears on `/topic/chat/{conversationId}` and is persisted |
| TC-CHAT-018 | Unread digest | Create unread messages, trigger `unreadChatEmailJob` | One email per receiver |

## 7. Notifications (TC-NOTIF)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-NOTIF-001 | Create | Admin POST `/api/notifications` | 200 with `published=false` |
| TC-NOTIF-002 | Publish now | POST `/{id}/publish` | `published=true`, `publishedAt` set |
| TC-NOTIF-003 | Single published | Publish a second notification | The first becomes unpublished |
| TC-NOTIF-004 | Public read | GET `/api/notifications/published` without a token | The published notification |
| TC-NOTIF-005 | Future schedule | Create with `scheduledFrom` in 2 minutes, publish | Not published immediately; published at the time |
| TC-NOTIF-006 | Past schedule rejected | Publish with `scheduledFrom` in the past | 400; the previously published one is untouched |
| TC-NOTIF-007 | Auto unpublish | Publish with `scheduledTo` in 2 minutes | Unpublished at that time |
| TC-NOTIF-008 | Edit published | PUT a published notification | 400 (must unpublish first) |
| TC-NOTIF-009 | Email fan-out | Publish with `canSendEmail=true` | Every active user receives an email |
| TC-NOTIF-010 | Non-admin create | POST as a user | 403 |

## 8. Media (TC-MEDIA)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-MEDIA-001 | Valid PNG | Upload a 100 KB PNG | Accepted; content type `image/png` |
| TC-MEDIA-002 | Large image | Upload a 6 MB PNG | 400 with the image size message |
| TC-MEDIA-003 | Large video | Upload a 60 MB MP4 | 413 (servlet limit) |
| TC-MEDIA-004 | Renamed SVG | Upload an SVG named `.png` | 400 (bytes do not match) |
| TC-MEDIA-005 | PDF as chat attachment | Send a 2 MB PDF | Accepted with `application/pdf` |
| TC-MEDIA-006 | Gzip at rest | Upload an image with gzip on, inspect the object | Object starts with `1f 8b`; reading returns valid bytes |
| TC-MEDIA-007 | Gzip off | Set `APP_MEDIA_GZIP_ENABLED=false`, upload, inspect | Stored uncompressed; still readable |
| TC-MEDIA-008 | Empty file | Upload a 0-byte file | 400 `A non-empty file is required` |
| TC-MEDIA-009 | Wish image | Admin POST `/api/admin/wish-images` with a JPEG | 201 with `imageUrl` |
| TC-MEDIA-010 | Wish image update | PUT `/{id}` with a new file | 200; new image; old object deleted |
| TC-MEDIA-011 | Wish image delete | DELETE `/{id}` | 204; object removed |
| TC-MEDIA-012 | Wish image filters | GET `?eventType=Birthday&active=true` | Matching rows only |

## 9. Themes and seed data (TC-THEME, TC-SEED)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-THEME-001 | List themes | GET `/api/themes` | 11 themes with `code` and `displayName` |
| TC-THEME-002 | Save theme | PUT `/api/themes/me` `{theme:"liquid-glass"}` | 200; GET `/me` returns it |
| TC-THEME-003 | Unknown theme | PUT with `{theme:"neon"}` | 400 |
| TC-THEME-004 | Default theme | A user with no preference | GET `/me` returns `default` |
| TC-SEED-001 | Create event type | Admin POST `/api/seed/event-types` | 200 with `code` |
| TC-SEED-002 | Update by code | PUT `/api/seed/event-types/{code}` | 200; `displayName` changed |
| TC-SEED-003 | List with search | GET `?searchKey=ann` | Matching rows only |
| TC-SEED-004 | Delete | DELETE `/{id}` and `?ids=` | 200 |
| TC-SEED-005 | Relationship seeds | CRUD on `/api/seed/relationships` | Works as above |
| TC-SEED-006 | Template types | Create/list `/api/seed/template-types` | Works; no delete endpoint |
| TC-SEED-007 | Special events | CRUD on `/api/seed/special-events` | Works; calendar cache evicted after a write |
| TC-SEED-008 | Non-admin write | POST a seed as a user | 403 |

## 10. Dashboards and agents (TC-DASH, TC-AGENT)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-DASH-001 | Mail summary | GET `/api/dashboard/mail` | Four counters; a second call within 60 s is served from cache |
| TC-DASH-002 | Mail chart | GET `/api/dashboard/chart/mail?startDate=…&endDate=…` | Daily points for the range |
| TC-DASH-003 | Same-day chart | Pass identical dates | 400 `startDate and endDate must not be the same` |
| TC-DASH-004 | Future end date | `endDate` tomorrow | 400 `endDate must not be greater than today` |
| TC-DASH-005 | Bad date format | `startDate=2026/01/01` | 400 `Invalid date format…` |
| TC-DASH-006 | Violated users | GET `/violated-users` as admin | Totals, chart, map points (≤300), page, top lists |
| TC-DASH-007 | Paging bounds | `size=0` then `size=501` | Both 400 |
| TC-DASH-008 | Non-admin violated users | GET as a user | 403 |
| TC-DASH-009 | OTP dashboard | GET `/api/dashboard/mail/otp` as admin | OTP counters |
| TC-DASH-010 | Login locations | GET `/chart/login-locations` | Points with location and IP; `truncated` when > 1000 |
| TC-DASH-011 | Instagram counters | GET `/api/dashboard/insta` | Counters (zeros when unused) |
| TC-AGENT-001 | Status | GET `/api/agent/status` | Three flags and the failed-email count |
| TC-AGENT-002 | Copilot question | POST `/api/agent/admin/ask` with a question | `{answer}` |
| TC-AGENT-003 | Copilot disabled | Set `AGENT_COPILOT_ENABLED=false` and ask | 503 |
| TC-AGENT-004 | Delivery ops dry run | POST `/delivery-ops/run` `{dryRun:true}` | `ran=true`, report describes actions, nothing changed |
| TC-AGENT-005 | Delivery ops concurrent | Start two runs | Second returns 409 |
| TC-AGENT-006 | Delivery ops disabled | Set `AGENT_DELIVERY_OPS_ENABLED=false` | 503 |

## 11. Operations (TC-OPS)

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| TC-OPS-001 | Health | GET `/health` without a token | 200 `{"status":"UP","time":…}` |
| TC-OPS-002 | OpenAPI | GET `/v3/api-docs` | Valid OpenAPI 3 JSON |
| TC-OPS-003 | Swagger UI | Open `/swagger-ui.html` | Page renders and lists the endpoints |
| TC-OPS-004 | Scheduler list | GET `/api/schedulers` | Nine Quartz jobs with counters and next fire times |
| TC-OPS-005 | Scheduler detail | GET `/api/schedulers/dailyWishJob` | Status for that job |
| TC-OPS-006 | Scheduler trigger | POST `/api/schedulers/dailyWishJob/trigger` | `triggeredAt` returned; counters increase |
| TC-OPS-007 | Unknown job | GET `/api/schedulers/nope` | 404 |
| TC-OPS-008 | Refresh cleanup | Insert an expired refresh token, trigger the job | The token is deleted |
| TC-OPS-009 | Instagram verify | GET `/instagram/webhook?hub.mode=subscribe&hub.verify_token=<correct>&hub.challenge=123` | Body `123` |
| TC-OPS-010 | Instagram verify, wrong token | Same with a wrong token | 403/400 |
| TC-OPS-011 | Instagram event | POST `/instagram/webhook` with a payload | 200 `EVENT_RECEIVED`; payload logged |
| TC-OPS-012 | Startup with missing secret | Start without `JWT_SECRET` | Startup fails with a clear error |
| TC-OPS-013 | Chat schema init | Start against an empty chat database | Tables and indexes created |

## 12. Traceability

| Case group | Requirement | Automated test |
| --- | --- | --- |
| TC-AUTH | FR-AUTH-01…18 | `AuthServiceImplTest`, `JwtServiceTest`, `JwtAuthFilterTest`, `RateLimiterTest` |
| TC-USER | FR-USER-01…10 | `UserServiceImplTest`, `UserDirectoryTest` |
| TC-EVT | FR-EVT-01…07 | `EventServiceImplTest` |
| TC-CAL | FR-CAL-01…06 | `CalendarServiceImplTest` |
| TC-FEST | FR-FEST-01…08 | `FestivalWishMappingServiceImplTest`, `SpecialEventSeedServiceImplTest` |
| TC-WISH | FR-WISH-01…14 | `AiServiceImplTest`, `GmailEmailServiceImplTest`, `WishEmailExtrasTest` |
| TC-CIR | FR-CIR-01…10 | `CircleServiceImplTest`, `CircleRequestNotifierTest` |
| TC-CHAT | FR-CHAT-01…09 | `ChatServiceImplCircleGateTest` |
| TC-NOTIF | FR-NOTIF-01…05 | `NotificationServiceImplTest` |
| TC-MEDIA | FR-MEDIA-01…05 | `MediaValidationTest`, `MediaContentTypesTest`, `GzipUtilTest`, `WishImageServiceImplTest` |
| TC-THEME / TC-SEED | FR-THEME-01…03, FR-SEED-01…04 | `ThemeServiceImplTest` |
| TC-DASH | FR-DASH-01…07 | `DashboardServiceImplTest` |
| TC-AGENT | FR-OPS-04…05 | `AgentRunnerTest` |
| TC-OPS | FR-OPS-01…05 | `CacheConfigurationTest`, manual |
