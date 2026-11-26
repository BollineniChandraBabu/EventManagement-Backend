# 0013 — Gmail API as the primary email transport

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../integration-design.md](../integration-design.md), [../../operations/troubleshooting-guide.md](../../operations/troubleshooting-guide.md)

## Context

Wishes must be delivered from a real mailbox that recipients recognise. Sending from a new domain
requires SPF, DKIM and DMARC setup and reputation building; SMTP from a cloud host is often blocked or
lands in spam.

## Decision

* Send through the Gmail API using OAuth 2.0 (`gmail.send` scope) with a refresh token
  (`GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN`).
* Keep `EmailServiceImpl` (SMTP) and `BrevoEmailServiceImpl` as alternative transports.
* Log every attempt in `email_logs`; retry failures up to 3 times.
* Detect the revoked/expired refresh-token case and log an actionable message.

## Consequences

**Positive**

* Mail comes from a trusted mailbox with existing reputation.
* OAuth avoids storing an account password.
* Delivery results are logged and visible.

**Negative**

* Bound to Google's quotas and policy; the refresh token can be revoked and needs manual re-consent.
* OAuth setup is a manual, error-prone step (`invalid_grant`).
* No bounce/complaint webhook integration.

**Follow-up**

* Document the re-consent runbook (done in
  [../../operations/troubleshooting-guide.md](../../operations/troubleshooting-guide.md)).
* Consider a transactional provider with delivery webhooks if volume grows.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| SMTP from the cloud host | Blocked or spam-filtered frequently |
| Own domain with DKIM | Setup and reputation work outside the project's scope |
| Brevo only | Free-tier limits and sender verification; kept as a fallback |
