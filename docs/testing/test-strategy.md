# Test strategy

## 1. Objectives

1. Prove the business rules in [../backend/business-logic.md](../backend/business-logic.md) hold,
   especially the ones that guard against duplicate or missing greetings.
2. Prove the security boundary: authentication, roles, ownership, rate limits, upload validation.
3. Prove integrations degrade as designed (AI, storage, mail, geolocation).
4. Keep the suite fast enough to run on every change (currently ~1 400 lines, 223 tests, no Spring
   context for most of it).

## 2. Scope

| In scope | Out of scope |
| --- | --- |
| All Java code in `src/main` | The Angular UI (separate repository) |
| Service logic, filters, utilities, agents | Provider-side correctness (Gmail, Gemini, Calendarific) |
| API contract shape (manual + contract checks) | Load testing of provider quotas |
| Configuration-driven behaviour | Browser compatibility |

## 3. Test levels

| Level | What | Tooling | Where |
| --- | --- | --- | --- |
| Unit | Services, filters, utilities, agents with mocked collaborators | JUnit 5, Mockito, AssertJ | `src/test/java` (33 classes) |
| Integration (narrow) | Classes that need a real collaborator shape but no container (for example `GeminiClient` with a fake `RestTemplate`) | JUnit 5 + hand-written fakes | `ai/FakeGeminiRestTemplate`, `GmailConfigTest` |
| API / contract | Endpoint paths, payloads, status codes | Manual `curl`/Postman, Swagger UI | [api-testing.md](api-testing.md) |
| End-to-end | Sign-in → calendar → circle → chat → wish email | Manual scripted run | [e2e-testing.md](e2e-testing.md) |
| Performance | Latency and provider behaviour under load | Manual (k6/JMeter or `curl` loops) | [performance-testing.md](performance-testing.md) |
| Security | Auth bypass, role escalation, upload abuse, rate limits | Manual + automated unit tests | [security-testing.md](security-testing.md) |

There is **no** `@SpringBootTest` context test, no Testcontainers, and no database integration test in
the repository. That is a deliberate gap recorded in
[test-coverage-report.md](test-coverage-report.md).

## 4. Test pyramid in this repository

```
        manual E2E (few)            e2e-testing.md
      manual API checks             api-testing.md
   unit tests with mocks (223)      src/test/java
```

## 5. Conventions

| Convention | Rule |
| --- | --- |
| Naming | `<Class>Test` in the same package under `src/test/java` |
| Method names | Descriptive sentences (`login_locksAccountAfterFiveFailures`) |
| Structure | Arrange / Act / Assert with blank-line separation |
| Mocking | Mockito for collaborators; never mock the class under test |
| Assertions | AssertJ (`assertThat`) or JUnit (`assertEquals`, `assertThrows`) |
| Fakes over mocks where behaviour matters | `FakeGeminiRestTemplate` simulates provider responses |
| No network | Tests must not call real providers |
| Determinism | Fixed clocks or injected values where time matters |

## 6. What is covered today (by area)

| Area | Test class | Focus |
| --- | --- | --- |
| Auth | `AuthServiceImplTest` (25 tests) | Login, lockout, OTP, reset, SSO, direct login, impersonation, single session |
| JWT | `JwtServiceTest`, `JwtAuthFilterTest` | Claims, expiry, `sid` enforcement |
| Rate limiting | `RateLimiterTest` | Window, pruning, limit |
| Users | `UserServiceImplTest`, `UserDirectoryTest` | CRUD, settings, directory projection |
| Events | `EventServiceImplTest` | Type resolution, ownership, image lifecycle |
| Calendar | `CalendarServiceImplTest` | Circle merge, year scoping, de-duplication |
| Circles | `CircleServiceImplTest`, `CircleRequestNotifierTest` | Direction handling, reminders, mail best effort |
| Chat | `ChatServiceImplCircleGateTest` | Circle gate |
| Festivals | `FestivalWishMappingServiceImplTest`, `SpecialEventSeedServiceImplTest` | Upsert, bulk, seeds |
| AI | `AiServiceImplTest`, `GeminiClientTest`, `GeminiModelCatalogTest`, `GeminiEndpointTest`, `AgentRunnerTest` | Prompt/parse, fallback, catalogue, tool loop |
| Email | `GmailEmailServiceImplTest`, `EmailImageServiceTest` | Send, classification, visibility, image load |
| Media | `MediaValidationTest`, `MediaContentTypesTest`, `GzipUtilTest`, `WishImageServiceImplTest` | Sniffing, limits, gzip, image matching |
| Notifications | `NotificationServiceImplTest` | Publish rules, scheduling |
| Themes | `ThemeServiceImplTest` | Validation and persistence |
| Dashboards | `DashboardServiceImplTest` | Scoping and counters |
| Cache | `CacheConfigurationTest` | Every cache bounded and declared |
| Utils | `WishesUrlsTest`, `WishEmailExtrasTest`, `WishImageKeysTest` | Link rules, escaping, key order |

## 7. Entry and exit criteria

**Entry:** code compiles; the change has a described behaviour; test data is available (see
[test-data.md](test-data.md)).

**Exit:** `./gradlew test` green; new behaviour has at least one test; no new high-severity defect;
manual API checks for changed endpoints pass; documentation updated.

## 8. Environments

| Environment | Purpose | Data |
| --- | --- | --- |
| Local | Unit tests and manual API checks | Local PostgreSQL + a test bucket |
| QA/UAT (optional) | End-to-end runs without touching production | Dedicated database and mailbox |
| Production | Read-only smoke checks after deploy | Real data |

> Never point a local run at the production database: the schedulers would start and send real email.

## 9. Risk-based priorities

| Risk | Priority | Mitigation |
| --- | --- | --- |
| Duplicate or missed wishes | Highest | Date-stamp logic, unit tests, manual trigger checks |
| Auth bypass or role escalation | Highest | Filter/role tests, manual probes |
| Email not sent (token revoked) | High | Failure logging, retry, alerting, manual smoke |
| Chat data leak | High | Circle gate test, participation checks |
| Upload abuse | Medium | Byte-sniffing tests, size limits |
| Festival dates wrong | Medium | Sync and on-demand checks per year |
| Cache staleness | Low | TTLs, eviction on write, cache config test |

## 10. Tooling roadmap

1. Add `@DataJpaTest` coverage for the custom repository queries (month filters, circle pair).
2. Add a `@SpringBootTest` smoke test with H2 or Testcontainers for context wiring.
3. Add contract tests (OpenAPI diff) so the UI cannot break silently.
4. Add a scheduler test that runs each job against an in-memory database with a fixed clock.
5. Add mutation testing (PIT) for the auth and wish-sending logic.
