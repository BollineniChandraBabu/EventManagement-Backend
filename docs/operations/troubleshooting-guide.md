# Troubleshooting guide

## 1. The service is down

| Check | Command | Then |
| --- | --- | --- |
| Is it listening? | `curl -i $BASE/health` | No response → Render logs |
| Did it crash on start? | Render logs | Look for the last exception before exit |
| Database reachable? | `psql "$DB_URL" -c 'select 1'` | If not, check Neon status and credentials |
| Missing secret? | Logs mention `JWT_SECRET` or a property | Set the variable and restart |
| Out of memory? | Logs show `OutOfMemoryError` | Raise the instance memory |
| Port conflict (local) | `lsof -i :8080` | Stop the other process |

## 2. Sign-in problems

| Symptom | Cause | Fix |
| --- | --- | --- |
| `404 User not found` | Unknown email or deleted user | Check the user exists and `deleted=0` |
| `400 Invalid email or password` | Wrong password | Reset it |
| Lockout message | 5 failures | Reset `failed_login_attempts` in SQL or wait for an admin reset |
| `MFA_OTP_REQUIRED` | MFA on | Complete the OTP step |
| `401 CONCURRENT_LOGIN:` | Single-session user signed in elsewhere | Sign in again |
| 429 on login | Rate limit | Wait 60 seconds |
| SSO fails | Wrong client id or unverified email | Check `GOOGLE_SSO_CLIENT_ID` |
| Direct-login link fails | Token used or expired | Send a new circle request |

Reset a lockout:

```sql
UPDATE users SET failed_login_attempts = 0 WHERE email = 'user@example.com';
```

## 3. Emails are not being sent

| Check | How | Meaning |
| --- | --- | --- |
| Are rows created? | `GET /api/emails/status?page=0&size=5` | No rows → the send never started |
| Status | Look at `status` | `FAILED` → read `error_message` |
| Gmail token | Logs contain `invalid_grant` | Re-authorise (runbook §5) |
| Gmail quota | Logs contain 429/403 from Gmail | Wait or use another account |
| AI failure | Logs contain `AI service is busy` | Fix the Gemini key or quota |
| Scheduler ran? | `GET /api/schedulers` | Not run → trigger manually |
| Duplicate guard | Logs say "Already sent" | Expected behaviour |

Common messages:

| Message | Action |
| --- | --- |
| `invalid_grant` | Refresh token revoked; re-authorise |
| `401 Unauthorized` from Gmail | Client id/secret wrong |
| `insufficientPermissions` | The refresh token lacks `gmail.send` |
| `Mail send failed` with a timeout | Provider latency; retry |
| `AI response missing` | Gemini returned an unexpected body |

## 4. Wishes are missing or duplicated

| Symptom | Cause | Fix |
| --- | --- | --- |
| No birthday wish | `birthdayEnabled=false`, or already sent today | Check settings and `last_birthday_wish_sent` |
| No event wish | Event inactive, wrong date, non-recurring in a past year | Check the event |
| No festival wish | Mapping inactive, already sent, festival date wrong | Check the mapping and the seed date |
| Duplicate wish | Two instances running, or a manual trigger after a successful run | Confirm a single instance |
| Wish text is generic | AI unavailable, fallback used | Check the Gemini logs |

Inspect the guards:

```sql
SELECT email, birthday, last_birthday_wish_sent FROM users WHERE birthday IS NOT NULL;
SELECT id, special_event_id, user_id, last_wish_sent_on, is_error FROM festival_wish_mappings;
```

## 5. AI problems

| Symptom | Cause | Fix |
| --- | --- | --- |
| `503 The AI service is busy right now` | All models failed | Check the key and quota; retry later |
| `Gemini model X failed (HTTP 429)` | Rate limit | Normal; the fallback handles it |
| `Could not list Gemini models` | Catalogue call failed | The previous list is kept for 1 minute |
| Slow wishes | Provider latency | Expected; check the run duration |
| Placeholder text in a wish | Agent validator bypassed or disabled | Enable `AGENT_WISH_ENABLED` |
| Images missing | Pollinations failure | Expected; upload wish images for key occasions |

## 6. Chat problems

| Symptom | Cause | Fix |
| --- | --- | --- |
| `You can only message users in your circle` | Circles-only gate | Accept the circle first |
| Chat tables missing | Chat datasource not configured | Check `SUPABASE_DB_*`; restart |
| Messages not appearing live | WebSocket blocked | Check `/ws-chat` reachability and the ingress |
| Attachment download fails | Object missing or wrong content type | Check storage and the message row |
| Unread count stuck | `seen_at` not updated | Confirm `markSeen=true` on read |
| Everyone offline | Heartbeats not sent by the UI | Expected if the UI does not call the endpoint |

## 7. Calendar and festival problems

| Symptom | Cause | Fix |
| --- | --- | --- |
| Festivals empty for a month | No rows for the year | Trigger the sync or request that year |
| Festival dates wrong | Old year's data | Request the correct `year` |
| Circle member's dates missing | Circle not accepted | Check the circle status |
| Duplicate entries | Merge by id should prevent it | Report a defect with the ids |

## 8. Uploads fail

| Symptom | Cause | Fix |
| --- | --- | --- |
| 400 with the media message | Type mismatch or too large | Check the file and the limits |
| 413 | Servlet limit (50 MB) | Reduce the file |
| 400 `Unable to upload image to storage` | Storage credentials or bucket | Check the Supabase keys |
| Image not visible | Object key wrong or bucket private without a URL | Check `getSignedUrl` and the public base URL |

## 9. Dashboards look wrong

| Symptom | Cause | Fix |
| --- | --- | --- |
| Counters stale | 60-second cache | Wait |
| Empty charts | No data in the range | Choose a wider range |
| `status=fail` on map points | ip-api.com rate limit | Wait; results are cached when successful |
| 400 on dates | Validation rules | Use `yyyy-MM-dd`, end ≤ today, start < end |

## 10. Deployment problems

| Symptom | Cause | Fix |
| --- | --- | --- |
| Build fails at `COPY` | Jar name changed | Update the `Dockerfile` |
| Build fails at compile | Code error or JDK mismatch | Reproduce locally |
| Starts then exits | Missing env var | Read the startup exception |
| Slow first request | Free tier slept | Expected; use a paid instance |
| Schema error on start | Bad entity change | Roll back ([../deployment/rollback-plan.md](../deployment/rollback-plan.md)) |

## 11. Where to look first

1. Render logs (the last 200 lines around the failure).
2. `email_logs` for delivery issues.
3. `GET /api/schedulers` for job issues.
4. Neon/Supabase dashboards for database issues.
5. Provider dashboards (Google, Gemini, Calendarific) for quota issues.
