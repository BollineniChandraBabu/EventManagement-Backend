# Business requirements

## 1. Problem statements

| # | Problem | Who is affected | Current workaround |
| --- | --- | --- | --- |
| P1 | Family dates (birthdays, anniversaries, festivals) are missed or remembered late | Every family member | Phone reminders, memory, chat-group nudges |
| P2 | Greetings are generic and repeated; nobody personalises dozens of messages | The person sending wishes | Forwarded images and one-line texts |
| P3 | Sending wishes is manual and does not scale with family size | The family organiser | Spreadsheets, calendar entries, manual emails |
| P4 | There is no private space for the family to share dates and talk | Whole family | Public social networks and group chats with unclear membership |
| P5 | Nobody knows whether a wish was actually delivered | Organiser | None — delivery failures are silent |
| P6 | Suspicious sign-in attempts go unnoticed | Account owners, administrator | None |
| P7 | Admin work (adding a person, mapping a festival, uploading a card) needs code changes | Administrator | Manual database edits |

## 2. Business goals

| # | Goal | Rationale | Success indicator |
| --- | --- | --- | --- |
| G1 | Automate date-triggered greetings end to end | Remove P1 and P3 | Wish emails generated and sent on the correct IST day with a `SENT` log row |
| G2 | Personalise every greeting | Remove P2 | Generated subject/body contains the recipient's name and relationship; no placeholders; validator rejects duplicates of recent subjects |
| G3 | Create a private family space | Remove P4 | Calendar, circles and chat visible only to the owner, accepted circle members and admins |
| G4 | Make delivery observable | Remove P5 | Email log with type/status/retry/error; status API and dashboards; retry job |
| G5 | Make the system administrable without code changes | Remove P7 | CRUD APIs for users, events, seeds, wish images, festival mappings, notifications |
| G6 | Surface security events | Remove P6 | Failed-login tracking, account lock after 5 failures, alert emails, login-location analytics |
| G7 | Keep operating cost predictable | Business constraint | Free/low-tier providers; single container; bounded caches; capped retries and AI calls |

## 3. Stakeholders

| Stakeholder | Interest |
| --- | --- |
| Family organiser (primary admin) | Reliable delivery, easy administration, proof of what was sent |
| Family members (users) | Correct dates on their calendar, personal messages, private chat |
| Recipients of wishes | A well-formed, personal email that renders correctly |
| Administrator / support | Dashboards, logs, retry, scheduler control |
| Owner of the deployment | Cost, security, uptime |

## 4. Business rules that drive requirements

These are the non-negotiable rules the business has set. They are implemented as described in
[../backend/business-logic.md](../backend/business-logic.md).

| ID | Rule |
| --- | --- |
| BR-01 | One wish per occasion per day: a birthday wish is sent at most once per user per day; an event wish at most once per event per day; a festival mapping at most once per day unless the previous attempt failed |
| BR-02 | Every date is evaluated in **Asia/Kolkata** |
| BR-03 | A greeting is sent even when AI image generation fails; text failure is logged and reported, never silently dropped |
| BR-04 | A user receives a birthday wish only when their own birthday switch is on; good-morning and good-night wishes are per-user switches too |
| BR-05 | A circle must be **accepted** before calendar sharing or chat is allowed between two users |
| BR-06 | Only the addressee can accept or reject a circle request |
| BR-07 | OTP and password-reset emails are sensitive: non-admin users can never read them |
| BR-08 | Five consecutive failed sign-ins lock password sign-in until an administrator intervenes |
| BR-09 | Custom wishes links must be `http`/`https` URLs of at most 500 characters; anything else is rejected |
| BR-10 | Uploaded media is validated by content, not by file name |
| BR-11 | Only one notification is published at a time |
| BR-12 | Admin impersonation cannot target another admin and cannot be used to change the target's password |

## 5. Expected outcomes

| Outcome | Observable result |
| --- | --- |
| Zero missed family dates | Daily jobs run at 06:00, 06:10, 06:30, 07:30 and 22:00 IST; each run leaves log rows |
| Personal, non-repetitive messages | Agentic composer (when enabled) reads the recipient profile and recent wishes, and must pass `submit_wish` validation |
| Private by default | Chat blocked unless both users are in an accepted circle (`app.chat.circles-only=true`) |
| Full delivery transparency | `GET /api/emails/status` shows type, status, retry count; `GET /api/emails/status/{id}/image` serves the picture; failed sends retried up to 3 times |
| Fast administration | All seed tables and content are editable through admin APIs, no redeploy needed |
| Security visibility | Failed sign-ins recorded in `violated_users`; threshold alerts emailed; login locations mapped |
| Controlled spend | AI calls bounded by model-fallback retries; email retries capped; caches bounded by entry count and TTL |

## 6. Assumptions

1. One deployment serves one family group; there is no tenant isolation.
2. An administrator creates all accounts; there is no public sign-up.
3. The Gmail account that sends mail is a dedicated account with an OAuth refresh token kept valid.
4. Calendarific covers Indian holidays only (`calendarific.country=IN` by default).
5. Recipients have valid email addresses; the system does not verify deliverability.
6. The UI is deployed separately and consumes the JSON API with a bearer token.
7. Internet access is available from the runtime to Google, Pollinations, Calendarific, ip-api.com
   and Supabase.

## 7. Constraints

| Type | Constraint |
| --- | --- |
| Technical | Spring Boot 3.3 / Java 17 (toolchain pinned); PostgreSQL only; Render free/low tier container |
| Provider | Gmail sending quota; Gemini rate limits; Pollinations rate limits; Calendarific free plan; ip-api.com 45 requests/minute; Supabase storage/DB quotas |
| Operational | Single region; no blue-green infrastructure; schema managed by Hibernate `ddl-auto=update` |
| Legal | Personal data of family members; emails and chat content must remain private to the deployment |

## 8. Out of scope

Payments, public registration, native apps, multi-language UI (emails are generated in English,
`language=EN`), SMS/WhatsApp delivery, calendar sync (ICS/Google Calendar), and analytics beyond the
built-in dashboards.

## 9. Related documents

* [functional-requirements.md](functional-requirements.md)
* [non-functional-requirements.md](non-functional-requirements.md)
* [acceptance-criteria.md](acceptance-criteria.md)
* [../backend/business-logic.md](../backend/business-logic.md)
