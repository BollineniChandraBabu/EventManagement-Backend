# Contribution guide

## 1. Workflow

```
main ──► feature branch ──► pull request ──► review ──► merge ──► deploy
```

1. Branch from `main` with a descriptive name (`feat/circle-reminders`, `fix/email-type`).
2. Make focused commits with clear messages.
3. Run the tests locally.
4. Open a pull request describing the change, the risk and the testing done.
5. Address review comments.
6. Merge (squash is fine) and let the deploy run.

## 2. Before you push

```bash
./gradlew test
./gradlew spotlessApply            # format, then revert unrelated files
./gradlew bootRun                  # optional smoke start
```

| Check | Required |
| --- | --- |
| Tests pass | Yes |
| New behaviour has a test | Yes |
| Java formatted (google-java-format) | Yes |
| No secret committed | Yes |
| Documentation updated | Yes for API/config/rule changes |

## 3. Commit messages

```
<type>(<scope>): <short summary>

<optional body: why, not what>
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `security`.

Examples:

```
feat(circles): remind addressees after seven days
fix(email): keep OTP bodies hidden from non-admins
docs(api): document the chat attachment endpoint
```

## 4. Pull request template

```markdown
## What
Brief description.

## Why
The problem or requirement.

## How
Approach and notable decisions.

## Testing
- [ ] `./gradlew test`
- [ ] Manual API checks (list endpoints)
- [ ] Screenshots/logs if relevant

## Risk and rollback
What could break; how to roll back.

## Checklist
- [ ] Tests added or updated
- [ ] No secrets
- [ ] Docs updated
- [ ] Formatting applied
```

## 5. Code review expectations

| Aspect | Look for |
| --- | --- |
| Correctness | Business rules match [../backend/business-logic.md](../backend/business-logic.md) |
| Security | Authorisation, ownership, validation, escaping ([../security/secure-coding-guidelines.md](../security/secure-coding-guidelines.md)) |
| Tests | Happy path and failure path |
| Style | Consistent with [coding-standards.md](coding-standards.md) |
| Docs | API, configuration and known issues updated |
| Performance | No obvious N+1 queries or unbounded loops |

## 6. Branch protection (recommended)

1. Require a pull request for `main`.
2. Require the test workflow to pass.
3. Require one approval.
4. Disallow force pushes.

## 7. Working with the UI

The Angular UI lives in another repository. If your change affects the contract:

1. Add fields rather than removing them.
2. Note the change in the PR and the API specification.
3. Coordinate the UI release.

## 8. Reporting issues

Use the defect template in [../testing/defect-tracking.md](../testing/defect-tracking.md) §4. For
security issues, do not open a public issue; follow
[../security/vulnerability-management.md](../security/vulnerability-management.md) §11.

## 9. Documentation changes

Documentation lives in `docs/`. Keep it accurate:

| Change | Update |
| --- | --- |
| New endpoint | [../backend/api-specification.md](../backend/api-specification.md) |
| New property | [../backend/configuration-reference.md](../backend/configuration-reference.md) |
| New table or column | [../backend/database-design.md](../backend/database-design.md), [../backend/data-dictionary.md](../backend/data-dictionary.md) |
| New rule | [../backend/business-logic.md](../backend/business-logic.md) |
| New job | [../backend/background-jobs.md](../backend/background-jobs.md) |
| Fixed defect | [../operations/known-issues.md](../operations/known-issues.md) |

## 10. Etiquette

1. Keep pull requests small and reviewable.
2. Explain trade-offs in the description, not in code comments.
3. Ask before adding a dependency.
4. Do not reformat unrelated files.
5. Leave the codebase better than you found it, in small steps.
