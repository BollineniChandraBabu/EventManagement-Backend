# Onboarding guide

For a developer joining the project. Read in this order; each step links to the detail.

## 1. Day 1 — understand the product (1–2 hours)

| Read | Why |
| --- | --- |
| [../business/project-overview.md](../business/project-overview.md) | What the product does and for whom |
| [../business/functional-requirements.md](../business/functional-requirements.md) | What it must do |
| [../business/glossary.md](../business/glossary.md) | Vocabulary (circle, wish, festival mapping) |
| [../README.md](../README.md) | Documentation map |

Key idea: a family administrator records people and dates; scheduled jobs send AI-written greeting
emails; members share dates and chat with their circles.

## 2. Day 1 — get it running (2–3 hours)

1. Install JDK 17 and Gradle 8.14 ([../deployment/environment-setup.md](../deployment/environment-setup.md)).
2. Start PostgreSQL and create a database.
3. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.
4. `./gradlew test` then `./gradlew bootRun`.
5. `curl localhost:8080/health` → `UP`.
6. Open `http://localhost:8080/swagger-ui.html`.
7. Create an admin user in SQL ([../deployment/build-and-run.md](../deployment/build-and-run.md) §6).
8. Sign in with `POST /api/auth/login`.

> Optional: configure Gmail, Gemini and Supabase to exercise the full feature set. Without them the
> API starts but email, AI and uploads do not work.

## 3. Day 2 — architecture (2–3 hours)

| Read | Why |
| --- | --- |
| [../architecture/system-architecture.md](../architecture/system-architecture.md) | The big picture |
| [../architecture/system-design.md](../architecture/system-design.md) | Module design |
| [../architecture/component-diagram.md](../architecture/component-diagram.md) | What talks to what |
| [../backend/backend-design.md](../backend/backend-design.md) | Packages and layers |
| [../backend/business-logic.md](../backend/business-logic.md) | The rules you must not break |

Then trace two flows in the code:

1. **Sign-in:** `AuthController` → `AuthServiceImpl.login` → `JwtService` → `JwtAuthFilter`.
2. **Birthday wish:** `BirthdayMailScheduler` → `AiServiceImpl.generate` → `GmailEmailServiceImpl` →
   `email_logs`.

## 4. Day 2 — data model (1 hour)

| Read | Why |
| --- | --- |
| [../backend/database-design.md](../backend/database-design.md) | Tables and constraints |
| [../backend/entity-relationship-diagram.md](../backend/entity-relationship-diagram.md) | Relationships |
| [../backend/data-dictionary.md](../backend/data-dictionary.md) | Field meanings |

Try these queries against your local database:

```sql
SELECT code, display_name FROM event_type_seeds;
SELECT email, birthday, last_birthday_wish_sent FROM users;
SELECT id, subject, status, email_type FROM email_logs ORDER BY id DESC LIMIT 5;
```

## 5. Day 3 — make a change

Good first changes:

1. Add a field to a DTO and expose it (follow the API specification).
2. Add a unit test for an untested rule (birthday duplicate guard).
3. Fix a known issue from [../operations/known-issues.md](../operations/known-issues.md) marked P4.

Workflow: [contribution-guide.md](contribution-guide.md), style: [coding-standards.md](coding-standards.md).

## 6. Essential commands

```bash
./gradlew compileJava
./gradlew test
./gradlew test --tests '*AuthServiceImplTest'
./gradlew spotlessApply
./gradlew bootJar
./gradlew bootRun
docker build -t family-wishes:local .
docker run --rm -p 8080:8080 --env-file .env family-wishes:local
```

## 7. Where things live

| I need to… | Look at |
| --- | --- |
| Add an endpoint | `controller/`, `service/`, `service/impl/`, then the API specification |
| Change a rule | `service/impl/`, then [business-logic.md](../backend/business-logic.md) |
| Change the schema | `entity/`, then [database-design.md](../backend/database-design.md) |
| Add a job | `scheduler/` + `WishesSchedulerConfig`, then [background-jobs.md](../backend/background-jobs.md) |
| Change configuration | `application.properties`, then [configuration-reference.md](../backend/configuration-reference.md) |
| Change AI behaviour | `ai/`, `agent/` |
| Change email | `GmailEmailServiceImpl`, `EmailTemplateBuilder`, `WishEmailExtras` |
| Change storage | `SupabaseStorageService`, `MediaValidation` |
| Deploy | [../deployment/deployment-guide.md](../deployment/deployment-guide.md) |

## 8. Conventions to internalise

1. Controllers never touch repositories or entities.
2. Ownership checks live in services; hidden resources return 404.
3. Every list is `PagedResponse`.
4. Every tunable is a property with an environment override.
5. Schedulers must be idempotent (date stamps, unique constraints).
6. Uploads are validated by bytes, not by name.
7. Never log secrets or personal content.
8. Tests: JUnit 5 + Mockito, no network.

## 9. Traps

| Trap | Why |
| --- | --- |
| Running locally against production data | The schedulers start and send real email |
| `spotlessApply` on the whole tree | Reverts unrelated files; check `git status` |
| Gradle with only JDK 21 | The 17 toolchain must be available |
| Changing an enum ordinal | Corrupts existing rows |
| Renaming the jar version | Breaks the Dockerfile `COPY` |
| Triggering jobs "to test" | Duplicate guards may block real sends later that day |

## 10. Who to ask

| Topic | Owner |
| --- | --- |
| Product behaviour | Product owner / family administrator |
| Backend code | Backend owner |
| Deployment and secrets | Operator |
| UI | UI repository maintainers |

## 11. First-week checklist

- [ ] Project runs locally
- [ ] Tests pass
- [ ] Read the business overview and glossary
- [ ] Read the architecture and backend design
- [ ] Traced sign-in and birthday wish in the code
- [ ] Ran the smoke API calls
- [ ] Opened one small pull request
- [ ] Read the security and known-issues documents
