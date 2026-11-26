# Change management

## 1. Types of change

| Type | Examples | Approval | Lead time |
| --- | --- | --- | --- |
| Standard | Documentation, tests, dependency patch | Developer | Same day |
| Normal | New endpoint, new job, behaviour change | Release owner | Next release |
| Major | Breaking API change, schema removal, provider switch | Release owner + product owner | Planned release |
| Emergency | Security fix, production outage fix | Release owner | Immediate |
| Configuration | Environment variable change | Operator | Immediate with verification |

## 2. Change request

For normal and major changes, record:

```markdown
**Title:**
**Type:** normal | major
**Why:** the problem or opportunity
**What:** the change, including API and schema impact
**Affected:** endpoints, tables, jobs, integrations, UI
**Risk:** what could break, and the rollback
**Testing:** tests added, manual checks
**Docs:** files updated
**Approval:** who and when
```

## 3. Impact analysis checklist

- [ ] API contract: new/removed/changed fields or endpoints
- [ ] Database: new columns, constraints, enum ordinals
- [ ] Schedulers: timing, guards, duplicate risk
- [ ] Email: new templates, types, sensitivity classification
- [ ] Security: authorisation, ownership, input validation
- [ ] UI: does the Angular app need a change first?
- [ ] Configuration: new properties and defaults
- [ ] Documentation: API spec, configuration, business logic, known issues

## 4. UI coordination

The UI is a separate deployment. For any breaking change:

1. Deploy the backend in a backward-compatible way (add, do not remove).
2. Deploy the UI that uses the new field.
3. Remove the old field in a later release.

## 5. Database changes

| Change | Process |
| --- | --- |
| Additive | Ship with the release; Hibernate applies it |
| Destructive | Backup, review SQL, apply manually, verify ([../deployment/database-migration.md](../deployment/database-migration.md)) |
| Enum change | Append only; never reorder ordinals |
| Index change | Additive is automatic; drops are manual |

## 6. Configuration changes

1. Change the variable in the platform (not the repository).
2. Restart.
3. Verify the affected feature.
4. Record it in the changelog when the behaviour is user-visible.

## 7. Emergency changes

1. Fix the smallest thing that restores service.
2. Deploy directly with the release owner's approval.
3. Verify.
4. Write the change up afterwards, including the missing tests to add.

## 8. Change log and traceability

| Artefact | Where |
| --- | --- |
| Commit history | Git |
| Release notes | [release-notes.md](release-notes.md) |
| Changelog | [changelog.md](changelog.md) |
| Decisions | [decision-log.md](decision-log.md) and [../architecture/architecture-decision-records/](../architecture/architecture-decision-records/README.md) |
| Defects | [../testing/defect-tracking.md](../testing/defect-tracking.md) |

## 9. Rollback decision

| Situation | Action |
| --- | --- |
| Change causes an outage | Roll back |
| Change causes data corruption | Roll back and restore |
| Change is wrong but harmless | Roll forward with a fix |
| Change is partially deployed | Complete or roll back; avoid a mixed state |

## 10. Post-change review

For major changes, answer:

1. Did it do what was intended?
2. What was the actual impact (positive and negative)?
3. Were there surprises?
4. What should be done differently next time?
