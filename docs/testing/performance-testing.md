# Performance testing

## 1. Objectives

1. Confirm ordinary API calls stay fast for a family-sized user base.
2. Find the slow paths (AI generation, email sending, calendar aggregation, dashboards).
3. Confirm the system survives bursts (many calendar reads, many chat sends).
4. Confirm provider failure does not hang requests indefinitely.

## 2. Workload model

| User type | Count | Behaviour |
| --- | --- | --- |
| Members | 10–50 | A few calendar and chat calls per day |
| Admin | 1–3 | Frequent list views, occasional uploads |
| Schedulers | 9 jobs | Bursts at 06:00–07:30 and 22:00 |
| External | 1 | UptimeRobot every 5 minutes |

Realistic peak: 20 concurrent users plus a wish run.

## 3. Targets

| Endpoint class | p95 target | Notes |
| --- | --- | --- |
| `GET /health` | < 100 ms | No dependency checks |
| Auth (login, refresh) | < 800 ms | BCrypt cost dominates |
| List endpoints (users, events, emails) | < 500 ms | Paged, indexed |
| Calendar read | < 800 ms | Aggregates festivals + events + users + circles |
| Chat send (text) | < 600 ms | Includes storage only when an attachment exists |
| Chat conversation read | < 500 ms | Indexed by conversation + sent_at |
| Dashboards (cached) | < 200 ms | Cache hit |
| Dashboards (uncached) | < 2 s | Aggregations over `email_logs` |
| Upload (1 MB image) | < 2 s | Validation + gzip + storage |
| Wish generation (AI) | 3–15 s | Provider latency; not user-facing |
| Email send (Gmail) | 1–5 s | Async; not user-facing |

## 4. Scenarios

| ID | Scenario | Tool | Pass condition |
| --- | --- | --- | --- |
| P-01 | 20 virtual users read the calendar for 5 minutes | k6 | p95 < 800 ms, 0 errors |
| P-02 | 20 virtual users sign in | k6 | p95 < 800 ms; no 5xx (429 expected beyond 10/min per IP) |
| P-03 | 10 users send 100 chat messages | k6 | p95 < 600 ms; all persisted |
| P-04 | 5 admins page through email status | k6 | p95 < 500 ms |
| P-05 | Upload 20 × 1 MB images | k6 | No 5xx; storage objects created |
| P-06 | Trigger `dailyWishJob` with 50 events | Manual | Completes; all emails logged |
| P-07 | Gemini latency injection (5 s per call) | Manual | Wishes still complete; no request timeouts for users |
| P-08 | Gmail outage | Manual | Emails logged `FAILED`; API responses unaffected |
| P-09 | Chat database slow (500 ms) | Manual | Chat slow; other endpoints unaffected |

## 5. Example k6 script (calendar read)

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '5m',
  thresholds: { http_req_duration: ['p(95)<800'], http_req_failed: ['rate<0.01'] },
};

const BASE = __ENV.BASE;
const TOKEN = __ENV.TOKEN;

export default function () {
  const res = http.get(`${BASE}/api/calendar?month=11`, {
    headers: { Authorization: `Bearer ${TOKEN}` },
  });
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}
```

Run: `k6 run -e BASE=https://host -e TOKEN=… calendar.js`

## 6. What to watch during a run

| Signal | Where |
| --- | --- |
| Response times | k6 summary |
| 429 rate | k6 (`http_req_failed`) |
| Hikari pool saturation | Application logs (leak detection, timeouts) |
| Database CPU / connections | Neon dashboard |
| Chat pool saturation | Supabase dashboard |
| Provider latency | Logs (`Gemini model … failed`, Pollinations warnings) |
| Memory | Render metrics (email image cache is the main heap consumer) |

## 7. Known performance characteristics

| Area | Characteristic |
| --- | --- |
| BCrypt | Login cost is intentional; ~100–300 ms per hash |
| AI generation | 3–15 s per wish; schedulers run sequentially with a 5-thread pool |
| Calendar | Several queries plus in-memory merge; fine at family scale |
| Dashboards | Aggregate queries over `email_logs`; cached 60 s |
| Geolocation | One external call per uncached IP; capped at 300 per dashboard read |
| Chat | Plain SQL with indexes; `DISTINCT ON` for conversation summaries |
| Media | gzip costs CPU on upload; downloads decompress in memory |

## 8. Bottleneck hypotheses (ranked)

1. **AI provider latency** — dominates wish runs; mitigated by per-recipient isolation.
2. **Gmail send latency** — async, but a large run can exhaust the 5-thread pool; retries extend it.
3. **Dashboard aggregations** — mitigated by caching; a cold cache on a large table is slow.
4. **Calendar aggregation** — grows with circle size; fine for tens of members.
5. **BCrypt** — a burst of logins is CPU-bound; rate limiting also caps it.

## 9. Capacity estimate

| Resource | Estimate |
| --- | --- |
| Render instance (1 vCPU / 512 MB–2 GB) | Comfortable for 20–50 concurrent users |
| Database connections | 10 primary + 10 chat |
| Wish run of 50 events | 5–15 minutes wall clock (provider bound) |
| Storage per year | A few hundred MB with gzip |

## 10. Reporting

Record: scenario, virtual users, duration, p50/p95/p99, error rate, provider errors, database CPU,
and any threshold breach. Attach the k6 summary and note the commit tested.

## 11. Gaps

* No automated performance suite in CI.
* No baseline stored per release; add one to detect regressions.
* No load test of the STOMP chat path (only HTTP).
