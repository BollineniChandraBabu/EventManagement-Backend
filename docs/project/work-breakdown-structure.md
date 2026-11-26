# Work breakdown structure

## 1. Delivered (1.0.0)

| ID | Work package | Deliverable |
| --- | --- | --- |
| WBS-1 | Identity | Password, OTP, Google SSO, direct login, reset, MFA gate, single-session, impersonation, lockout, rate limiting |
| WBS-2 | People | User CRUD, directory, wish settings, profile pictures, themes |
| WBS-3 | Dates | Events with seeded types and recurrence, calendar aggregation |
| WBS-4 | Festivals | Calendarific sync, on-demand years, festival mappings, per-year seeds |
| WBS-5 | Wishes | AI text with model fallback, images, wishes links, personal notes, signature |
| WBS-6 | Delivery | Gmail transport, logging, retry, status APIs, per-type views |
| WBS-7 | Circles | Requests, reminders, one-click login, acceptance emails |
| WBS-8 | Chat | Conversations, attachments, replies, reactions, presence, unread digest, WebSocket |
| WBS-9 | Announcements | Create, schedule, publish, email fan-out, realtime |
| WBS-10 | Insight | Mail/Instagram dashboards, charts, OTP/reset stats, login map, failed-login analytics |
| WBS-11 | Agents | Wish composer, delivery ops, admin copilot |
| WBS-12 | Administration | Seed CRUD, wish images, scheduler status and trigger |
| WBS-13 | Platform | Health, OpenAPI, Docker, prod profile, Render deployment |
| WBS-14 | Quality | 223 unit tests, 33 test classes |
| WBS-15 | Documentation | This documentation set |

## 2. Planned (backlog)

### 2.1 Security (P0/P1)

| ID | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| WBS-S1 | Remove or hash `violated_users.password` | 0.5 day | — |
| WBS-S2 | Restrict `/actuator` exposure and permit rules | 0.5 day | — |
| WBS-S3 | Remove or protect the OAuth/Gmail diagnostic controller | 0.5 day | — |
| WBS-S4 | Add `@PreAuthorize("hasRole('ADMIN')")` to the scheduler controller | 0.1 day | — |
| WBS-S5 | Restrict `GET /api/users/{id}` to admin or self | 0.2 day | — |
| WBS-S6 | Authenticate the STOMP handshake | 2 days | UI coordination |
| WBS-S7 | Rotate refresh tokens on use | 1 day | — |
| WBS-S8 | Global rate limit for non-auth endpoints | 1 day | — |

### 2.2 Reliability and observability (P1)

| ID | Task | Estimate |
| --- | --- | --- |
| WBS-R1 | `/health/ready` checking both databases | 0.5 day |
| WBS-R2 | Micrometer metrics endpoint | 1 day |
| WBS-R3 | Alert on scheduler failure | 0.5 day |
| WBS-R4 | Alert when no wish email is sent on a day with birthdays | 1 day |
| WBS-R5 | Persist scheduler statistics | 1 day |
| WBS-R6 | Persist notification schedules | 1 day |

### 2.3 Quality (P2)

| ID | Task | Estimate |
| --- | --- | --- |
| WBS-Q1 | CI workflow with tests | 0.5 day |
| WBS-Q2 | JaCoCo coverage reporting | 0.5 day |
| WBS-Q3 | Scheduler unit tests (filtering logic) | 1 day |
| WBS-Q4 | `@SpringBootTest` smoke test | 1 day |
| WBS-Q5 | `@DataJpaTest` for the hottest repositories | 1 day |
| WBS-Q6 | Chat SQL tests with Testcontainers | 1 day |
| WBS-Q7 | OpenAPI contract diff in CI | 0.5 day |
| WBS-Q8 | gitleaks and Trivy in CI | 0.5 day |

### 2.4 Data (P3)

| ID | Task | Estimate |
| --- | --- | --- |
| WBS-D1 | Flyway baseline and first migration | 2 days |
| WBS-D2 | Switch `ddl-auto` to `validate` | 0.5 day |
| WBS-D3 | Retention job for security tables | 1 day |
| WBS-D4 | Admin "erase user" endpoint | 1 day |
| WBS-D5 | Orphaned storage sweep | 1 day |

### 2.5 Product (P4)

| ID | Task | Estimate |
| --- | --- | --- |
| WBS-P1 | Self-service account deletion | 1 day |
| WBS-P2 | Data export (JSON) | 1 day |
| WBS-P3 | Email delivery webhooks or bounce handling | 2 days |
| WBS-P4 | Per-user cap on pending circle requests | 0.5 day |
| WBS-P5 | Localisation of generated wishes | 2 days |

### 2.6 Cleanup (P4)

| ID | Task | Estimate |
| --- | --- | --- |
| WBS-C1 | Replace `System.out.println` with `log` | 0.2 day |
| WBS-C2 | Always pass `EmailType` | 0.5 day |
| WBS-C3 | Remove unused Thymeleaf dependency | 0.2 day |
| WBS-C4 | Consolidate the two email transports | 1 day |
| WBS-C5 | Add `.env` to `.gitignore` and a `.env.example` | 0.1 day |
| WBS-C6 | Dockerfile: wildcard jar copy, non-root user, healthcheck | 0.5 day |

## 3. Dependency graph (high level)

```
WBS-S1 ─┐
WBS-S2 ─┤
WBS-S3 ─┼─► release 1.0.1 (security)
WBS-S4 ─┤
WBS-S5 ─┘

WBS-R1 ─┐
WBS-R2 ─┼─► release 1.1.0 (observability)
WBS-R3 ─┘

WBS-Q1 ─► WBS-Q2 ─► WBS-Q3..Q8 ─► CI maturity

WBS-D1 ─► WBS-D2 ─► WBS-D3 ─► release 1.2.0

WBS-S6 ─► release 1.3.0
```

## 4. Effort summary

| Group | Estimate |
| --- | --- |
| Security | ~5.5 days |
| Reliability | ~5 days |
| Quality | ~5.5 days |
| Data | ~5.5 days |
| Product | ~5.5 days |
| Cleanup | ~2.5 days |
| **Total** | **~30 days** |
