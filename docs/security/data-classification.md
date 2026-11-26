# Data classification

## 1. Levels

| Level | Definition | Handling |
| --- | --- | --- |
| **Restricted** | Compromise enables account takeover or reveals secrets | Never logged, never returned, encrypted in transit, access restricted |
| **Confidential** | Personal or private content | Returned only to the owner or an admin; not in logs |
| **Internal** | Operational data with limited sensitivity | Admin-only where practical |
| **Public** | Safe to expose | No restriction |

## 2. Classification by data item

| Data | Level | Notes |
| --- | --- | --- |
| Passwords (hash) | Restricted | BCrypt; never returned |
| `violated_users.password` (clear text) | **Restricted — defect** | Must be removed or hashed |
| Access tokens | Restricted | Short-lived |
| Refresh tokens | Restricted | Stored in the database, revocable |
| OTP codes | Restricted | 5-minute life, single use |
| Password-reset tokens | Restricted | 30-minute life, single use |
| Direct-login tokens | Restricted | 60-minute life, single use |
| Gmail refresh token, Gemini key, storage keys, database passwords | Restricted | Environment only |
| Names, emails, birthdays, relationships, gender | Confidential | Personal data |
| Profile pictures | Confidential | Private bucket, presigned URLs |
| Wish settings, MFA flags | Confidential | Owner or admin |
| Events (dates, types) | Confidential | Owner, circle members (calendar), admin |
| Wishes URLs | Confidential | May reveal a personal page |
| Email bodies and subjects | Confidential | Owner or admin; OTP/reset/circle bodies admin-only |
| Chat messages and attachments | Confidential | Participants only |
| `login_location_events` (IP, coordinates) | Confidential | Owner or admin |
| `violated_users` (email, IP, location) | Confidential | Admin only |
| Festival data | Public | Derived from Calendarific |
| Themes, seeds | Public/Internal | Reference data |
| Dashboard counters | Internal | Authenticated users see scoped counters |
| Scheduler statistics | Internal | Authenticated |
| Instagram message log | Internal | Admin dashboards |
| Application logs | Internal | Must not contain Restricted or Confidential values |

## 3. Where each class lives

| Store | Data classes present |
| --- | --- |
| Primary database | Business data (Restricted to Confidential) |
| Chat database | Confidential messages and attachment metadata |
| Supabase Storage | Confidential media |
| `email_logs` | Confidential bodies |
| Application logs | Internal only (by design) |
| Backups | Everything — treat as Confidential/Restricted |
| Browser storage (UI) | Restricted tokens — outside this repository |

## 4. Handling rules

### Restricted

1. Never log and never include in error messages.
2. Never return in a response (except the token the caller just earned).
3. Store only where necessary (hashes, or short-lived rows).
4. Rotate on suspicion.

### Confidential

1. Return only to the owner or an admin.
2. Do not log bodies or addresses at INFO level.
3. Use presigned URLs for media; never make the bucket public.
4. Exclude from dashboards unless aggregated.

### Internal

1. Authenticated access is acceptable.
2. Admin-only where it reveals system behaviour (violated users, scheduler details).

### Public

1. No authentication needed.
2. Still validate input.

## 5. Data flows and classification

| Flow | Data | Class |
| --- | --- | --- |
| Sign-in | email, password | Restricted |
| Token issue | tokens | Restricted |
| Calendar read | names, birthdays, event dates | Confidential |
| Directory search | names, emails, relationships | Confidential |
| Chat send | message text, attachment | Confidential |
| Wish send | name, relationship, generated text, image | Confidential |
| Dashboard read | aggregated counters | Internal |
| Festival read | festival names and dates | Public |
| Health check | status and time | Public |

## 6. Classification-driven controls

| Control | Applies to |
| --- | --- |
| Owner-or-admin checks | Confidential |
| Admin-only endpoints | Internal sensitive (violations, OTP dashboards) |
| Never log | Restricted |
| Presigned URLs | Confidential media |
| Sensitive email types | Confidential bodies (OTP, reset, circle) |
| Redaction in agent tools | Confidential (recipient addresses withheld) |
| Backup encryption | Everything |

## 7. Review triggers

Re-classify when:

1. A new field is added to an entity.
2. A new endpoint returns existing data to a new audience.
3. A new integration receives data (for example a provider that needs names).
4. A retention or deletion policy changes.

## 8. Quick reference for developers

| Question | Answer |
| --- | --- |
| Can I log this value? | Only Internal. Never Restricted or Confidential. |
| Can I return this to any authenticated user? | Only Internal/Public. |
| Can I store this in the repository? | Only Public/Internal placeholders. |
| Can I put this in a URL query string? | Avoid for Confidential; never for Restricted. |
| Can I put this in an email body? | Confidential only when the recipient is the owner. |
| Can I send this to an AI provider? | Only what the prompt needs (name, relationship, occasion); never emails, tokens or extra identifiers. |
