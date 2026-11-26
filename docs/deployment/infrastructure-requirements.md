# Infrastructure requirements

## 1. Compute

| Environment | CPU | Memory | Disk | Notes |
| --- | --- | --- | --- | --- |
| Local development | 2 cores | 4 GB | 10 GB | IDE plus database |
| QA/UAT | 1 vCPU | 1 GB | 10 GB | Same image as production |
| Production (Render) | 1 vCPU (shared) | 512 MB–2 GB | ephemeral | Free tier sleeps when idle |

Memory consumers: JVM heap (default 25% of container memory), Hikari pools, Ehcache
(`emailImages` 30 entries is the largest single item), and the AI/HTTP client stacks.

Recommended production: **1 vCPU / 1 GB** minimum for predictable wish runs; 2 GB if email images
are large.

## 2. Runtime

| Requirement | Value |
| --- | --- |
| Java | 17 (JRE is enough at runtime) |
| Container base | `eclipse-temurin:17-jre-alpine` |
| Build base | `gradle:8.14.3-jdk17-alpine` |
| Port | 8080 |
| File system | No persistent state required |
| Time zone | Asia/Kolkata for scheduling (container default is UTC; the app sets the zone) |

## 3. Databases

### Primary (Neon)

| Item | Requirement |
| --- | --- |
| Engine | PostgreSQL 14+ |
| Storage | < 1 GB for years of family data |
| Connections | 10 (Hikari) plus Quartz |
| Backups | Daily point-in-time where available |
| Extensions | None required |
| TLS | `sslmode=require` in the JDBC URL |

### Chat (Supabase)

| Item | Requirement |
| --- | --- |
| Engine | PostgreSQL 14+ |
| Storage | Grows with message volume; tens of MB per year at family scale |
| Connections | 10 (Hikari) |
| Schema | Created by the application |

## 4. Object storage

| Item | Requirement |
| --- | --- |
| Service | Supabase Storage (S3-compatible) |
| Bucket | `golden-greetings` |
| Region | `ap-southeast-1` (default) |
| Capacity | < 1 GB per year with gzip |
| Permissions | PutObject, GetObject, DeleteObject |
| Signed URLs | Required for private objects |

## 5. Network

| Flow | Direction | Port | Notes |
| --- | --- | --- | --- |
| Clients → API | inbound | 443 | TLS terminated at Render |
| Clients → WebSocket | inbound | 443 (WSS) | `/ws-chat` |
| Monitor → `/health` | inbound | 443 | UptimeRobot |
| API → primary DB | outbound | 5432 | Neon |
| API → chat DB | outbound | 5432 | Supabase |
| API → storage | outbound | 443 | Supabase S3 |
| API → Gmail | outbound | 443 | Google |
| API → Gemini | outbound | 443 | Google |
| API → Pollinations | outbound | 443 | |
| API → Calendarific | outbound | 443 | |
| API → ip-api.com | outbound | 80 | Plain HTTP |

Firewall requirements: outbound HTTPS (443), outbound PostgreSQL (5432) to the two database hosts,
outbound HTTP (80) only for ip-api.com.

## 6. Third-party quotas

| Service | Free-tier limit | Mitigation |
| --- | --- | --- |
| Gmail API | Per-account quota | Retry cap, one email per event |
| Gemini | Per-model rate limits | Model fallback, cooldown |
| Pollinations | Plan-based | Best effort only |
| Calendarific | Free plan quota | Monthly sync plus on-demand years |
| ip-api.com | 45 requests/minute | 24-hour cache |
| Supabase | Storage and database plan limits | gzip, small objects |
| Render | Free tier sleeps | Paid instance or keep-alive |

## 7. Monitoring and operations

| Item | Requirement |
| --- | --- |
| Uptime monitoring | UptimeRobot on `/health`, 5-minute interval |
| Log retention | Render default; export if longer is needed |
| Alerting | UptimeRobot email; application alert emails |
| Access control | Render team members; Neon and Supabase project access |

## 8. Sizing scenarios

| Scenario | Users | Events | Emails/month | Recommendation |
| --- | --- | --- | --- | --- |
| Small family | ≤ 20 | ≤ 60 | ≤ 600 | 1 vCPU / 512 MB, free tiers |
| Extended family | ≤ 100 | ≤ 300 | ≤ 3 000 | 1 vCPU / 1 GB, paid database tier |
| Multi-family (future) | > 100 | > 300 | > 3 000 | Multi-tenant redesign required |

## 9. Disaster-recovery requirements

| Metric | Target |
| --- | --- |
| RPO (data loss tolerated) | 24 hours (daily backup) |
| RTO (time to restore) | 2 hours |
| Backup retention | 30 days |
| Restore test | Quarterly |

See [../operations/disaster-recovery.md](../operations/disaster-recovery.md).

## 10. Non-functional infrastructure constraints

1. Single instance today: no load balancer configuration is needed.
2. Stateless application: horizontal scaling is possible once rate limiting and caching are shared.
3. No local disk state: containers can be replaced at any time.
4. All secrets from environment variables.
5. Outbound internet access is required for email, AI, festivals and storage.
