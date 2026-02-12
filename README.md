# family-wishes

Production-ready Spring Boot 3.x (Java 17) backend for family wish automation.

## Features
- JWT auth + refresh tokens
- OTP login and forgot/reset password
- Role-based user management
- Event management and festival templates with versioning
- Gemini API wish generation
- Quartz daily scheduler
- SMTP email sending with retry and status tracking

## Run
```bash
gradle bootRun
```

## Build
```bash
gradle clean bootJar
```

## Environment variables
- `JWT_SECRET`
- `GEMINI_API_KEY`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `DB_URL`, `DB_USER`, `DB_PASS` for production profile

## Database reference
- Primary PostgreSQL database is hosted on Neon.
- Neon project console: https://console.neon.tech/

## API Docs
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
