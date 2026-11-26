# Component diagram

## 1. Component view

```
┌──────────────────────────── Clients ────────────────────────────┐
│ Angular SPA   ·   UptimeRobot   ·   Instagram webhook   ·   Mail │
└───────┬─────────────────┬──────────────────┬────────────────────┘
        │ REST/STOMP      │ GET /health      │ GET/POST /instagram
        ▼                 ▼                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Edge components                              │
│  RateLimitFilter ──► JwtAuthFilter ──► SecurityConfig ──► Dispatcher│
│  GlobalExceptionHandler  ·  CORS (AppConfig)  ·  Compression        │
└───────┬─────────────────────────────────────────────────────────────┘
        │
        ├──► AuthController ──► AuthService ──────────────┐
        ├──► UserController ──► UserService ──────────┐   │
        ├──► EventController ─► EventService ─────┐   │   │
        ├──► FestivalController ─► FestivalService│   │   │
        ├──► FestivalWishMappingController ─► FestivalWishMappingService
        ├──► CalendarController ─► CalendarService ──► (Festival, Event, User, Circle)
        ├──► CircleController ──► CircleService ──► CircleRequestNotifier
        ├──► ChatController ────► ChatService ──► ChatMessageRepository (chat DB)
        │                                    ├──► CircleService (gate)
        │                                    └──► ChatRealtimePublisher ──► /topic
        ├──► NotificationController ─► NotificationService ──► GmailEmailService
        ├──► EmailController ─► GmailEmailService ──► EmailImageService / Storage
        ├──► DashboardController ─► DashboardService ──► IpInfoService (ip-api.com)
        ├──► ThemeController ─► ThemeService
        ├──► Seed controllers ─► *SeedService ─► repositories
        ├──► WishImageAdminController ─► WishImageService ─► Storage
        ├──► AiController ─► AiService (AgenticAiService → AiServiceImpl) ─► GeminiClient
        ├──► AgentController ─► AdminCopilotAgent / DeliveryOpsAgent ─► AgentRunner
        ├──► SchedulerController ─► SchedulerManagementService ─► Quartz
        └──► HealthController (no service)

┌──────────────────────────── Cross-cutting ──────────────────────────┐
│ JwtService · RateLimiter · AppUserDetails · CustomUserDetailsService │
│ EmailTemplateBuilder · MediaValidation · MediaContentTypes · GzipUtil│
│ WishesUrls · WishImageKeys · WishEmailExtras · CacheNames · Ehcache  │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────── Scheduling ──────────────────────────────┐
│ Quartz: WishesScheduler, GoodMorningScheduler, BirthdayMailScheduler, │
│ FestivalScheduler, GoodNightScheduler, RefreshTokenCleanupScheduler,  │
│ FestivalSyncScheduler, UnreadChatEmailScheduler,                     │
│ CircleRequestReminderScheduler                                        │
│ Spring: BirthdayScheduler, RetryScheduler, EmailAlertScheduler,      │
│ DeliveryOpsAgentScheduler                                            │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────── External systems ────────────────────────────┐
│ Neon PostgreSQL (primary) · Supabase PostgreSQL (chat) ·             │
│ Supabase Storage (S3) · Gmail API · SMTP · Brevo · Gemini ·          │
│ Pollinations · Calendarific · ip-api.com · Google Identity           │
└──────────────────────────────────────────────────────────────────────┘
```

## 2. Component dependencies (allowed direction)

| Component | Depends on | Must not depend on |
| --- | --- | --- |
| Controllers | service interfaces, DTOs, security helpers | repositories, entities |
| `AuthServiceImpl` | user/token/security repositories, `JwtService`, mail, chat unread, audit | controllers, schedulers |
| `UserServiceImpl` | user/settings/theme/event/mapping repositories, storage, AI | chat SQL |
| `EventServiceImpl` | event/type/user repositories, storage | AI |
| `FestivalServiceImpl` | special-event repository, `RestTemplate` | schedulers |
| `CalendarServiceImpl` | festival, event, user, circle services | repositories |
| `CircleServiceImpl` | circle/user repositories, notifier, storage | chat |
| `ChatServiceImpl` | chat SQL repository, user repository, storage, circle service, publisher | JPA chat entities (none exist) |
| `NotificationServiceImpl` | notification/user repositories, mail, publisher | schedulers |
| `GmailEmailServiceImpl` | email-log/user/type repositories, Gmail client, storage, image cache | controllers |
| `AiServiceImpl` | `GeminiClient`, `WishImageService`, `RestTemplate` | mail |
| `AgenticAiService` | `AiServiceImpl` (qualified), `WishComposerAgent` | controllers |
| `WishComposerAgent` / `DeliveryOpsAgent` / `AdminCopilotAgent` | `AgentRunner`, repositories | controllers |
| Schedulers | services, AI, mail, storage | controllers |
| `SupabaseStorageService` | S3 client | domain services |

## 3. Shared data stores

| Store | Components using it |
| --- | --- |
| Primary PostgreSQL | Auth, User, Event, Festival, Mapping, Calendar, Circle, Notification, Theme, Seeds, WishImage, Email logs, Dashboard, Schedulers (Quartz tables) |
| Chat PostgreSQL | `ChatServiceImpl`, `ChatMessageRepository`, `UnreadChatEmailScheduler`, `AuthServiceImpl` (unread count) |
| Supabase Storage | `SupabaseStorageService` used by email, events, wish images, chat attachments, profile pictures |
| Ehcache | `FestivalServiceImpl`, `DashboardServiceImpl`, `IpInfoService`, `EmailImageService`, Hibernate second-level/query cache |

## 4. Deployment components

| Component | Instances | Notes |
| --- | --- | --- |
| Spring Boot container | 1 (Render) | Port 8080, `prod` profile |
| Neon PostgreSQL | managed | Primary data + Quartz |
| Supabase PostgreSQL | managed | Chat |
| Supabase Storage | managed | Media bucket |
| Quartz scheduler | inside the container | Clustered configuration, single instance today |

See [deployment-architecture.md](deployment-architecture.md) for the network view.
