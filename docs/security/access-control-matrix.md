# Access control matrix

Legend: **A** = admin only, **U** = any authenticated user (ownership rules apply), **O** = owner or
admin, **P** = participant (circle/chat), **–** = public (no token).

## 1. Authentication

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `POST /api/auth/login` | – | – | – |
| `POST /api/auth/refresh` | – | – | – |
| `POST /api/auth/otp/send`, `/otp/verify`, `/otp/verify-login` | – | – | – |
| `POST /api/auth/forgot-password`, `/password-reset/confirm` | – | – | – |
| `POST /api/auth/sso/google`, `GET /sso/token` | – | – | – |
| `POST /api/auth/direct-login` | – | – | – |
| `POST /api/auth/change-password` | | U | U |
| `GET /api/auth/admin/switch-back` | | U (impersonated only) | U |
| `POST /api/auth/admin/login-as-user` | | | A |

## 2. Users

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `POST /api/users` | | | A |
| `PUT /api/users` | | | A |
| `GET /api/users` | | | A |
| `GET /api/users/filter-values` | | | A |
| `GET /api/users/{id}` | | U (full profile — see DEF-006) | U |
| `GET /api/users/directory` | | U | U |
| `GET /api/users/me` | | O | O |
| `PATCH /api/users/me/*` (wish settings, MFA, concurrent login) | | O | O |
| `PATCH /api/users/{id}/concurrent-login` | | O (self) | A |
| `GET /api/users/me/wish-preview` | | O | O |
| `POST/DELETE /api/users/me/profile-picture*` | | O | O |
| `POST /api/users/{id}/deactivate`, `DELETE /api/users` | | | A |
| `PATCH /api/users/{id}/status` | | | A |

## 3. Events

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `POST/PUT/DELETE /api/events*` | | | A |
| `GET /api/events` | | O (own only) | A (all) |
| `GET /api/events/{id}` | | O | A |
| `GET /api/events/filter-values` | | U | U |

## 4. Festivals and calendar

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `GET /api/festivals` | | U | U |
| `GET /api/calendar` | | O plus circle members | A (all) |
| `POST/PUT/DELETE /api/festival-wish-mappings*` | | | A |
| `GET /api/festival-wish-mappings` | | | A |
| `GET /api/festival-wish-mappings/me` | | O | O |
| `GET /api/festival-wish-mappings/summary`, `/filter-values` | | | A |

## 5. Circles and chat

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `GET /api/circles` | | P | P |
| `POST /api/circles/requests` | | U (not self) | U |
| `POST /requests/accept`, `/requests/reject` | | addressee only | addressee only |
| `DELETE /api/circles` | | P | P |
| `GET /api/circles/status/{userId}` | | U | U |
| `POST /api/chat` | | P (circle gate) | P |
| `GET /api/chat/messages/{otherUserId}` | | P | P |
| `GET /api/chat/messages/{id}/attachment` | | P | P |
| `PATCH /api/chat/messages/{id}` | | sender | sender |
| `DELETE /api/chat/messages*` | | P | P |
| `GET/POST/DELETE /api/chat/messages/{id}/reactions`, `/like` | | P | P |
| `POST /api/chat/presence/*` | | U | U |
| `GET /api/chat/unread-count`, `/conversations`, `/users`, `/users/active` | | U | U |
| `GET /api/chat/messages/all` | | U (own conversations) | U |
| `/ws-chat` (STOMP) | – (unauthenticated — DEF-004) | – | – |

## 6. Notifications

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `GET /api/notifications/published` | – | – | – |
| `POST/PUT/DELETE /api/notifications*` | | | A |
| `POST /api/notifications/{id}/publish`, `/unpublish` | | | A |
| `GET /api/notifications`, `/{id}`, `/filter-values` | | | A |

## 7. Emails

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `POST /api/email/test` | | U | U |
| `POST /api/email/send-now` | | | A |
| `GET /api/emails/status` | | O (non-sensitive) | A (all) |
| `GET /api/emails/status/{id}` | | O (non-sensitive) | A |
| `GET /api/emails/status/{id}/image` | | O (non-sensitive) | A |
| `GET /api/emails/status/admin/otp`, `/admin/forgot-password` | | | A |
| `GET /api/emails/status/festival-wishes` | | O | A |
| `GET /api/emails/status/unread-chat-messages` | | O | A |
| `DELETE /api/emails/status*` | | | A |
| `GET /api/emails/filter-values` | | U | U |

## 8. Dashboards

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `GET /api/dashboard/mail`, `/insta`, `/chart`, `/chart/mail`, `/chart/insta` | | U | U |
| `GET /api/dashboard/chart/login-locations` | | O (own) | A (all) |
| `GET /api/dashboard/mail/otp`, `/mail/forgot-password` | | | A |
| `GET /api/dashboard/chart/mail/otp`, `/chart/mail/forgot-password` | | | A |
| `GET /api/dashboard/violated-users` | | | A |
| `GET /api/dashboard/chat/messages`, `/chat/users/active` | | U | U |

## 9. Content and administration

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `GET /api/themes`, `/me`; `PUT /api/themes/me` | | U | U |
| `/api/seed/event-types`, `/relationships`, `/template-types`, `/special-events` (reads) | | U or A | A |
| Same, writes | | | A |
| `/api/admin/wish-images` (all) | | | A |
| `POST /api/ai/generate-wish`, `GET /api/ai/pollinations/balance` | | U | U |
| `/api/agent/**` | | | A |
| `GET /api/schedulers`, `/api/schedulers/{job}` | | U | U |
| `POST /api/schedulers/{job}/trigger` | | U (DEF-005) | U |

## 10. Platform

| Endpoint | Public | Member | Admin |
| --- | --- | --- | --- |
| `GET /health` | – | – | – |
| `/v3/api-docs`, `/swagger-ui.html` | – | – | – |
| `/actuator/**` | – (DEF-002) | – | – |
| `/google/**`, `/oauth2callback`, `/gmail/**` | – (DEF-003) | – | – |
| `/instagram/**` | – | – | – |

## 11. Ownership rules in detail

| Resource | Rule | Failure |
| --- | --- | --- |
| Event read | `isAdmin || event.user.email == caller` | 404 |
| Event list | Admin: all; member: own | — |
| Email status | Admin: all; member: own address and non-sensitive type | 403 |
| Login location | Admin: any user; member: self | 403 |
| Chat message | Caller is sender or receiver | 403/404 |
| Chat attachment | Caller participates in the conversation | 403/404 |
| Circle response | Caller is the addressee | 400 |
| Circle removal | Caller is requester or addressee | 404 |
| Theme preference | Caller only | — |
| Concurrent-login flag | Admin or self | 403 |
