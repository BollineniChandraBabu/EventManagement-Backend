# Coding standards

## 1. Java version and language level

* Java 17 language features are allowed (records, `var`, switch expressions, text blocks).
* Use records for DTOs; use classes with Lombok for entities.
* Prefer `Optional` for lookups; never return `null` collections.

## 2. Formatting

| Rule | Value |
| --- | --- |
| Formatter | google-java-format 1.23.0 via Spotless |
| Indent | 2 spaces |
| Line length | 100 (formatter default) |
| Imports | No wildcards, no unused imports (Spotless removes them) |
| Trailing whitespace | Removed by Spotless |
| Command | `./gradlew spotlessApply` (formats the whole tree — revert unrelated files) |

Because `build` depends on `spotlessApply`, a build reformats sources. Run `spotlessApply` before
committing and check `git status` for unrelated changes.

## 3. Naming

| Element | Convention | Example |
| --- | --- | --- |
| Package | lower case, singular | `com.familywishes.service.impl` |
| Class | PascalCase, noun | `CircleServiceImpl`, `JwtAuthFilter` |
| Interface | PascalCase, capability | `AuthService`, `WishImageService` |
| Method | camelCase, verb | `sendRequest`, `findTodaysBirthdays` |
| Constant | UPPER_SNAKE | `MAX_TRACKED_KEYS`, `CONCURRENT_LOGIN_MARKER` |
| DTO record | PascalCase with suffix | `UserRequest`, `EventResponse` |
| Boolean field | `is`/`has`/`can` prefix | `isBirthdayEnabled`, `canSendEmail` |
| Entity table | snake_case plural | `festival_wish_mappings` |
| Test | `<Class>Test` | `CircleServiceImplTest` |
| Test method | sentence | `rejectsRequestToSelf` |

## 4. Package layout rules

1. Controllers in `controller`, services in `service`/`service.impl`, repositories in `repository`.
2. Feature-specific helpers stay near their feature (`chat`, `ai`, `agent`).
3. Pure helpers go to `util` and must not depend on services.
4. Nothing in `entity` depends on `dto` or `service`.

## 5. Controllers

```java
@RestController
@RequestMapping("/api/things")
@RequiredArgsConstructor
public class ThingController {
  private final ThingService thingService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ThingResponse create(@Valid @RequestBody ThingRequest request) {
    return thingService.create(request);
  }
}
```

Rules:

1. No business logic, no repository access, no `try/catch`.
2. `@Valid` on request bodies; `@PreAuthorize` where the role matters.
3. Return DTOs, never entities.
4. Use `@RequestParam` with defaults for paging.

## 6. Services

1. Interfaces in `service`, implementations in `service.impl`.
2. `@Transactional` on write methods; keep external calls out of transactions where possible.
3. Throw typed exceptions (`NotFoundException`, `BadRequestException`).
4. Resolve the current user from the security context, not from a request parameter.
5. Log with `@Slf4j`; never `System.out`.

## 7. Entities and DTOs

```java
@Entity
@Table(name = "things")
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class Thing extends ActivatableEntity { … }
```

```java
public record ThingRequest(@NotBlank String name, @NotNull Long ownerId) {}
```

Rules:

1. Entities extend `AuditableEntity`/`ActivatableEntity`; timestamps are managed there.
2. Use `SMALLINT` booleans via the converter and ordinal enums (append only).
3. `@Column(length = …)` for bounded strings; `TEXT` for bodies.
4. DTOs are immutable records; no Lombok on them.
5. Validation annotations live on the request record.

## 8. Error handling

1. Throw, do not catch, in controllers.
2. Add a handler in `GlobalExceptionHandler` for a new exception type.
3. Never include stack traces or SQL in messages.
4. Use `404` to hide a resource the caller may not see.

## 9. Logging

```java
log.info("Birthday wish sent to user {}", user.getId());
log.warn("Gemini model {} failed with status {}", model, status);
log.error("mail send failed", ex);
```

1. Parameterised messages, no string concatenation.
2. No personal data, tokens or bodies.
3. `info` for lifecycle, `warn` for degraded behaviour, `error` with the exception.

## 10. Tests

1. JUnit 5, Mockito, AssertJ.
2. One behaviour per test; descriptive names.
3. Arrange/Act/Assert with blank lines.
4. No network, no real database.
5. Cover the failure path as well as the happy path.

## 11. SQL and repositories

1. Derived query names for simple filters.
2. `@Query` JPQL for joins and month/day logic.
3. `@Modifying` for bulk updates.
4. Chat SQL uses named parameters only.
5. Never concatenate user input into SQL.

## 12. Configuration

1. Every tunable is a property with an environment override:
   `app.thing.size=${THING_SIZE:10}`.
2. Document new properties in the configuration reference.
3. Never hard-code secrets.

## 13. Comments

Write a comment only when the code cannot express the intent: a non-obvious invariant, a workaround,
a subtle ordering requirement or a deliberate trade-off. Do not restate the code or narrate history.

## 14. Angular (separate repository)

The UI is not in this repository, but for consistency:

| Aspect | Convention |
| --- | --- |
| Language | TypeScript strict |
| Components | One per folder, `*.component.ts/html/scss` |
| Services | One per API area, injectable, typed with interfaces matching the DTOs |
| Formatting | Prettier, 2 spaces |
| State | RxJS services; avoid deep nesting |
| Naming | `kebab-case` files, `PascalCase` classes |
| Tests | Jasmine/Karma or Jest for services and key components |

## 15. Code quality checklist

- [ ] Formatted with google-java-format
- [ ] No unused imports or fields
- [ ] No `System.out.println`
- [ ] No secret, token or personal data in code or logs
- [ ] Tests added and passing
- [ ] Documentation updated
- [ ] No reformatting of unrelated files
