# Dependency management

## 1. Build setup

* Gradle with the Spring Boot plugin `3.3.3`, `io.spring.dependency-management 1.1.6`, and Spotless
  `6.25.0`.
* Java toolchain pinned to **17**; the build compiles with 17 even when Gradle runs on a newer JDK.
* Repositories: `mavenCentral()` only, resolved from `settings.gradle`
  (`RepositoriesMode.PREFER_SETTINGS`).
* Version management: the Spring Boot BOM controls Spring and most transitive versions; the AWS SDK
  uses its own BOM; JJWT and springdoc are pinned explicitly.
* `tasks.named('build')` depends on `spotlessApply`, so a build reformats sources.

## 2. Direct dependencies

| Dependency | Version | Purpose |
| --- | --- | --- |
| `spring-boot-starter-web` | Boot 3.3.3 | REST, Jackson, Tomcat |
| `spring-boot-starter-websocket` | Boot | STOMP over WebSocket for chat |
| `spring-boot-starter-security` | Boot | Filter chain, method security, BCrypt |
| `spring-boot-starter-validation` | Boot | Bean Validation |
| `spring-boot-starter-data-jpa` | Boot | Hibernate, repositories, transactions |
| `spring-boot-starter-mail` | Boot | `JavaMailSender` for SMTP |
| `spring-boot-starter-thymeleaf` | Boot | Present for template rendering of the email layout |
| `spring-boot-starter-quartz` | Boot | Quartz scheduler with JDBC store |
| `spring-boot-starter-cache` | Boot | `@Cacheable` abstraction |
| `springdoc-openapi-starter-webmvc-ui` | 2.6.0 | OpenAPI JSON and Swagger UI |
| `jjwt-api` | 0.12.6 | JWT API |
| `jjwt-impl` (runtime) | 0.12.6 | JWT implementation |
| `jjwt-jackson` (runtime) | 0.12.6 | JWT JSON serialisation |
| `postgresql` (runtime) | Boot-managed | PostgreSQL JDBC driver |
| `ehcache` (runtime) | Boot-managed | JCache provider |
| `hibernate-jcache` (runtime) | Boot-managed | Hibernate second-level cache bridge |
| `lombok` (compileOnly + annotationProcessor) | Boot-managed | Getters, builders, `@Slf4j` |
| `google-api-services-gmail` | v1-rev20240520-2.0.0 | Gmail send API |
| `google-auth-library-oauth2-http` | 1.23.0 | OAuth credentials and ID-token verification |
| `google-http-client-jackson2` | 1.43.3 | HTTP transport for Google clients |
| `jakarta.mail` | 2.0.1 | MIME message building |
| `software.amazon.awssdk:s3` | BOM 2.25.48 | S3 client and presigner for Supabase Storage |
| `jaxb-api` / `jaxb-runtime` | 2.3.1 / 2.3.8 | XML support required by some transitive Google/Brevo paths |

## 3. Test dependencies

| Dependency | Purpose |
| --- | --- |
| `spring-boot-starter-test` | JUnit 5, AssertJ, Mockito, Spring test context |
| `spring-security-test` | Security test support |
| `junit-platform-launcher` (runtime) | Test engine launcher |
| `lombok` (test) | Generated accessors in tests |

## 4. Transitive risks and notes

| Concern | Detail |
| --- | --- |
| Google client stack | Pulls Guava, `google-http-client`, `opencensus`; the tree is large but stable |
| `jakarta.mail` 2.0.1 | Older than the newest Jakarta Mail; kept for compatibility with the MIME code |
| JAXB 2.3.x | `javax.xml.bind` era; needed by legacy paths, not used directly |
| AWS SDK BOM | Pins all AWS modules consistently |
| Ehcache 3 | Uses `javax.cache` (JCache) while the app uses Jakarta EE; the provider still works through Hibernate's bridge |
| Thymeleaf | Present although the email template is assembled with `EmailTemplateBuilder` |

## 5. Upgrade policy

| Item | Policy |
| --- | --- |
| Spring Boot | Upgrade in a dedicated change; re-run the suite; check release notes for Hibernate and Security changes |
| JJWT | Keep `api`, `impl` and `jackson` on the same version |
| AWS SDK | Bump the BOM version only |
| Google clients | Bump together (`api-services-gmail`, `auth-library`, `http-client`) |
| google-java-format | Bump with Spotless; reformat only touched files |
| Database driver | Follow the Boot BOM unless a fix is needed |

## 6. Vulnerability management

See [../security/vulnerability-management.md](../security/vulnerability-management.md). In short:

1. Generate a dependency report (`./gradlew dependencies`).
2. Run a scanner (OWASP Dependency-Check, Trivy, or GitHub Dependabot).
3. Triage by exploitability in this deployment (for example, a library only reachable from an
   unused feature is lower priority).
4. Upgrade the smallest set, re-run tests, and record the change in
   [../project/changelog.md](../project/changelog.md).

## 7. Reproducibility

* Versions are pinned by the BOMs and explicit declarations; there is no dynamic version range.
* The Docker build uses `gradle:8.14.3-jdk17-alpine`, so the build tool version is fixed by the image.
* No dependency is fetched from a custom repository.
* The Gradle wrapper is git-ignored in this repository; CI or Docker supplies Gradle.

## 8. Adding a dependency — checklist

1. Confirm no existing dependency already provides it.
2. Prefer a Spring Boot-managed version (omit the version).
3. Add it to `build.gradle` in the right configuration (`implementation`, `runtimeOnly`,
   `compileOnly`).
4. Check licence compatibility (the project is proprietary).
5. Run `./gradlew test` and `./gradlew dependencies` to inspect the resolved tree.
6. Document the purpose in this file.
