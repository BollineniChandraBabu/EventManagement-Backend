# Maintenance guide

## 1. Routine maintenance calendar

| Frequency | Task | Owner |
| --- | --- | --- |
| Daily | Check `/health`, wish jobs, failed emails | Operator |
| Weekly | Review failed sign-ins and delivery failures; check scheduler stats | Operator |
| Monthly | Rebuild the image; verify provider credentials; review dependencies | Backend owner |
| Quarterly | Restore test, rollback drill, access review, documentation review | Operator |
| Yearly | Review architecture, retention policy and provider plans | Backend owner |

## 2. Dependency upgrades

```bash
# Inspect the tree
./gradlew dependencies

# Upgrade Spring Boot (build.gradle)
#  id 'org.springframework.boot' version '3.3.3'
#  id 'io.spring.dependency-management' version '1.1.6'

./gradlew clean test bootJar
```

| Package | How to upgrade | Watch for |
| --- | --- | --- |
| Spring Boot | Bump the plugin version | Hibernate, Security, Jackson changes |
| JJWT | Keep api/impl/jackson aligned | API changes between 0.11 and 0.12 |
| AWS SDK | Bump the BOM | S3 client behaviour |
| Google clients | Bump together | Auth library API changes |
| google-java-format | Bump with Spotless | Reformat only touched files |
| PostgreSQL driver | Follow the Boot BOM | — |

## 3. Image and base image maintenance

1. Rebuild monthly to pick up Alpine and JRE patches.
2. Confirm the runtime is Java 17.
3. Consider adding a non-root user and a `HEALTHCHECK`.
4. Scan the image with Trivy before deploying.

## 4. Database maintenance

| Task | Frequency | How |
| --- | --- | --- |
| Check size and growth | Monthly | Neon/Supabase dashboards |
| Review slow queries | Quarterly | Provider query insights |
| Vacuum/analyse | Automatic in PostgreSQL | — |
| Prune expired tokens | Monthly (manual today) | `DELETE FROM refresh_tokens WHERE expires_at < now();` etc. |
| Prune `violated_users` | Quarterly | Decide a retention window and delete older rows |
| Review indexes | Yearly | Compare with query patterns |
| Plan a Flyway migration | Once | [../deployment/database-migration.md](../deployment/database-migration.md) |

## 5. Storage maintenance

1. Delete orphaned objects (rows removed but objects left).
2. Check bucket size against the plan.
3. Verify the signature and default pictures still exist.
4. Rotate storage keys quarterly.

## 6. Configuration maintenance

| Task | Frequency |
| --- | --- |
| Confirm environment variables match the documentation | Each release |
| Verify `UI_BASE_URL` and emailed links | Each release |
| Review agent flags | Each release |
| Review CORS origins | Each release |
| Review rate-limit settings | Yearly |

## 7. Provider credential maintenance

| Credential | Check | Action |
| --- | --- | --- |
| Gmail refresh token | `POST /api/email/test` monthly | Re-authorise if it fails |
| Gemini key | `POST /api/ai/generate-wish` monthly | Replace if 401/403 |
| Calendarific key | Trigger the sync monthly | Renew the plan if needed |
| Storage keys | Confirm uploads work | Rotate quarterly |
| Database passwords | Confirm connections | Rotate quarterly |

## 8. Log and data hygiene

1. Review logs for personal data leakage.
2. Remove `System.out.println` calls when touching those classes.
3. Cap `violated_users` retention.
4. Delete old backups beyond the retention window.

## 9. Documentation maintenance

| Document | Review trigger |
| --- | --- |
| API specification | Any controller change |
| Configuration reference | Any property change |
| Database design and dictionary | Any entity change |
| Business logic | Any rule change |
| Known issues | Any fix or new defect |
| Runbook and troubleshooting | Any new failure mode |

## 10. Scheduled-job maintenance

| Task | Frequency |
| --- | --- |
| Confirm all nine Quartz jobs are registered | Weekly |
| Check `nextFireTime` is populated | Weekly |
| Review `failedRuns` | Weekly |
| Confirm the festival sync ran | Monthly |
| Test a manual trigger | Quarterly |

## 11. Deprecation and cleanup backlog

| Item | Action |
| --- | --- |
| `EmailServiceImpl` vs `GmailEmailServiceImpl` | Keep one transport |
| Thymeleaf dependency | Remove if unused |
| `System.out.println` | Replace with `log` |
| Keyword email classification | Always pass `EmailType` |
| Diagnostic OAuth controller | Remove |
| `ddl-auto=update` | Move to Flyway |
| In-memory scheduler stats | Persist |
| `.env` gitignore | Add |

## 12. Monthly checklist

- [ ] Image rebuilt and deployed
- [ ] Dependencies reviewed and advisories checked
- [ ] Gmail, Gemini, Calendarific and storage verified
- [ ] Festival sync confirmed for the next month
- [ ] Failed emails and sign-ins reviewed
- [ ] Backups confirmed
- [ ] Documentation updated for the month's changes
