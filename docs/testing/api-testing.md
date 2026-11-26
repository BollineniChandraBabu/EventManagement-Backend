# API testing

## 1. Approach

API testing is manual and script-driven against a running instance, because the contract is already
described by springdoc (`/v3/api-docs`) and by
[../backend/api-specification.md](../backend/api-specification.md). Each change to a controller is
verified with `curl` or Postman before merge.

## 2. Prerequisites

1. The application is running (`./gradlew bootRun` or the container).
2. Test data exists ([test-data.md](test-data.md)).
3. Two tokens are available: an admin token and a member token.

```bash
BASE=http://localhost:8080
ADMIN=$(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"Test"}' | jq -r .accessToken)
MEMBER=$(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"Test"}' | jq -r .accessToken)
```

## 3. Contract checks (every endpoint group)

| Check | How | Pass condition |
| --- | --- | --- |
| Path and verb | Call the documented path with the documented verb | Not 404/405 |
| Auth required | Call without `Authorization` | 401 or 403 (never 200) |
| Role required | Call an admin endpoint with a member token | 403 |
| Validation | Send a request missing a required field | 400 with a field message |
| Happy path | Send a valid request | 200/201 with the documented shape |
| Error shape | Trigger a business error | `{timestamp,status,message}` |
| Pagination | Request `page=0&size=5` | `PagedResponse` fields present and consistent |
| Sorting | `sortBy`/`sortDir` | Order changes accordingly |
| Filter values | Call `filter-values` | Returns the documented keys |
| OpenAPI parity | Compare with `/v3/api-docs` | No undocumented endpoint, no missing schema |

## 4. Smoke suite (10 minutes)

| # | Call | Expect |
| --- | --- | --- |
| 1 | `GET /health` | 200 `UP` |
| 2 | `POST /api/auth/login` (admin) | 200 tokens |
| 3 | `GET /api/users?page=0&size=5` (admin) | 200 paged |
| 4 | `GET /api/users/directory` (member) | 200, reduced fields |
| 5 | `GET /api/calendar?month=<current>` | 200 entries |
| 6 | `GET /api/festivals?month=<current>` | 200 paged |
| 7 | `GET /api/events?page=0&size=5` | 200 own events |
| 8 | `GET /api/circles` | 200 three lists |
| 9 | `GET /api/chat/unread-count` | 200 `{unreadCount}` |
| 10 | `GET /api/notifications/published` (no token) | 200 or 204 |
| 11 | `GET /api/emails/status?page=0&size=5` | 200 paged |
| 12 | `GET /api/dashboard/mail` | 200 counters |
| 13 | `GET /api/themes` | 200 list |
| 14 | `GET /api/schedulers` (admin) | 200 nine jobs |
| 15 | `GET /v3/api-docs` | 200 JSON |

## 5. Negative and boundary suite

| Case | Call | Expect |
| --- | --- | --- |
| No token | `GET /api/users/me` | 401 |
| Bad token | `Authorization: Bearer junk` | 401 |
| Expired token | Use an old access token | 401 |
| Member on admin endpoint | `POST /api/users` | 403 |
| Missing body field | `POST /api/events` without `eventType` | 400 |
| Invalid enum | `relationShip="COUSIN"` | 400/404 |
| Unknown id | `GET /api/events/999999` | 404 |
| Oversized page | `GET /api/users?size=100000` | Capped or 400 (documented behaviour) |
| Malformed JSON | Broken body | 400 |
| Wrong content type | JSON sent as text | 415 |
| Too-large upload | 60 MB file | 413 |
| Rate limit | 11 logins | 429 |

## 6. Group-by-group verification notes

| Group | Watch for |
| --- | --- |
| Auth | `expiresIn=900`, `unreadChatMessagesCount` present, `MFA_OTP_REQUIRED`, `CONCURRENT_LOGIN:` prefix |
| Users | Directory must not leak role/birthday; `PATCH /{id}/concurrent-login` 403 for a stranger |
| Events | `eventType` is the seed code in responses; stranger read is 404 not 403 |
| Calendar | `fromCircle` and `ownerUserId` set only for circle entries; `recurring` semantics |
| Festivals | `month` is required; `year` triggers on-demand fill |
| Mappings | Upsert does not duplicate; bulk returns counts |
| Circles | Direction flips on mutual request; accept by a stranger is 400 |
| Chat | Multipart `payload` part is JSON; attachment part optional; `409`/`400` on gate failures |
| Notifications | Past schedule is 400 and changes nothing; only one published |
| Emails | Sensitive types are admin-only; `hasImage` instead of base64 |
| Dashboards | Date validation messages; `size` bounds |
| Themes | Unknown code is 400 |
| Seeds | Non-admin writes are 403 |
| Wish images | `201` on create, `204` on delete |
| Agents | `503` when disabled, `409` when a run is in progress |
| Schedulers | Unknown job is 404; trigger returns `triggeredAt` |

## 7. Contract testing (recommended)

1. Save `/v3/api-docs` from the current release as a baseline.
2. After a change, diff the JSON; any removed field or changed type is a breaking change.
3. Add a CI step that fetches `/v3/api-docs` and compares it with the baseline.
4. For the UI, generate a typed client from the OpenAPI document and fail the UI build on a breaking
   change.

## 8. Automation options

| Option | Effort | Value |
| --- | --- | --- |
| Postman collection + Newman | Low | Runs the smoke and negative suites in CI |
| REST Assured + JUnit | Medium | Same assertions inside the build |
| OpenAPI diff in CI | Low | Catches accidental contract breaks |
| Pact-style consumer tests | High | Overkill for one consumer team |

## 9. Recording results

For each release, record:

| Field | Content |
| --- | --- |
| Release | version and commit |
| Date / tester | |
| Endpoint groups run | checkboxes |
| Failures | case id + status + body |
| Defects raised | IDs from [defect-tracking.md](defect-tracking.md) |
| Verdict | pass / pass with known issues / fail |
