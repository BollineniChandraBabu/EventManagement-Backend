# 0009 — Supabase Storage over the S3 API for all media

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../backend/external-api-integrations.md](../../backend/external-api-integrations.md)

## Context

The product stores greeting images, email images, chat attachments, profile pictures and the email
signature. Some must be embedded in emails, some served to the UI, some downloaded by users. Storing
binary data in PostgreSQL would bloat backups and slow queries.

## Decision

* Use one Supabase bucket (`golden-greetings`) through its S3-compatible endpoint with the AWS SDK v2
  (`S3Client` + `S3Presigner`, path-style access).
* Keep only object keys (and optionally public URLs) in the database.
* Organise objects by purpose and date: `Wish Images/{date}/{uuid}{ext}`, `Chat/{date}/{uuid}{ext}`,
  email images by type, profile pictures by user id.
* Generate object keys server-side (UUID + detected extension); never trust the client file name.
* Serve media through presigned GET URLs, or a public base URL when configured.
* Treat delete as best effort.

## Consequences

**Positive**

* The database stays small and fast; media scales independently.
* Presigned URLs keep the bucket private while allowing the UI to render images.
* One client library for every media need.

**Negative**

* The S3 client is built with static credentials; rotation needs a restart.
* Supabase quotas and availability become part of the product's availability.
* Orphaned objects can accumulate when rows are deleted without the storage delete succeeding.

**Follow-up**

* Add a periodic orphan sweep.
* Move credentials to a rotating secret if Supabase supports it.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Store bytes in PostgreSQL (`bytea`) | Bloats backups, hurts query performance |
| Local disk in the container | Ephemeral on Render; lost on redeploy |
| Cloudinary / imgix | Extra provider; the bucket was already available |
