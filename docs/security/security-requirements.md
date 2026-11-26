# Security requirements

| ID | Requirement | Priority | Implementation | Verified by |
| --- | --- | --- | --- | --- |
| SR-01 | Passwords shall be stored using a strong, salted hash | Must | BCrypt | Review, `AuthServiceImplTest` |
| SR-02 | Access tokens shall expire within 15 minutes | Must | `expiresIn=900` | `JwtServiceTest` |
| SR-03 | Refresh tokens shall be revocable and expire within 7 days | Must | `refresh_tokens`, cleanup job | `AuthServiceImplTest` |
| SR-04 | A user shall be able to limit sign-in to one active session | Should | `active_session_id` + `sid` | `JwtAuthFilterTest`, `AuthServiceImplTest` |
| SR-05 | Repeated failed sign-ins shall lock the account and notify the owner | Must | `failed_login_attempts`, threshold 5 | `AuthServiceImplTest` |
| SR-06 | Sensitive auth endpoints shall be rate limited per client | Must | `RateLimitFilter`, 10 per 60 s | `RateLimiterTest` |
| SR-07 | Admin operations shall require `ROLE_ADMIN` | Must | `@PreAuthorize` | SEC-AUTHZ cases |
| SR-08 | Users shall not read or modify another user's data | Must | Ownership checks | SEC-AUTHZ cases |
| SR-09 | Chat shall be limited to accepted circle members by default | Must | `ensureCanMessage` | `ChatServiceImplCircleGateTest` |
| SR-10 | Uploads shall be validated by content and size | Must | `MediaContentTypes`, `MediaValidation` | `MediaValidationTest` |
| SR-11 | Email content shall escape user-provided text | Must | `WishEmailExtras.escape`, `escapeHtml` | `WishEmailExtrasTest` |
| SR-12 | OTP and password-reset emails shall be readable only by admins | Must | Sensitive `EmailType` filter | `GmailEmailServiceImplTest` |
| SR-13 | Secrets shall not be stored in the repository | Must | Environment variables | gitleaks (recommended), review |
| SR-14 | External traffic shall use TLS (except the IP lookup) | Must | Render TLS, HTTPS provider URLs | Deployment review |
| SR-15 | Tokens shall never be written to logs | Must | Code review | Log review |
| SR-16 | Media objects shall not be publicly enumerable | Should | Private bucket, presigned URLs, UUID keys | Manual check |
| SR-17 | Error responses shall not reveal stack traces or SQL | Should | `GlobalExceptionHandler` | SEC-DATA-02 |
| SR-18 | Failed sign-in attempts shall be recorded for analysis | Should | `violated_users` | `AuthServiceImplTest` |
| SR-19 | An admin shall be able to impersonate a member for support | Could | `adminLoginAsUser`, `switch-back` | `AuthServiceImplTest` |
| SR-20 | The application shall expose a health endpoint for monitoring | Must | `/health` | UptimeRobot |
| SR-21 | Dependency vulnerabilities shall be tracked and remediated | Should | Process | [vulnerability-management.md](vulnerability-management.md) |
| SR-22 | Personal data shall be limited to what the product needs | Must | Schema review | [privacy-and-data-retention.md](privacy-and-data-retention.md) |
| SR-23 | Users shall be able to deactivate an account | Should | Soft delete | TC-USER-022 |
| SR-24 | The system shall not log Confidential or Restricted data | Must | Review | Log review |
| SR-25 | CORS shall allow only known UI origins | Must | `SecurityConfig`, `WebSocketConfig` | SEC-CONF-03/04 |

## 1. Compliance context

The product stores personal data for a single family group. No formal regime (GDPR, HIPAA, PCI) is
declared, but these principles are applied as good practice:

| Principle | Application |
| --- | --- |
| Lawful basis and consent | Accounts are created by the family administrator; members can disable wishes and leave circles |
| Data minimisation | Only name, email, birthday, relationship, optional picture and wish preferences |
| Purpose limitation | Data is used for greetings, calendars and chat only |
| Storage limitation | Soft delete exists; hard deletion is manual |
| Integrity and confidentiality | TLS, hashing, access control, private storage |
| Accountability | Audit tables and this documentation |

## 2. Gaps against these requirements

| Requirement | Gap | Priority |
| --- | --- | --- |
| SR-01, SR-24 | `violated_users` stores clear-text passwords | High |
| SR-07 | Scheduler trigger lacks the admin annotation (DEF-005) | High |
| SR-17 | The fallback handler returns `ex.getMessage()` | Medium |
| SR-21 | No automated dependency scanning | Medium |
| SR-23 | No self-service account deletion; only soft delete by an admin | Medium |
| SR-24 | A few `System.out.println` calls remain | Low |
| SR-14 | ip-api.com is queried over plain HTTP | Low |

## 3. Verification approach

| Requirement group | How verified |
| --- | --- |
| Authentication (SR-01…06) | Unit tests plus SEC-AUTH cases |
| Authorisation (SR-07…09) | SEC-AUTHZ cases |
| Input and upload (SR-10, SR-11, SR-16) | Unit tests plus SEC-FILE and SEC-INPUT |
| Data visibility (SR-12, SR-24) | SEC-DATA cases |
| Configuration (SR-13, SR-14, SR-25) | SEC-CONF cases |
| Operations (SR-20, SR-21) | Monitoring check and dependency review |

## 4. Ownership

| Area | Owner |
| --- | --- |
| Authentication and sessions | Backend owner |
| Authorisation matrix | Backend owner |
| Secrets and rotation | Operator |
| Dependency updates | Backend owner |
| Incident response | Operator ([../operations/incident-management.md](../operations/incident-management.md)) |
