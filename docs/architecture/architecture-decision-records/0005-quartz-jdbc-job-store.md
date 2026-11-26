# 0005 — Quartz with a JDBC job store for scheduled work

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../backend/background-jobs.md](../../backend/background-jobs.md)

## Context

Wishes must go out at fixed times in Asia/Kolkata, survive a restart, and must not be sent twice when
more than one instance runs. Some jobs (unread-mail digest, circle reminders) are configurable;
others (wish times) are fixed in code.

## Decision

* Use Quartz with `job-store-type=jdbc`, `isClustered=true`, `instanceId=AUTO`, table prefix `QRTZ_`
  and `initialize-schema=always`.
* Declare jobs and triggers as Spring beans in `WishesSchedulerConfig` with `storeDurably()`.
* Use `FireAndProceed` for catch-up jobs (wishes) and `DoNothing` for digest/reminder jobs.
* Record execution statistics with `QuartzJobTrackingListener` → `SchedulerTrackingService` and
  expose them through `/api/schedulers`.
* Keep the Instagram bridge and the delivery-ops agent on Spring `@Scheduled`, because they are
  lightweight and do not need durability.

## Consequences

**Positive**

* Jobs survive restarts and are stored in the primary database.
* Clustering prevents duplicate execution across instances.
* Misfire policy is explicit per job.
* Operators can inspect and trigger jobs through the API.

**Negative**

* Quartz tables are created on every start (`initialize-schema=always`).
* Tracking statistics are in memory, so they reset on restart and are per instance.
* Two scheduling mechanisms coexist, which is a small cognitive cost.
* Job times fixed in code need a redeploy to change.

**Follow-up**

* Move job times to properties if operators need to change them.
* Consider persisting scheduler statistics if history is required.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Spring `@Scheduled` only | No durability, no clustering, no misfire handling |
| Cloud scheduler + HTTP trigger | Extra infrastructure and authentication surface |
| External cron container | Another deployment to operate |
