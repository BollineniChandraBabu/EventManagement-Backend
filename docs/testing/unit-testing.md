# Unit testing

## 1. Framework and conventions

| Item | Value |
| --- | --- |
| Framework | JUnit 5 (Jupiter) |
| Mocking | Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`) |
| Assertions | AssertJ (`assertThat`) and JUnit (`assertEquals`, `assertThrows`) |
| Location | `src/test/java`, same package as the class under test |
| Naming | `<Class>Test`; method names describe behaviour |
| Execution | `./gradlew test` (Java 17 toolchain) |

There is **no Angular test suite in this repository**; UI unit tests belong to the UI repository. For
the UI, the same conventions apply with Jasmine/Karma or Jest, focusing on services that call this
API and on components that render calendar and chat data.

## 2. Current suite

| Metric | Value |
| --- | --- |
| Test classes | 30 |
| Test methods | 223 |
| Test code | ≈4 800 lines |
| Spring context tests | 0 |
| Container tests | 0 |

## 3. Test classes and what they pin down

| Class | Key behaviours asserted |
| --- | --- |
| `AuthServiceImplTest` | Successful login, wrong password, unknown email, lockout threshold, counter reset, MFA gate, OTP send/verify/reuse/expiry/resend cap, reset flow, change password, refresh, single-session rejection, impersonation, switch-back, direct login, SSO failures |
| `JwtServiceTest` | Access/refresh claims, TTLs, `sid`, `impersonatedBy`, invalid signature |
| `JwtAuthFilterTest` | Valid token passes, expired token rejected, stale `sid` produces `CONCURRENT_LOGIN` |
| `RateLimiterTest` | Limit reached, window rollover, key pruning |
| `UserServiceImplTest` | Create with hashed `Test`, relationship validation, wishes URL validation, update, settings |
| `UserDirectoryTest` | Reduced projection fields |
| `EventServiceImplTest` | Type resolution (case-insensitive), invalid type, ownership 404, image upload/delete |
| `CalendarServiceImplTest` | Festival/event/birthday mapping, circle merge, de-duplication, year scoping |
| `CircleServiceImplTest` | Self-request, duplicate, mutual accept, addressee-only response, rejection reuse, reminders |
| `CircleRequestNotifierTest` | Email content, link building, best-effort failure |
| `ChatServiceImplCircleGateTest` | Blocked without a circle, allowed with one |
| `FestivalWishMappingServiceImplTest` | Upsert, bulk counts, status filters |
| `SpecialEventSeedServiceImplTest` | Seed CRUD and deactivation |
| `AiServiceImplTest` | Prompt content, JSON parsing, fenced JSON, missing candidate, image fallback |
| `GeminiClientTest` | Fallback on 429/404/5xx, no retry on 400/401/403, cooldown, exhaustion → `AiUnavailableException` |
| `GeminiModelCatalogTest` | Filtering, exclusion regex, cache refresh |
| `GeminiEndpointTest` | URL parsing into base + model |
| `AgentRunnerTest` | Tool calls, unknown tool feedback, step cap, terminal tool acceptance |
| `GmailEmailServiceImplTest` | Send success/failure, classification, sensitive visibility, retry, image |
| `EmailImageServiceTest` | Cached load, content type |
| `MediaValidationTest` | Size limits per kind, declared/detected mismatch, empty file |
| `MediaContentTypesTest` | Signatures for each supported format |
| `GzipUtilTest` | Compress/decompress, idempotence, magic-number detection |
| `WishImageServiceImplTest` | User-first then global selection, event normalisation |
| `NotificationServiceImplTest` | Publish validation, unpublish previous, edit guard |
| `ThemeServiceImplTest` | Valid/invalid codes, persistence |
| `DashboardServiceImplTest` | Role scoping, counters, date validation |
| `CacheConfigurationTest` | Every `CacheNames` constant has a bounded cache with a TTL |
| `WishesUrlsTest` | Scheme, host, whitespace, user-info, length rules |
| `WishEmailExtrasTest` | Escaping, link insertion, note formatting |
| `WishImageKeysTest` | Key order for events and festivals |

## 4. Writing a new unit test

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {

  @Mock private UserRepository userRepository;
  @InjectMocks private MyServiceImpl service;

  @Test
  void rejectsUnknownUser() {
    when(userRepository.findByEmailAndDeletedFalse("x@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.doThing("x@example.com"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("User not found");
  }
}
```

Rules:

1. Mock only collaborators, never the class under test.
2. Verify side effects on collaborators (`verify(repo).save(...)`).
3. Do not assert log output.
4. Use `ArgumentCaptor` to inspect saved entities.
5. Prefer `assertThatThrownBy` over try/catch.

## 5. Testing time-dependent code

`LocalDateTime.now()` is used directly in several services. To test it:

* Pass explicit dates where the API allows it (event dates, `last_birthday_wish_sent`).
* For schedulers, extract the pure part (filtering) and test that, not the clock.
* Where a clock must be controlled, refactor to inject a `Clock` (recommended improvement).

## 6. What unit tests cannot prove

* Spring wiring (bean names, `@Qualifier` choices, filter order).
* Repository queries (JPQL correctness, month/day filters).
* Serialisation of DTOs (JSON field names).
* Migration behaviour (`ddl-auto=update`).

Those need the integration layer described in [integration-testing.md](integration-testing.md).

## 7. Coverage expectations

| Area | Expectation |
| --- | --- |
| New service method | At least one happy path and one failure path |
| New filter/security rule | Both allow and deny cases |
| New utility | Boundary cases (empty, max length, wrong type) |
| Bug fix | A test that fails before the fix |

## 8. Running and reporting

```bash
./gradlew test                                   # run everything
./gradlew test --tests '*AuthServiceImplTest'    # one class
./gradlew test jacocoTestReport                  # only if JaCoCo is added
```

Test reports are written to `build/reports/tests/test/index.html`.
