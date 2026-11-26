# Backup and recovery

## 1. What must be backed up

| Item | Where | Backed up by | Frequency |
| --- | --- | --- | --- |
| Primary database | Neon | Provider automated backups + manual dumps | Daily (provider) |
| Chat database | Supabase | Provider automated backups | Daily (provider) |
| Storage objects | Supabase Storage | Not automatically; copy on demand | On demand |
| Configuration | Render environment variables | Documented in [../deployment/secrets-management.md](../deployment/secrets-management.md) | On change |
| Code | Git repository | Git remote | On push |
| Documentation | Git repository | Git remote | On push |

The application holds no local state, so the container does not need backing up.

## 2. Provider backups

| Provider | Feature | Notes |
| --- | --- | --- |
| Neon | Point-in-time restore / branch | Verify the plan's retention window |
| Supabase (database) | Daily backups per plan | Check the retention window |
| Supabase (storage) | No automatic versioning by default | Copy the bucket manually |

Confirm the actual retention window in each provider's dashboard; the defaults change by plan.

## 3. Manual backups

### Primary database

```bash
# Custom format (compressed, restorable with pg_restore)
pg_dump "$DB_URL" -Fc -f backup-primary-$(date +%F-%H%M).dump

# Plain SQL (readable, restorable with psql)
pg_dump "$DB_URL" -Fp -f backup-primary-$(date +%F-%H%M).sql
```

### Chat database

```bash
pg_dump "$SUPABASE_DB_URL" -Fc -f backup-chat-$(date +%F-%H%M).dump
```

### Storage

```bash
# With the AWS CLI configured for the Supabase S3 endpoint
aws --endpoint-url "$SUPABASE_STORAGE_ENDPOINT" s3 sync \
  "s3://${SUPABASE_STORAGE_BUCKET}" ./backup-storage-$(date +%F)
```

## 4. Backup schedule

| Backup | Frequency | Retention | Owner |
| --- | --- | --- | --- |
| Provider automatic (primary) | Daily | Plan default | Provider |
| Provider automatic (chat) | Daily | Plan default | Provider |
| Manual dump before a release | Per release | 90 days | Release owner |
| Manual dump before a migration | Per migration | 90 days | Release owner |
| Storage copy | Monthly | 3 months | Operator |

Store dumps outside the repository and outside the application host. Treat them as Confidential.

## 5. Restore procedures

### Primary database

```bash
# 1. Stop the application (Render: suspend the service)
# 2. Restore
pg_restore --clean --if-exists --no-owner -d "$DB_URL" backup-primary-YYYY-MM-DD-HHMM.dump
# 3. Start the application and verify
```

Plain SQL:

```bash
psql "$DB_URL" -f backup-primary-YYYY-MM-DD-HHMM.sql
```

### Chat database

```bash
pg_restore --clean --if-exists --no-owner -d "$SUPABASE_DB_URL" backup-chat-YYYY-MM-DD-HHMM.dump
```

### Storage

```bash
aws --endpoint-url "$SUPABASE_STORAGE_ENDPOINT" s3 sync \
  ./backup-storage-YYYY-MM-DD "s3://${SUPABASE_STORAGE_BUCKET}"
```

## 6. Verification after a restore

| Check | Expected |
| --- | --- |
| Startup | `Started FamilyWishesApplication` |
| Row counts | Users, events, email logs match the backup date |
| Recent records | The last user and event created before the backup exist |
| Chat | Conversations and messages present |
| Storage | Profile pictures and email images resolve |
| Schedulers | Nine jobs registered |
| Smoke tests | Sign-in, calendar, one test email |

## 7. Recovery scenarios

| Scenario | Action | Expected loss |
| --- | --- | --- |
| Accidental row deletion | Restore the table from the latest dump, or re-insert | Since the last backup |
| Bad migration | Restore the pre-migration dump | Since that backup |
| Corrupted storage object | Restore from the storage copy | Since the last copy |
| Provider outage | Wait; use provider status; restore elsewhere if prolonged | Depends |
| Ransomware / compromised credentials | Rotate secrets, restore from a known-good backup | Since that backup |

## 8. Restore test (quarterly)

1. Create a scratch database.
2. Restore the latest dump into it.
3. Start the application against it (`DB_URL` pointing at the scratch database).
4. Run the smoke tests.
5. Record the duration and any issues here.

| Date | Backup used | Duration | Result | Notes |
| --- | --- | --- | --- | --- |
| (to be filled) | | | | |

## 9. RPO and RTO

| Metric | Target |
| --- | --- |
| RPO (maximum data loss) | 24 hours |
| RTO (time to restore) | 2 hours |
| Verification time | 30 minutes after restore |

## 10. Rules

1. **Never** test a restore against production.
2. Encrypt or restrict access to dumps; they contain personal data and `violated_users`.
3. Record every backup and restore in this document.
4. Delete dumps older than the retention period.
5. Confirm the provider's retention window each quarter — plans change.
