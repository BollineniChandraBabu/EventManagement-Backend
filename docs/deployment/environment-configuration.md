# Environment configuration

## 1. Environments

| Environment | Purpose | Data | Who uses it |
| --- | --- | --- | --- |
| Local | Development | Local PostgreSQL, test bucket | Developers |
| QA (optional) | Integration and E2E | Dedicated database, mailbox, bucket | QA and release owner |
| UAT (optional) | Business acceptance | Copy of production shape, fake people | Product owner |
| Production | Live service | Real data | Family members |

Today only **local** and **production** exist as configured artefacts
(`application.properties`, `application-prod.properties`). QA and UAT, if created, reuse the `prod`
profile with different environment variables.

## 2. Differences that matter

| Setting | Local | Production |
| --- | --- | --- |
| Profile | default | `prod` |
| `server.port` | 8080 | 8080 (behind Render TLS) |
| Database | local PostgreSQL | Neon |
| Chat database | local or shared PostgreSQL | Supabase |
| Storage bucket | test bucket | production bucket |
| Mail | Gmail API (same account) or SMTP | Gmail API |
| `UI_BASE_URL` | `http://localhost:4200` | `https://chandu-goldengreetings.web.app` |
| Schedulers | running (careful) | running |
| TLS | none | Render |
| Logging | INFO for the app | INFO for the app |
| Rate limiting | enabled | enabled |
| Agents | enabled (same defaults) | enabled (same defaults) |

## 3. Variable matrix

| Variable | Local | QA/UAT | Production |
| --- | --- | --- | --- |
| `DB_URL` | local | QA database | Neon |
| `DB_USERNAME` / `DB_PASSWORD` | local | QA credentials | Neon credentials |
| `JWT_SECRET` | any 32+ chars | random 32+ | random 32+, stored as a secret |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | your account | shared test account | production account |
| `GMAIL_*` | dev OAuth client | dev or shared | production OAuth client |
| `GEMINI_API_KEY` / `GEMINI_API_URL` | dev key | shared key | production key |
| `SUPABASE_*` | test project | test project | production project |
| `CALENDARIFIC_API_KEY` | optional | yes | yes |
| `UI_BASE_URL` | localhost | QA UI URL | production UI URL |
| `AGENT_*` | as needed | as needed | as needed (documented defaults are `true`) |
| `APP_CHAT_CIRCLES_ONLY` | `true` | `true` | `true` |

## 4. Secrets per environment

| Environment | Storage |
| --- | --- |
| Local | `.env` file (add it to `.gitignore` first) or your shell profile |
| QA/UAT | Platform secret store (Render environment group) |
| Production | Render environment variables, restricted access |

Never reuse production credentials in a non-production environment: a mistake there would send real
email or write real data.

## 5. Feature flags by environment

| Flag | Local suggestion | Production |
| --- | --- | --- |
| `APP_RATE_LIMIT_ENABLED` | `false` while testing | `true` |
| `AGENT_WISH_ENABLED` | `false` to save quota | `true` |
| `AGENT_DELIVERY_OPS_ENABLED` | `false` | `true` (or `dry-run=true` initially) |
| `AGENT_DELIVERY_OPS_DRY_RUN` | `true` | `false` |
| `AGENT_COPILOT_ENABLED` | `false` | `true` |
| `APP_CHAT_CIRCLES_ONLY` | `true` | `true` |
| `APP_MEDIA_GZIP_ENABLED` | `true` | `true` |

## 6. Promoting a change

```
local → (QA/UAT) → production
```

| Step | Check |
| --- | --- |
| Build once | The same jar/image is promoted, not rebuilt |
| Configure per environment | Only environment variables change |
| Database | Apply schema changes first (Hibernate does it at startup) |
| Smoke test | `/health`, sign-in, one affected feature |
| Rollback ready | Previous image tag recorded |

## 7. Configuration drift checks

| Check | Frequency |
| --- | --- |
| Compare production env vars with the documented list | Each release |
| Confirm `UI_BASE_URL` matches the deployed UI | Each release |
| Confirm the Gmail refresh token still works | Monthly and after a failure |
| Confirm the storage bucket name and region | Each release |
| Confirm agent flags match the intended state | Each release |

## 8. Notes

* `application-prod.properties` repeats many values; a change in `application.properties` may be
  overridden by `prod`. Check both before concluding a setting has no effect.
* Values in the repository are placeholders or non-secrets; every real secret comes from the
  environment.
