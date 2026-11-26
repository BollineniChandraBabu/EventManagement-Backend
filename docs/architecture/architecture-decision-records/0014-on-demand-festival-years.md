# 0014 — Fetch other years' festivals on demand

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../backend/business-logic.md](../../backend/business-logic.md), [../data-flow.md](../data-flow.md)

## Context

Festival dates move every year (lunar calendars). The monthly sync only stores the current year, but
the calendar lets users browse any month and year. Without data for the requested year, the calendar
would show nothing or, worse, repeat last year's dates.

## Decision

* Keep the monthly sync for the current year (`FestivalSyncScheduler` → `syncCalendarificFestivals`).
* When a request asks for another year, check `existsByEventDateBetween(year-01-01, year-12-31)`; if
  empty, fetch that year once from Calendarific, store it, and serve later requests from the database.
* Mark a year-scoped festival entry `recurring=false` so the calendar does not repeat it into other
  years.
* Swallow provider failures: the year simply stays empty rather than breaking the calendar.

## Consequences

**Positive**

* Any year becomes viewable after a single provider call.
* No cron for every future year and no unbounded pre-fetching.
* One extra API call per year, not per request.

**Negative**

* The first request for a new year is slower (provider latency).
* A provider outage leaves that year empty until retried.
* Stored rows accumulate for every browsed year.

**Follow-up**

* Add a warm-up job for the next year in December.
* Prune years far outside the useful range.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Sync all years up front | Wasteful and quota-limited |
| Compute dates locally | Requires a lunar calendar implementation and maintenance |
| Show only the current year | Poor UX for planning |
