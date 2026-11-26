# Release management

## 1. Versioning

| Item | Scheme |
| --- | --- |
| Application version | Semantic `MAJOR.MINOR.PATCH`; currently `1.0.0` |
| Where declared | `build.gradle` (`version = '1.0.0'`) and the jar name in the `Dockerfile` |
| Git tags | `v1.0.0`, `v1.1.0`, … (recommended; not yet used) |
| API versioning | None — the path has no version segment (`/api/...`); changes must stay compatible |

### What changes the version

| Change | Bump |
| --- | --- |
| Breaking API change, schema removal, behaviour change | MAJOR |
| New feature, new endpoint, new job | MINOR |
| Bug fix, config default, text change | PATCH |

Because the UI is a separate deployment, any breaking API change must be coordinated: deploy the
backend in a backward-compatible way first (add fields, keep old ones), migrate the UI, then remove.

## 2. Release types

| Type | Content | Approval |
| --- | --- | --- |
| Patch | Fixes only | Developer |
| Minor | Features | Release owner |
| Major | Breaking changes | Release owner + product owner |
| Hotfix | P1 fix | Release owner, deployed immediately |

## 3. Release checklist

**Before**

- [ ] All tests green (`./gradlew test`)
- [ ] Manual API checks for changed endpoints
- [ ] Database backup taken
- [ ] Environment variables reviewed
- [ ] Release notes written ([release-notes.md](../project/release-notes.md))
- [ ] Changelog updated ([changelog.md](../project/changelog.md))
- [ ] Known issues reviewed ([known-issues.md](../operations/known-issues.md))
- [ ] Rollback plan confirmed

**During**

- [ ] Deploy
- [ ] Startup log clean
- [ ] Smoke checks pass
- [ ] Scheduler list and next fire times correct

**After**

- [ ] Monitor for 15–30 minutes
- [ ] Confirm email delivery for the next scheduled run
- [ ] Announce the release
- [ ] Tag the commit
- [ ] Update the version if the jar name changed

## 4. Approval matrix

| Change type | Who approves |
| --- | --- |
| Documentation only | Developer |
| Test-only | Developer |
| Code fix (non-security) | Developer + one reviewer |
| New feature | Release owner |
| Security fix | Release owner; expedited |
| Schema-destructive change | Release owner + explicit backup verification |
| Secret rotation | Operator |

## 5. Release cadence

* **On demand** rather than fixed sprints; the product serves a single family group.
* Hotfixes whenever a P1 defect appears.
* A monthly check that dependencies and provider credentials are still valid.

## 6. Version bump procedure

1. Change `version = 'x.y.z'` in `build.gradle`.
2. Update the jar path in `Dockerfile` (`family-wishes-x.y.z.jar`).
3. Update the version in the README and here.
4. Commit, tag (`git tag vx.y.z`), push the tag.
5. Deploy.

> Forgetting step 2 breaks the image build: the `COPY --from=build` path will not exist.

## 7. Communication

| Audience | Channel | Content |
| --- | --- | --- |
| Family members | The UI announcement feature | What changed, what to expect |
| Operators | This documentation and the release notes | Deploy, rollback, known issues |
| Developers | Commit history and the changelog | Technical detail |

## 8. Post-release review

For a minor or major release, record:

1. What shipped and whether it matched the plan.
2. Defects found within a week.
3. Whether the rollback plan was needed.
4. Actions for the next release.

## 9. Compatibility promises

| Promise | Detail |
| --- | --- |
| Additive API changes | New fields may appear; clients must ignore unknown fields |
| Removals | Announced one release ahead; the UI must migrate first |
| Enum values | Adding a value is safe; removing one is breaking |
| Error messages | Not part of the contract, but `MFA_OTP_REQUIRED` and the `CONCURRENT_LOGIN:` prefix are relied upon |
| Schema | Additive at startup; destructive changes need explicit SQL and a backup |
