# Rollback plan

## 1. When to roll back

| Trigger | Roll back? |
| --- | --- |
| `/health` fails after deploy | Yes, immediately |
| Sign-in broken for everyone | Yes |
| Wish emails failing after a change | Yes |
| A new endpoint is buggy but the rest works | No — hotfix forward |
| Cosmetic regression | No — fix in the next release |
| Schema change that cannot be reversed | Roll forward only (see §5) |

## 2. Pre-deploy preparation (makes rollback possible)

1. Record the **current commit SHA** and, if applicable, the image tag.
2. Take a database backup: `pg_dump "$DB_URL" -Fc -f pre-release-$(date +%F).dump`.
3. Note the environment variables changed in this release.
4. Keep the previous container image available (Render keeps build history).

## 3. Application rollback

### Render

1. Open the service in the Render dashboard.
2. Choose *Deploys* → select the previous successful deploy → *Redeploy*.
3. Watch the log for `Started FamilyWishesApplication`.
4. Run the smoke checks in [deployment-guide.md](deployment-guide.md) §3.3.

### Local or self-hosted

```bash
docker stop family-wishes && docker rm family-wishes
docker run -d --name family-wishes -p 8080:8080 --env-file .env family-wishes:<previous-tag>
```

### Git-based

```bash
git checkout <previous-sha>
# rebuild and redeploy, or revert the offending commit on main
git revert --no-edit <bad-sha> && git push
```

## 4. Database rollback

Roll back the schema only if the release changed it destructively.

| Change | Rollback approach |
| --- | --- |
| Added table/column (Hibernate `update`) | No action; the old code ignores it |
| Renamed column | Restore from backup, or add the old column back and copy data |
| Dropped column/table | Restore from backup |
| Changed type | Restore from backup |
| Data corruption | Restore from backup, then replay what can be recovered |

Restore procedure (PostgreSQL):

```bash
# Stop the application first
pg_restore --clean --if-exists -d "$DB_URL" pre-release-YYYY-MM-DD.dump
```

For the chat database, restore only if the chat schema was altered destructively; the initializer is
additive.

## 5. Roll-forward instead of rollback

Prefer rolling forward when:

* The schema change is already applied and destructive.
* Only one endpoint is affected.
* The fix is small and can be tested quickly.

Steps: write the fix, run the tests, deploy, verify.

## 6. Rollback of external configuration

| Change | Revert by |
| --- | --- |
| Environment variable | Restore the previous value in Render and restart |
| Gmail refresh token | Restore the previous token (if still valid) |
| Bucket or region | Restore the previous values |
| `JWT_SECRET` | Restoring the old secret invalidates tokens minted with the new one; all users must sign in again |
| Agent flags | Flip back |

## 7. Post-rollback verification

| Check | Expected |
| --- | --- |
| `/health` | `UP` |
| Sign-in | Works |
| Calendar | Loads |
| Schedulers | Nine jobs with sensible next fire times |
| Email | A test email arrives |
| Logs | No repeated errors |

## 8. Communication

1. Tell the operator and product owner that a rollback happened and why.
2. Record it in [../project/changelog.md](../project/changelog.md).
3. Open a defect ([../testing/defect-tracking.md](../testing/defect-tracking.md)) for the cause.
4. Update [../operations/known-issues.md](../operations/known-issues.md) if the issue persists.

## 9. Time targets

| Action | Target |
| --- | --- |
| Decide to roll back | < 15 minutes from detection |
| Application rollback complete | < 10 minutes |
| Database restore complete | < 60 minutes |
| Communication sent | < 30 minutes |

## 10. Practice

Run a rollback drill once per quarter on a non-production environment: deploy a deliberately broken
image, roll back, and time it. Record the result here.
