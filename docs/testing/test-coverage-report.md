# Test coverage report

## 1. Snapshot

| Metric | Value | Source |
| --- | --- | --- |
| Test classes | 30 | `src/test/java` |
| Test methods | 223 | `@Test` annotations |
| Test code lines | ≈4 800 | line count of `src/test` |
| Production code | ≈250 classes | `src/main` |
| Line coverage | **Not measured** | No JaCoCo configured |
| Spring context tests | 0 | — |
| Database tests | 0 | — |
| UI tests in this repository | 0 | The UI lives in another repository |

> These figures are structural, not coverage percentages. No coverage tool is wired into the build,
> so treat coverage as "unknown — probably moderate in services, low in controllers".

## 2. Coverage by area (qualitative)

| Area | Automated | Manual | Assessment |
| --- | --- | --- | --- |
| Auth service | High | Medium | Strongest area; every sign-in path has tests |
| JWT and filters | High | Medium | Good |
| Rate limiter | High | Low | Good |
| Users | Medium | Medium | CRUD covered; the presigned upload path is thin |
| Events | Medium | Medium | Type, ownership and image covered |
| Calendar | Medium | Medium | Merge logic covered |
| Circles | Medium | Medium | Direction, response and reminders covered |
| Chat | Low | High | Only the circle gate is unit tested; SQL paths are manual |
| Festivals | Medium | Medium | Upsert and bulk covered; on-demand year is manual |
| AI text | Medium | Medium | Parsing and fallback covered; prompt quality is manual |
| AI images | Medium | Medium | Selection covered; Pollinations is manual |
| Agents | Medium | Low | Runner covered; agent prompts are manual |
| Email | Medium | High | Send, classification and visibility covered; real Gmail is manual |
| Media | High | Medium | Sniffing, limits and gzip covered |
| Notifications | Medium | Medium | Publish rules covered; scheduling is manual |
| Themes | High | Low | Simple and well covered |
| Seeds | Medium | Medium | Special events covered |
| Dashboards | Medium | Medium | Scoping and counters covered; IP enrichment is manual |
| Schedulers | **None** | High | Highest residual risk |
| Controllers and wiring | **None** | High | No `@SpringBootTest` |

## 3. Known gaps (prioritised)

| # | Gap | Risk | Recommended action |
| --- | --- | --- | --- |
| G1 | No scheduler tests | A regression could skip or duplicate wishes | Extract the filtering logic and test it; add one integration test per job |
| G2 | No context or integration test | Wiring errors reach production | Add a `@SpringBootTest` smoke test |
| G3 | No repository tests | Month/day and circle queries unverified | Add `@DataJpaTest` for the four hottest repositories |
| G4 | Chat SQL untested | Upsert, `DISTINCT ON` and counters unverified | Testcontainers tests for `ChatMessageRepository` |
| G5 | No controller tests | Status codes and validation unverified automatically | Add `@WebMvcTest` for the main controllers |
| G6 | No coverage tool | Trends invisible | Add JaCoCo with a non-blocking report |
| G7 | No contract test | UI breakage | OpenAPI diff in CI |
| G8 | Agent prompts untested | Output quality regressions | Golden-set evaluation |
| G9 | Time and randomness are not injectable | Some tests rely on fixed expectations | Inject `Clock` and a code generator |
| G10 | No UI tests here | End-to-end confidence limited | The UI repository owns this |

## 4. Producing a real coverage number

Add to `build.gradle`:

```groovy
plugins { id 'jacoco' }
jacoco { toolVersion = '0.8.12' }
tasks.named('test') { finalizedBy jacocoTestReport }
jacocoTestReport {
  reports { xml.required = true; html.required = true }
}
```

Then run:

```bash
./gradlew test jacocoTestReport
# report: build/reports/jacoco/test/html/index.html
```

Suggested gates after the first measurement: services 60% line coverage, filters and utilities 80%,
overall 50%. Raise gradually.

## 5. Coverage of business rules

| Rule | Covered by |
| --- | --- |
| Lockout at 5 failures | `AuthServiceImplTest` |
| OTP resend cap | `AuthServiceImplTest` |
| Reset single use | `AuthServiceImplTest` |
| Single-session rejection | `AuthServiceImplTest`, `JwtAuthFilterTest` |
| Event ownership 404 | `EventServiceImplTest` |
| Active event type required | `EventServiceImplTest` |
| Circle direction and reuse | `CircleServiceImplTest` |
| Calendar merge and de-duplication | `CalendarServiceImplTest` |
| Chat circle gate | `ChatServiceImplCircleGateTest` |
| Festival mapping upsert | `FestivalWishMappingServiceImplTest` |
| Birthday duplicate guard | **Not covered** |
| Festival same-day retry | **Not covered** |
| Email classification and sensitivity | `GmailEmailServiceImplTest` |
| Media size and sniffing | `MediaValidationTest`, `MediaContentTypesTest` |
| Gzip round-trip | `GzipUtilTest` |
| Wish image priority | `WishImageServiceImplTest` |
| Wishes link and escaping | `WishEmailExtrasTest`, `WishesUrlsTest` |
| Notification publish rules | `NotificationServiceImplTest` |
| Cache bounds and TTL | `CacheConfigurationTest` |

## 6. Exit criteria for coverage work

1. G1, G2 and G4 closed (the three highest-risk gaps).
2. A JaCoCo report produced in CI on every build.
3. Overall line coverage at least 50%; services at least 60%.
4. Every rule in §5 has at least one automated test.
5. This document updated with real numbers.
