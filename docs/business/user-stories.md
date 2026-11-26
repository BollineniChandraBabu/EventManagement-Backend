# User stories

Priorities: **M**ust, **S**hould, **C**ould. Each story lists the API that satisfies it, so it can
be verified directly.

## Epic 1 — Sign in and account security

| ID | Story | Priority | Implementation |
| --- | --- | --- | --- |
| US-001 | As a family member, I want to sign in with my email and password so that I can see my dates and wishes. | M | `POST /api/auth/login` |
| US-002 | As a family member, I want the session to stay alive without re-entering my password all day, so that the app is convenient. | M | Access token 15 min + `POST /api/auth/refresh` |
| US-003 | As a family member, I want to sign in with a one-time code when I do not want to type my password, so that I can still get in. | S | `POST /api/auth/otp/send`, `POST /api/auth/otp/verify-login` |
| US-004 | As a family member, I want to sign in with Google so that I do not manage another password. | S | `POST /api/auth/sso/google` |
| US-005 | As a family member, I want to reset a forgotten password from an emailed link, so that I am not locked out. | M | `POST /api/auth/forgot-password`, `POST /api/auth/password-reset/confirm` |
| US-006 | As a family member, I want to change my password while signed in, so that I can rotate it. | M | `POST /api/auth/change-password` |
| US-007 | As a family member, I want to be told when my account is locked after repeated wrong passwords, so that I know to contact the administrator. | M | `AuthServiceImpl.authenticateUser` |
| US-008 | As a family member, I want to allow only one active session when I choose, so that a lost device cannot keep using my account. | S | `PATCH /api/users/me/concurrent-login`, `sid` claim |
| US-009 | As a family member, I want to enable a second factor, so that a stolen password is not enough. | C | `PATCH /api/users/me/mfa`, `MFA_OTP_REQUIRED` |
| US-010 | As an administrator, I want to sign in as a user to reproduce their problem, and switch back, so that support is fast. | C | `POST /api/auth/admin/login-as-user`, `GET /api/auth/admin/switch-back` |
| US-011 | As an administrator, I want alerts for repeated attempts on unknown accounts, so that I can spot probing. | S | Unknown-email alert in `AuthServiceImpl.login` |

## Epic 2 — People and preferences

| ID | Story | Priority | Implementation |
| --- | --- | --- | --- |
| US-020 | As an administrator, I want to add a family member with their birthday and relationship, so that wishes know who they are. | M | `POST /api/users` |
| US-021 | As an administrator, I want to edit, deactivate and reactivate members, so that the list stays accurate. | M | `PUT /api/users`, `PATCH /api/users/{id}/status`, `POST /api/users/{id}/deactivate` |
| US-022 | As an administrator, I want to search and filter members, so that I can find someone quickly. | M | `GET /api/users` |
| US-023 | As a family member, I want to choose which wishes I receive (good morning, good night, birthday), so that my inbox matches my taste. | M | `PATCH /api/users/me/wish-settings` |
| US-024 | As a family member, I want a profile picture, so that my circle and chat show me. | S | `POST /api/users/me/profile-picture`, presigned URL variant |
| US-025 | As a family member, I want to preview my birthday wish, so that I know what will be sent. | C | `GET /api/users/me/wish-preview` |
| US-026 | As a family member, I want to pick a UI theme, so that the app feels mine. | C | `PUT /api/themes/me` |
| US-027 | As a family member, I want to find other members by name or email, so that I can add them to my circle. | M | `GET /api/users/directory` |
| US-028 | As a family member, I want to link a personal birthday wishes page, so that my email can point to something I made. | C | `birthdayWishesUrl` on `POST/PUT /api/users` |

## Epic 3 — Dates

| ID | Story | Priority | Implementation |
| --- | --- | --- | --- |
| US-030 | As an administrator, I want to record an anniversary or engagement for a member, so that it is wished every year. | M | `POST /api/events` |
| US-031 | As an administrator, I want to mark an event as one-time or yearly, so that a wedding date repeats but a one-off does not. | M | `recurring` flag |
| US-032 | As an administrator, I want to upload a card for a specific event, so that the email uses a real photo. | S | `POST /api/events/with-image` |
| US-033 | As an administrator, I want to attach a custom wishes page to an event, so that the email can link to it. | C | `wishesUrl` |
| US-034 | As a family member, I want a month calendar of birthdays, anniversaries and festivals, so that I can plan. | M | `GET /api/calendar` |
| US-035 | As a family member, I want my circle's dates on the same calendar, so that I do not miss theirs. | S | `CalendarServiceImpl.listByMonth` |
| US-036 | As a family member, I want to browse another year's festivals, so that I can look ahead. | C | `GET /api/calendar?year=`, `GET /api/festivals?year=` |

## Epic 4 — Festivals

| ID | Story | Priority | Implementation |
| --- | --- | --- | --- |
| US-040 | As an administrator, I want festival dates filled automatically, so that I do not maintain them by hand. | M | `FestivalSyncScheduler` → `syncCalendarificFestivals` |
| US-041 | As an administrator, I want to map a festival to members, individually or in bulk, so that they all get a wish. | M | `POST /api/festival-wish-mappings`, `/bulk` |
| US-042 | As an administrator, I want a personal note on a mapping, so that the festival wish sounds like the family. | C | `customMessage` |
| US-043 | As an administrator, I want to see which festival wishes failed, so that I can fix them. | M | `GET /api/festival-wish-mappings?status=FAILED` |
| US-044 | As an administrator, I want festival counters, so that I know how many are planned. | S | `GET /api/festival-wish-mappings/summary` |
| US-045 | As a family member, I want to see the festivals mapped to me, so that I know what is coming. | C | `GET /api/festival-wish-mappings/me` |

## Epic 5 — Wishes and delivery

| ID | Story | Priority | Implementation |
| --- | --- | --- | --- |
| US-050 | As a family member, I want a personal message written for me on my birthday, so that it does not feel automated. | M | `BirthdayMailScheduler` + `AiService.generate` |
| US-051 | As a family member, I want a good-morning and good-night note if I opted in, so that my day starts and ends warmly. | S | `GoodMorningScheduler`, `GoodNightScheduler` |
| US-052 | As a family member, I want a card image in the email, so that it looks like a greeting card. | S | `AiService.callGeminiImage` + `WishImageService` |
| US-053 | As an administrator, I want to upload images per person and event type, so that some cards are hand-picked. | S | `/api/admin/wish-images` |
| US-054 | As an administrator, I want to see every email's status and body, so that I can prove what was sent. | M | `GET /api/emails/status`, `/{id}` |
| US-055 | As an administrator, I want to retry failed emails, so that a transient failure is not a lost wish. | M | `retryFailed` (scheduler + service flows) |
| US-056 | As an administrator, I want to send a one-off wish immediately, so that I can handle a special case. | C | `POST /api/emails/send-now` |
| US-057 | As a family member, I want my OTP and reset emails to stay private, so that nobody else can read my codes. | M | Sensitive type filter in `GmailEmailServiceImpl` |
| US-058 | As an administrator, I want an agent to triage delivery failures, so that I do not read logs every hour. | C | `DeliveryOpsAgent`, `POST /api/agent/delivery-ops/run` |
| US-059 | As an administrator, I want to ask questions in plain English about delivery and logins, so that I get answers without SQL. | C | `AdminCopilotAgent`, `POST /api/agent/admin/ask` |

## Epic 6 — Circles

| ID | Story | Priority | Implementation |
| --- | --- | --- | --- |
| US-060 | As a family member, I want to invite someone to my circle, so that we share dates and chat. | M | `POST /api/circles/requests` |
| US-061 | As an invited member, I want a one-click link in the email, so that accepting takes no password. | S | `CircleRequestNotifier` + `POST /api/auth/direct-login` |
| US-062 | As an invited member, I want to accept or reject a request, so that I control who sees my dates. | M | `POST /api/circles/requests/accept`, `/reject` |
| US-063 | As an inviter, I want to know when my request was accepted, so that I can start chatting. | S | `notifyRequestAccepted` |
| US-064 | As an inviter, I want a reminder to reach someone who ignores my request, so that it does not go stale. | C | `CircleRequestReminderScheduler` |
| US-065 | As a family member, I want to leave a circle, so that I can withdraw access. | M | `DELETE /api/circles` |
| US-066 | As a family member, I want to see whether someone is in my circle, so that the UI can show the right action. | M | `GET /api/circles/status/{userId}` |

## Epic 7 — Chat

| ID | Story | Priority | Implementation |
| --- | --- | --- | --- |
| US-070 | As a family member, I want to chat only with my circle by default, so that strangers cannot message me. | M | `ensureCanMessage`, `app.chat.circles-only` |
| US-071 | As a family member, I want to send text, voice notes and files, so that I can share anything. | M | `POST /api/chat` (multipart) |
| US-072 | As a family member, I want replies and reactions, so that conversations make sense. | S | `replyToMessageId`, `/reactions`, `/like` |
| US-073 | As a family member, I want to edit and delete messages, so that I can fix mistakes. | S | `PATCH /api/chat/messages/{id}`, `DELETE` variants |
| US-074 | As a family member, I want to see who is online and when they were last seen, so that I know when to expect a reply. | S | `/presence/heartbeat`, `/presence/offline`, `lastSeenAt` |
| US-075 | As a family member, I want unread counts, so that I do not miss a message. | M | `GET /api/chat/unread-count`, `unreadChatMessagesCount` in `AuthResponse` |
| US-076 | As a family member, I want an email if I have unread messages overnight, so that I come back. | C | `UnreadChatEmailScheduler` |
| US-077 | As a family member, I want messages to appear instantly, so that chat feels live. | S | STOMP `/app/chat.send`, `/topic/chat/{conversationId}` |

## Epic 8 — Announcements

| ID | Story | Priority | Implementation |
| --- | --- | --- | --- |
| US-080 | As an administrator, I want to publish an announcement, so that everyone sees it. | S | `POST /api/notifications/{id}/publish` |
| US-081 | As an administrator, I want to schedule and expire an announcement, so that it appears at the right time. | C | `scheduledFrom`, `scheduledTo` |
| US-082 | As an administrator, I want to email an announcement, so that it reaches people who are not in the app. | C | `canSendEmail` |
| US-083 | As a family member, I want to read the current announcement, so that I am informed. | S | `GET /api/notifications/published` |

## Epic 9 — Insight and operations

| ID | Story | Priority | Implementation |
| --- | --- | --- | --- |
| US-090 | As an administrator, I want counters and charts of mail and Instagram activity, so that I know the system is working. | S | `/api/dashboard/*` |
| US-091 | As an administrator, I want to see where sign-ins came from on a map, so that I can spot unusual access. | C | `GET /api/dashboard/chart/login-locations` |
| US-092 | As an administrator, I want to see failed sign-in patterns by IP and email, so that I can act on abuse. | S | `GET /api/dashboard/violated-users` |
| US-093 | As an administrator, I want scheduler health and a manual trigger, so that I can recover a missed run. | S | `/api/schedulers` |
| US-094 | As an operator, I want a health endpoint, so that monitoring can alert me. | M | `GET /health` |
| US-095 | As an integrator, I want OpenAPI documentation, so that the UI team can work independently. | M | `/v3/api-docs`, `/swagger-ui.html` |

## Story-to-requirement map

| Epic | Requirements |
| --- | --- |
| 1 | FR-AUTH-01…18 |
| 2 | FR-USER-01…10, FR-THEME-01…03 |
| 3 | FR-EVT-01…07, FR-CAL-01…06 |
| 4 | FR-FEST-01…08 |
| 5 | FR-WISH-01…14, FR-MEDIA-01…05, FR-SEED-03…04 |
| 6 | FR-CIR-01…10 |
| 7 | FR-CHAT-01…09 |
| 8 | FR-NOTIF-01…05 |
| 9 | FR-DASH-01…07, FR-OPS-01…05 |
