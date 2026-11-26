# Project plan

## 1. Context

The backend is delivered as a working product (`1.0.0`) and is now maintained. This plan covers
maintenance, the known-issue backlog, and the technical improvements identified in the documentation.
It is deliberately lightweight: one developer, one family group, on-demand releases.

## 2. Objectives for the next cycle

| # | Objective | Measure of success |
| --- | --- | --- |
| O1 | Remove the highest security risk | `violated_users.password` no longer stores clear text |
| O2 | Close the access-control gaps | Scheduler trigger, `/actuator`, diagnostic endpoints, `GET /api/users/{id}` restricted |
| O3 | Make delivery observable | Scheduler-failure alerting and a readiness endpoint |
| O4 | Protect against regressions | CI with tests, plus scheduler and integration tests |
| O5 | Make schema changes safe | Flyway baseline and versioned migrations |
| O6 | Improve chat security | Authenticated STOMP handshake |

## 3. Phases

| Phase | Duration | Content | Exit |
| --- | --- | --- | --- |
| P0 — Stabilise | 1 week | Security fixes O1, O2 | Deployed, verified |
| P1 — Observe | 1–2 weeks | O3, metrics basics | Alerts fire correctly |
| P2 — Protect | 2 weeks | O4 (CI, tests) | Pipeline green on every change |
| P3 — Evolve | 2–3 weeks | O5, O6 | Migrations in place, chat authenticated |
| P4 — Polish | Ongoing | Documentation, cleanup backlog | Backlog shrinking |

## 4. Milestones

| Milestone | Content | Target |
| --- | --- | --- |
| M1 | Security patch release `1.0.1` | Week 1 |
| M2 | Observability release `1.1.0` | Week 3 |
| M3 | CI and test coverage in place | Week 5 |
| M4 | Flyway baseline release `1.2.0` | Week 8 |
| M5 | Chat security release `1.3.0` | Week 10 |

## 5. Activities

| Activity | Phase | Owner |
| --- | --- | --- |
| Drop or hash `violated_users.password` | P0 | Backend owner |
| Restrict actuator and diagnostic endpoints | P0 | Backend owner |
| Add `@PreAuthorize` to scheduler controller | P0 | Backend owner |
| Restrict `GET /api/users/{id}` | P0 | Backend owner |
| Add `/health/ready` | P1 | Backend owner |
| Alert on scheduler failure | P1 | Operator |
| Add CI workflow | P2 | Backend owner |
| Add scheduler and integration tests | P2 | Backend owner |
| Add Flyway baseline | P3 | Backend owner |
| Authenticate STOMP | P3 | Backend owner |
| Cleanup backlog (Thymeleaf, `System.out`, email classification) | P4 | Backend owner |

## 6. Dependencies

| Dependency | Needed for |
| --- | --- |
| Provider access (Google, Supabase, Neon, Render) | All releases |
| A test mailbox | Email verification |
| A QA database (or a scratch branch) | Integration tests and restore drills |
| Time from the operator | Deployment and monitoring |

## 7. Risks (summary)

| Risk | Mitigation |
| --- | --- |
| A security fix breaks sign-in | Test on a scratch environment first; keep the rollback plan ready |
| Provider quotas limit testing | Use unit tests with fakes |
| Single developer | Keep changes small and documented |
| Schema change risk | Backups plus a staged rollout |

Full register: [risk-register.md](risk-register.md).

## 8. Reporting

| Report | Frequency | Content |
| --- | --- | --- |
| Status note | Weekly | What moved, what is blocked |
| Release note | Per release | Features, fixes, known issues |
| Incident review | Per incident | Timeline and actions |

## 9. Definition of done (per item)

1. Code merged, tests green.
2. Deployed and smoke-tested.
3. Documentation updated (API, configuration, business logic, known issues).
4. Changelog entry added.
