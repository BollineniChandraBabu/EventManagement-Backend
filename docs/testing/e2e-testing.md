# End-to-end testing

## 1. Scope

E2E runs exercise the real system (API, database, storage, mail) through the workflows a family
member performs. There is no browser automation in this repository; the UI is tested separately. The
E2E suite here is an API-level journey that produces real emails.

## 2. Environment

| Item | Requirement |
| --- | --- |
| Application | Running with real properties (not a mocked profile) |
| Database | Dedicated E2E database, not production |
| Mailbox | Dedicated address you can read |
| Storage | Dedicated bucket |
| Providers | Gemini, Pollinations and Calendarific keys valid |
| Schedulers | Running (needed for wish flows) |

## 3. Journeys

### J1 — New member receives a birthday wish

| Step | Action | Expected |
| --- | --- | --- |
| 1 | Admin creates `alice.e2e@example.com` with birthday = today | 200 |
| 2 | Admin creates a recurring event for Alice dated today | 200 |
| 3 | Admin uploads a wish image for Alice + `Birthday` | 201 |
| 4 | Trigger `birthdayJob` | Job completes |
| 5 | Check the mailbox | One birthday email with the uploaded image and the signature |
| 6 | `GET /api/emails/status?mailTab=BIRTHDAY` as admin | The row is `SENT` with `hasImage=true` |
| 7 | Trigger `birthdayJob` again | No second email (`last_birthday_wish_sent`) |

### J2 — Sign-in, calendar and preferences

| Step | Action | Expected |
| --- | --- | --- |
| 1 | Sign in as Alice | 200 tokens |
| 2 | `GET /api/calendar?month=<current>` | Birthday + event + festivals |
| 3 | PATCH wish settings to disable good night | 200 |
| 4 | Sign out by dropping the token, sign in again | Settings persist |

### J3 — Circle, chat and shared calendar

| Step | Action | Expected |
| --- | --- | --- |
| 1 | Alice sends a circle request to Bob | `PENDING`; Bob receives a link |
| 2 | Bob opens the link and calls `direct-login` with the token | 200 tokens |
| 3 | Bob accepts the request | `ACCEPTED`; Alice receives a confirmation email |
| 4 | Alice reads her calendar | Bob's birthday appears with `fromCircle=true` |
| 5 | Bob sends a text message and a voice note | Both stored; Alice sees them |
| 6 | Alice marks the conversation seen | Unread count returns to 0 |
| 7 | Alice removes the circle | Chat is blocked again |

### J4 — Festival wish

| Step | Action | Expected |
| --- | --- | --- |
| 1 | Ensure a festival exists for today (sync or seed) | Row present |
| 2 | Admin maps it to Alice with a personal note | 200 |
| 3 | Trigger `festivalWishScheduler` | Email sent with the note |
| 4 | Trigger again | No duplicate |
| 5 | Break the mail credentials, trigger again on a new day | `error=true`; the retry on the same day re-attempts |

### J5 — Announcement

| Step | Action | Expected |
| --- | --- | --- |
| 1 | Admin creates a notification with `canSendEmail=true` | 200 |
| 2 | Publish | All active users receive the email; the public endpoint returns it |
| 3 | Publish a second one | The first disappears from `/published` |

### J6 — Profile and media

| Step | Action | Expected |
| --- | --- | --- |
| 1 | Alice uploads a profile picture | URL returned; visible in the directory and chat |
| 2 | Alice uploads a PDF as a picture | 400 |
| 3 | Alice removes the picture | Default picture returned |

### J7 — Security journey

| Step | Action | Expected |
| --- | --- | --- |
| 1 | Alice fails the password 5 times | Lockout message + security email |
| 2 | Admin resets via the API or the user resets via email | Access restored |
| 3 | Dave (single-session) signs in on two clients | The first is rejected with `CONCURRENT_LOGIN` |

## 4. Data cleanup after a run

1. Delete created users (soft delete) and their events.
2. Delete uploaded objects from the E2E bucket.
3. Delete `email_logs` rows for the test addresses (optional).
4. Delete chat rows for the test conversation.

## 5. Pass criteria

* Every step matches the expectation.
* No unexpected 5xx.
* Emails arrive within 2 minutes.
* No duplicate wishes.
* Logs contain no stack traces for the journeys.

## 6. Failure triage

| Symptom | First check |
| --- | --- |
| No email | `email_logs.error_message`, Gmail token validity |
| Wrong image | Wish image ordering (user vs global), event type spelling |
| No festival rows | `CALENDARIFIC_API_KEY`, sync logs |
| Circle email missing | Gmail failure logged; the request itself still succeeds |
| Chat blocked after acceptance | Circle status, `app.chat.circles-only` |
| Duplicate wish | `last_birthday_wish_sent`, `last_wish_sent_on`, second instance |

## 7. Automation roadmap

1. Script the journeys with `bash` + `jq` (fastest path).
2. Move them into REST Assured tests behind a profile that allows real email.
3. Add a mailbox reader (IMAP) to assert content automatically.
4. Add a Playwright suite in the UI repository for the same journeys.
