# Database migration

## 1. Current mechanism

| Database | Mechanism |
| --- | --- |
| Primary (Neon) | Hibernate `spring.jpa.hibernate.ddl-auto=update` at startup |
| Quartz tables | `spring.quartz.jdbc.initialize-schema=always` |
| Chat (Supabase) | `ChatSchemaInitializer` with idempotent DDL |

There is **no Flyway or Liquibase**. Consequences: no version history, no down-migrations, and
`update` never drops or narrows columns.

## 2. What Hibernate will and will not do

| Change | Applied automatically? |
| --- | --- |
| New table | Yes |
| New nullable column | Yes |
| New NOT NULL column on an empty table | Yes |
| New NOT NULL column on a populated table | Fails or leaves nulls depending on the database |
| Widen a column | Yes |
| Rename a column | No — adds a new column, leaves the old one |
| Drop a column | No |
| Narrow a column | No |
| Add an index declared on the entity | Yes |
| Drop an index | No |
| Change a foreign key | Partially |

## 3. Safe change patterns

### Adding a field

1. Add the field with a nullable type or a default.
2. Backfill in SQL if needed.
3. Make it required in a later release.

### Renaming a field

1. Add the new column.
2. Dual-write in code (or backfill with SQL).
3. Switch reads to the new column.
4. Drop the old column manually in a later release.

### Removing a field

1. Stop reading it in code.
2. Deploy.
3. Drop the column manually after a backup.

### Changing an enum

Because enums are stored as ordinals, **reordering or inserting a value corrupts existing rows**.
Only append new values at the end, or migrate the column to a string.

## 4. Chat schema changes

`ChatSchemaInitializer` runs idempotent statements:

```sql
CREATE TABLE IF NOT EXISTS chat_conversations (...);
CREATE TABLE IF NOT EXISTS chat_messages (...);
CREATE TABLE IF NOT EXISTS chat_message_reactions (...);
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reply_to_message_id BIGINT;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS message_type TEXT NOT NULL DEFAULT 'TEXT';
CREATE INDEX IF NOT EXISTS idx_chat_messages_conv_sent ON chat_messages (conversation_id, sent_at DESC);
```

Rules:

1. Only additive statements; never drop or rename in the initializer.
2. Keep every statement idempotent so restarts are safe.
3. Add a new `ALTER … ADD COLUMN IF NOT EXISTS` when the entity gains a field.

## 5. Manual migration procedure

For a change Hibernate cannot make:

1. Write the SQL in a reviewable file (for example `docs/deployment/sql/2026-11-26-drop-legacy.sql`).
2. Test it on a copy of production data.
3. Back up production.
4. Stop the application (to avoid concurrent writes).
5. Run the SQL in a transaction where possible.
6. Start the application and verify.
7. Record the change in [../project/changelog.md](../project/changelog.md).

## 6. Moving to Flyway (recommended)

1. Add `org.flywaydb:flyway-core` and `flyway-database-postgresql`.
2. Baseline the current schema:
   `spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=1`.
3. Set `spring.jpa.hibernate.ddl-auto=validate`.
4. Put every new change in `src/main/resources/db/migration/V<n>__<description>.sql`.
5. Keep the chat database on the initializer, or add a second Flyway instance for it.

Benefits: history, reviewability, repeatability and a way to write down-migrations.

## 7. Backup before migrating

```bash
pg_dump "$DB_URL" -Fc -f pre-migration-$(date +%F-%H%M).dump
# restore if needed
pg_restore --clean --if-exists -d "$DB_URL" pre-migration-YYYY-MM-DD-HHMM.dump
```

## 8. Verification after a migration

| Check | How |
| --- | --- |
| Application starts | Startup log has no DDL errors |
| Tables and columns | `\d+ users`, `\d+ chat_messages` |
| Data intact | Row counts and spot checks on recent records |
| Indexes | `\di` shows the expected indexes |
| Endpoints | Smoke suite ([../testing/api-testing.md](../testing/api-testing.md)) |

## 9. Rollback of a migration

| Migration | Rollback |
| --- | --- |
| Additive | No action |
| Destructive | Restore the backup |
| Data backfill | Reverse SQL prepared in advance |

See [rollback-plan.md](rollback-plan.md) §4.

## 10. Known risks

1. A startup DDL failure takes the service down.
2. `update` can lock a large table.
3. Ordinal enums are fragile.
4. No migration history makes it hard to know what a database contains.
5. Two schemas (primary and chat) must be kept in step by hand.
