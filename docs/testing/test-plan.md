# Test plan

## 1. Purpose

Define what is tested, by whom, when, and with which data for release `1.0.0` and subsequent
increments.

## 2. Scope

| Included | Excluded |
| --- | --- |
| Unit tests for all services, filters, utilities and agents | Angular UI component tests |
| Manual API verification of every endpoint group | Provider SLA testing |
| End-to-end workflows (sign-in, calendar, circle, chat, wish delivery) | Penetration testing by a third party (recommended, not scheduled) |
| Regression of the scheduler jobs | Native mobile clients (none) |
| Security probes (auth, roles, uploads, rate limits) | Localisation testing |

## 3. Test items and priority

| Item | Priority | Approach |
| --- | --- | --- |
| Authentication (password, OTP, SSO, direct login, refresh) | 1 | Unit + manual |
| Single-session enforcement | 1 | Unit + manual (two browsers) |
| Wish scheduling and duplicate protection | 1 | Unit + manual trigger with a seeded date |
| Email send, logging, retry, visibility | 1 | Unit + manual (test mailbox) |
| Circle request/accept and chat gate | 1 | Unit + E2E |
| Calendar aggregation and circle sharing | 2 | Unit + manual |
| Festivals sync and on-demand years | 2 | Manual with a real key |
| Media validation and gzip | 2 | Unit + manual uploads |
| Notifications publish/schedule | 3 | Unit + manual |
| Dashboards and analytics | 3 | Manual |
| Themes and seed administration | 4 | Manual |
| Agents (composer, delivery-ops, copilot) | 3 | Unit + manual with flags on |

## 4. Test approach per level

| Level | Owner | Frequency | Environment |
| --- | --- | --- | --- |
| Unit | Developer | Every commit | Local, no network |
| API | Developer / QA | Every change to a controller | Local or QA |
| E2E | QA / release owner | Before each release | QA/UAT with a dedicated mailbox |
| Performance | Release owner | Before a major release | QA |
| Security | Developer / release owner | Every release | QA |

## 5. Schedule (per release)

| Day | Activity |
| --- | --- |
| D-5 | Feature freeze for the release branch; unit tests green |
| D-4 | API verification of changed endpoints |
| D-3 | E2E run on QA/UAT (sign-in, calendar, circle, chat, wish) |
| D-2 | Security probes, performance smoke |
| D-1 | Regression of scheduler jobs, backup taken, release notes drafted |
| D0 | Deploy, production smoke (`/health`, sign-in, one manual wish) |

## 6. Entry criteria

1. Code merged to the release branch and compiles.
2. `./gradlew test` green.
3. Test data prepared ([test-data.md](test-data.md)).
4. Environment variables configured for the test environment.
5. Known issues reviewed so testers do not re-report them.

## 7. Exit criteria

1. All priority 1 and 2 test cases pass.
2. No open blocker or critical defect.
3. Coverage of changed classes is at least as good as before the change.
4. Regression suite green.
5. Release notes and changelog updated.

## 8. Roles and responsibilities

| Role | Responsibility |
| --- | --- |
| Developer | Unit tests, API checks for own change, fixes |
| Release owner | E2E, performance and security runs, go/no-go |
| Administrator (product) | Accepts business behaviour, provides test accounts |
| Operator | Environment, secrets, backups, monitoring |

## 9. Environment and data

| Environment | Database | Mailbox | Storage |
| --- | --- | --- | --- |
| Local | Local PostgreSQL | Real Gmail (careful) or a test account | Test bucket |
| QA/UAT | Dedicated database | Dedicated mailbox | Dedicated bucket |
| Production | Neon | Production mailbox | Production bucket |

Test accounts and seeding are described in [test-data.md](test-data.md).

## 10. Risk and mitigation in the plan

| Risk | Mitigation |
| --- | --- |
| A test run sends real emails | Use a dedicated mailbox; set `app.agent.*` flags carefully; prefer triggering with test users only |
| Local run starts schedulers against shared data | Use a separate database or stop the app after manual checks |
| Provider quotas exhausted during testing | Use the AI endpoint sparingly; rely on unit tests with fakes |
| Test data drifts | Recreate from the seed script in [test-data.md](test-data.md) |

## 11. Deliverables

1. This plan and the case catalogue ([test-cases.md](test-cases.md)).
2. Execution record per release (pass/fail per case, defects raised).
3. Coverage summary ([test-coverage-report.md](test-coverage-report.md)).
4. Defect log ([defect-tracking.md](defect-tracking.md)).
5. Release sign-off in [../project/release-notes.md](../project/release-notes.md).

## 12. Suspension and resumption

Suspend testing when: the environment is unavailable for more than a day, more than 20% of priority
1 cases fail for the same root cause, or a data-loss defect is found. Resume after the fix is
deployed and a smoke test passes.
