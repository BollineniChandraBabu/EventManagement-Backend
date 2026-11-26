# Secrets management

## 1. Principles

1. **No secret in the repository.** Configuration files hold placeholders or environment references
   only.
2. **One secret per environment.** Never reuse production credentials in local or QA.
3. **Least privilege.** The Gmail account can send only; the storage key can touch one bucket.
4. **Rotate on suspicion.** A leaked token is rotated immediately, not "later".
5. **Never log secrets.** Tokens, passwords and OTPs are excluded from logs.

## 2. Secret inventory

| Secret | Used by | Sensitivity | Rotation |
| --- | --- | --- | --- |
| `JWT_SECRET` | Token signing | Critical | On suspicion; invalidates sessions |
| `DB_PASSWORD` | Primary database | Critical | Quarterly or on suspicion |
| `SUPABASE_DB_PASSWORD` | Chat database | Critical | Quarterly |
| `SUPABASE_STORAGE_SECRET_ACCESS_KEY` | Storage | Critical | Quarterly |
| `GMAIL_CLIENT_SECRET` | Gmail OAuth | High | Yearly |
| `GMAIL_REFRESH_TOKEN` | Gmail sending | High | On revocation |
| `MAIL_PASSWORD` | SMTP | High | On suspicion |
| `GEMINI_API_KEY` | AI | High | On suspicion |
| `CALENDARIFIC_API_KEY` | Festivals | Medium | Yearly |
| `POLLINATIONS_IMAGE_KEY` | Images | Medium | Yearly |
| `INSTAGRAM_PAGE_ACCESS_TOKEN` | Instagram | Medium | On expiry |
| `INSTA_VERIFY_TOKEN` | Webhook verification | Low | Yearly |
| `BREVO_API_KEY` | Alternative email | Medium | Yearly |

## 3. Where secrets live

| Environment | Storage | Access |
| --- | --- | --- |
| Local | `.env` file (add it to `.gitignore` — it is **not** ignored today) or shell profile | Developer |
| CI | Repository or organisation secrets | Pipeline only |
| Production | Render environment variables | Operators |

`.gitignore` currently ignores `.gradle/`, `build/`, `.idea/` and the Gradle wrapper. **Add `.env`**
before creating one.

## 4. Setting secrets in Render

1. Open the service → *Environment*.
2. Add each variable with its production value.
3. Save; Render restarts the service.
4. Verify with `/health` and a feature smoke test (for example `POST /api/email/test`).

Use *Secret Files* only if a value must be a file; the application reads environment variables.

## 5. Handling a leaked secret

| Secret | Immediate action |
| --- | --- |
| `JWT_SECRET` | Rotate, restart (all sessions end), force a password reset if account takeover is suspected |
| Database password | Rotate at the provider, update Render, restart |
| Gmail refresh token | Revoke access in the Google account, re-authorise, update |
| Storage keys | Revoke the key pair in Supabase, create new keys, update |
| Gemini key | Delete the key in AI Studio, create a new one, update |
| Any secret | Review logs and access records for misuse; record the incident |

## 6. Rotation procedure

1. Create the new credential alongside the old one where the provider allows it.
2. Update the environment variable.
3. Restart the service.
4. Verify the feature that uses the credential.
5. Revoke the old credential.
6. Record the rotation date.

## 7. What is *not* secret but must be protected

| Item | Why |
| --- | --- |
| `violated_users` table | Contains attempted passwords in clear text |
| `email_logs.body` | Contains personal messages |
| `login_location_events` | Personal location and IP data |
| Chat attachments | Personal media |
| Database backups | Everything above |

These need access control, not encryption keys, but treat them as confidential.

## 8. Developer rules

1. Never paste a secret into a chat, an issue or a commit message.
2. Use a password manager or the platform store to share credentials.
3. Do not reuse your personal Google password for SMTP; use an app password.
4. Do not commit `.env`, `application-local.properties` or key files.
5. If you accidentally commit a secret, treat it as leaked and rotate immediately — removing the
   commit is not enough.

## 9. Verification checklist

- [ ] `git log -p` contains no secret (spot-check with `gitleaks`)
- [ ] `.env` is not tracked (`git check-ignore .env` after adding the rule)
- [ ] Production variables are set in Render, not in the repository
- [ ] The storage key is scoped to the bucket
- [ ] The Gmail account uses an app password and the `gmail.send` scope only
- [ ] No secret appears in logs (`grep -i "token\|password\|secret"` on a sample log)

## 10. Recommended improvements

1. Add `.env` to `.gitignore` and commit a `.env.example` with placeholders.
2. Run `gitleaks` in CI.
3. Store secrets in a managed vault if the team grows.
4. Add a rotation calendar with owners.
