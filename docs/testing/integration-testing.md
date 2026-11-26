# Integration testing

## 1. Current state

The repository has **no** Spring context integration tests, no `@DataJpaTest`, and no Testcontainers
setup. Integration is verified manually (see [api-testing.md](api-testing.md) and
[e2e-testing.md](e2e-testing.md)) plus a few "narrow" tests that use fakes instead of real providers:

| Test | Approach |
| --- | --- |
| `GeminiClientTest` with `FakeGeminiRestTemplate` | Real `GeminiClient` and `GeminiModelCatalog` with a fake HTTP layer |
| `GmailConfigTest` | Real credential/`Gmail` construction, including `invalid_grant` detection |
| `CacheConfigurationTest` | Real `ehcache.xml` parsed by the JCache provider |

## 2. What integration testing must cover

| Layer | Risk | How to test |
| --- | --- | --- |
| Spring wiring | Wrong bean chosen (`@Primary` AI service, `@Qualifier`), missing property | `@SpringBootTest` with a test profile |
| Security chain | Filter order, permit-all list, method security | `@SpringBootTest` + `MockMvc` with real filters |
| JPA queries | Month/day filters, circle pair lookup, paging, sorting | `@DataJpaTest` with H2 or Testcontainers PostgreSQL |
| Chat SQL | Upserts, `DISTINCT ON`, counters, `ALTER`-added columns | Testcontainers PostgreSQL |
| Storage | gzip round-trip, signed URL shape | Fake S3 (`LocalStack`) or a test bucket |
| Mail | MIME structure, inline images | Fake Gmail service or a test mailbox |
| Scheduler | Job registration, misfire policy, duplicate guards | `@SpringBootTest` with Quartz and a fixed clock |

## 3. Recommended setup

```groovy
testImplementation 'org.testcontainers:postgresql:1.20.1'
testImplementation 'org.testcontainers:junit-jupiter:1.20.1'
```

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {

  @Container
  static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", db::getJdbcUrl);
    registry.add("spring.datasource.username", db::getUsername);
    registry.add("spring.datasource.password", db::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    registry.add("spring.quartz.auto-startup", () -> "false");
  }
}
```

Disable scheduling and email in integration tests:

| Property | Value |
| --- | --- |
| `spring.quartz.auto-startup` | `false` |
| `app.agent.wish.enabled` | `false` |
| `app.agent.delivery-ops.enabled` | `false` |
| `app.agent.copilot.enabled` | `false` |
| `app.rate-limit.enabled` | `false` (unless testing limits) |

## 4. Priority integration scenarios

| # | Scenario | Assertion |
| --- | --- | --- |
| I-01 | Context loads with all required properties | No `BeanCreationException` |
| I-02 | `POST /api/auth/login` end to end | 200 with tokens; a `refresh_tokens` row exists |
| I-03 | Stale `sid` rejected | 401 with `CONCURRENT_LOGIN:` |
| I-04 | Rate limit | 11th request 429 |
| I-05 | Create user → create event → calendar read | The event appears in the calendar |
| I-06 | Circle request → accept → chat message | Message stored in the chat database |
| I-07 | Email send with a fake Gmail service | `email_logs` row `SENT`, image stored |
| I-08 | Festival sync with a stubbed HTTP layer | Rows upserted; removed row deactivated |
| I-09 | On-demand year fetch | One call, then database-only |
| I-10 | `ddl-auto=update` on an existing schema | No destructive change; app starts |
| I-11 | Chat schema initializer on an old schema | Missing columns added, indexes created |
| I-12 | Storage gzip round-trip | Uploaded bytes decompress to the original |
| I-13 | Scheduler job registration | Nine Quartz jobs with triggers |
| I-14 | Notification publish scheduling | Published at the scheduled time |

## 5. Test doubles

| Dependency | Double | Notes |
| --- | --- | --- |
| Gmail API | `@MockBean Gmail` or a local fake | Assert the encoded `raw` message |
| Gemini | `FakeGeminiRestTemplate` | Already used in unit tests |
| Pollinations | `MockRestServiceServer` on the `RestTemplate` | Return PNG bytes |
| Calendarific | `MockRestServiceServer` | Return a fixture JSON |
| ip-api.com | `MockRestServiceServer` | Return a success and a fail fixture |
| Supabase Storage | `LocalStack` S3 or a test bucket | Keep buckets separate |
| Chat database | Second Testcontainers instance | Separate JDBC URL |

## 6. Environment-specific checks

| Check | Method |
| --- | --- |
| Startup with missing `JWT_SECRET` | Expect failure with a clear message |
| Startup with an unreachable chat database | Chat endpoints fail, others work |
| `prod` profile loads | `--spring.profiles.active=prod` with test values |
| Time zone | Run with `TZ=UTC` and confirm jobs still fire in IST |

## 7. Gaps and plan

| Gap | Plan |
| --- | --- |
| No context test | Add one smoke test (highest value) |
| No repository tests | Add `@DataJpaTest` for `EventRepository`, `UserRepository`, `CircleRepository` |
| No chat SQL tests | Add Testcontainers tests for the upsert and unread queries |
| No storage test | Add a LocalStack test for gzip and signed URLs |
| No scheduler test | Add a test that invokes each job with seeded data and asserts side effects |
