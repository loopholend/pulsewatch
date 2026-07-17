# PulseWatch

> Uptime monitoring and incident management for developers who ship fast.

PulseWatch watches your APIs and services 24/7. When something goes down, you know before your users do.

---

## What it does

| Feature | Description |
|---|---|
| **HTTP Monitoring** | Check any HTTP/HTTPS endpoint on a configurable interval (30s–1h) |
| **Assertions** | Validate status codes, response body content, and more |
| **Incident Tracking** | Automatic incident creation after N consecutive failures, with severity escalation |
| **Alert Delivery** | Email alerts on incident open/resolve with cooldown |
| **Analytics** | 30-day uptime %, P95 latency, rolling error rate |
| **Status Pages** | Public status pages — share real-time status with your users |
| **Maintenance Windows** | Schedule planned downtime to suppress false alerts |
| **Realtime Updates** | SSE-based live dashboard — no polling required |
| **Data Export** | Export check history and incidents as CSV or JSON |
| **Workspaces** | Multi-user team support with RBAC |
| **API Keys** | Machine-to-machine access for CI/CD pipelines |

---

## How it's different

PulseWatch is not Datadog. It's not trying to be.

It's for developers who want a self-hosted, simple, reliable monitor for their own APIs — without paying $500/month or configuring a 40-screen setup.

---

## Architecture

```
Browser (React + Vite)
    │  REST + SSE
    ▼
Spring Boot API
    ├── JWT Auth + Workspace RBAC
    ├── Monitor Scheduler (ScheduledExecutorService)
    ├── WebClient (non-blocking HTTP checks)
    ├── IncidentService (failure threshold, escalation)
    ├── AlertService (email delivery, cooldown)
    ├── AnalyticsService (SQL aggregates, no memory bloat)
    └── RealtimeService (SSE broadcast by workspaceId)
         │
PostgreSQL (Flyway migrations V1–V15)
```

---

## Quickstart (Docker)

### 1. Copy the environment template

```bash
cp .env.example .env
```

### 2. Set your JWT secret

```bash
# Generate a strong secret
openssl rand -hex 32

# Then paste it into .env:
JWT_SECRET=your-generated-secret-here
```

> ⚠️ **The application will refuse to start if `JWT_SECRET` is missing.** This is intentional.

### 3. Start everything

```bash
docker compose up --build
```

- Frontend: http://localhost
- API: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html

---

## Quickstart (Local Development)

### Prerequisites

- Java 21+
- Node 20+
- PostgreSQL 15+

### Backend

```bash
cd pulsewatch-backend

# Set environment variables
export JWT_SECRET=$(openssl rand -hex 32)
export DB_PASSWORD=yourpassword

# Run with dev profile (enables verbose SQL logging)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
cd pulsewatch-frontend
npm install
npm run dev
```

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | **YES** | JWT signing key. Min 32 bytes. App fails to start without this. |
| `DB_PASSWORD` | No | PostgreSQL password. Default: `changeme` (for local dev only) |
| `MAIL_HOST` | No | SMTP host for email alerts. App runs without it (alerts log as FAILED) |
| `MAIL_PORT` | No | SMTP port. Default: `587` |
| `MAIL_USERNAME` | No | SMTP username |
| `MAIL_PASSWORD` | No | SMTP password |

See [.env.example](.env.example) for a full template.

---

## Running Tests

```bash
cd pulsewatch-backend
./mvnw clean test
```

Tests use `@SpringBootTest` with `@Transactional` — they roll back after each test and do not require a separate test database.

---

## Configuration

Key settings in `application.yml`:

```yaml
pulsewatch:
  incident:
    failure-threshold: 3        # Consecutive failures before incident opens
  scheduler:
    thread-pool-size: 20        # Max parallel monitor checks
    interval-ms: 60000          # Scheduler tick interval
    timeout-ms: 30000           # HTTP check timeout
```

Verbose SQL logging and development secrets live in `application-dev.yml`.
Activate with `--spring.profiles.active=dev`.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, Recharts, Lucide, TailwindCSS |
| Backend | Spring Boot 3, Spring Security, Spring WebFlux (WebClient) |
| Database | PostgreSQL 16 with Flyway migrations |
| Auth | JWT (RS256 via HS256 shared secret), per-request workspace resolution |
| Realtime | Server-Sent Events (SSE), broadcast per workspaceId |
| Deployment | Docker, Docker Compose, Nginx (SPA config + gzip) |

---

## API Reference

Interactive docs available at `/swagger-ui.html` when the backend is running.

Key endpoint groups:

- `POST /api/auth/register` — Register account
- `POST /api/auth/login` — Login, get JWT
- `GET /api/monitors` — List monitors (workspace-scoped, paginated)
- `GET /api/analytics/summary` — Dashboard summary stats
- `GET /api/realtime/connect` — SSE stream (requires auth)
- `GET /api/export/incidents?format=csv` — Export incidents
- `GET /api/status/{slug}` — Public status page (no auth)

---

## Deployment Notes

**React Router:** The Nginx config includes a `try_files` fallback so `/monitors`, `/incidents`, etc. don't return 404 on browser refresh.

**Security:** The backend will not start without `JWT_SECRET`. There is no insecure default. Production config has SQL logging disabled.

**Health check:** `GET /actuator/health` — used by Docker Compose `depends_on` conditions.

---

## License

MIT
