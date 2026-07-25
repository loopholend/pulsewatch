<div align="center">

# ⚡ PulseWatch

### API Monitoring & Observability Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue?logo=react)](https://reactjs.org/)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.java.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](https://www.docker.com/)
[![Azure](https://img.shields.io/badge/Azure%20Container%20Apps-Ready-0078D4?logo=microsoft-azure)](https://azure.microsoft.com/en-us/products/container-apps)
[![Version](https://img.shields.io/badge/version-1.0.0-blueviolet)](https://github.com/loopholend/pulsewatch/releases)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

**PulseWatch watches your APIs 24/7. When something breaks, you know before your users do.**

[Features](#features) · [Quick Start](#quick-start) · [Docker Deployment](#docker-deployment) · [Azure Deployment](#azure-container-apps-deployment) · [Architecture](#architecture) · [API Docs](#api-documentation)

</div>

---

## Features

| Feature | Description |
|---|---|
| **HTTP Monitoring** | Check any HTTP/HTTPS endpoint every 30s – 1h with configurable timeouts |
| **Smart Assertions** | Validate status codes, response body content, and custom rules |
| **Incident Management** | Automatic incident lifecycle after N consecutive failures with severity escalation |
| **Email Alerts** | Configurable alert rules with custom templates, cooldown, and recipient routing |
| **Analytics** | 30-day uptime %, P95 latency, rolling error rate, historical trends |
| **Status Pages** | Public status pages — share live service health with your users |
| **Maintenance Windows** | Schedule planned downtime to suppress false alerts |
| **Realtime Dashboard** | Server-Sent Events (SSE) live updates — no polling required |
| **Data Export** | Export check history and incidents as CSV or JSON |
| **Multi-Workspace** | Team workspaces with role-based access control (OWNER/ADMIN/DEVELOPER/VIEWER) |
| **API Keys** | Machine-to-machine access for CI/CD pipelines and integrations |
| **Audit Log** | Full audit trail for all workspace actions |

---

## Quick Start

### Prerequisites

- **Docker** 24+ and **Docker Compose** v2
- `openssl` (for JWT secret generation)

### 1. Clone the repository

```bash
git clone https://github.com/loopholend/pulsewatch.git
cd pulsewatch
```

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and set **at minimum**:

```bash
# REQUIRED - generate a secure random secret:
JWT_SECRET=$(openssl rand -hex 32)
# or on Windows PowerShell:
# JWT_SECRET = -join ((48..57 + 65..90 + 97..122) | Get-Random -Count 64 | % {[char]$_})
```

All other values have safe defaults for local development. See [Environment Variables](#environment-variables) for a full reference.

### 3. Start with Docker Compose

```bash
docker compose up --build
```

Wait for the backend health check to pass (about 60-90 seconds on first build).

### 4. Open the application

| Service | URL |
|---|---|
| **Frontend** (App) | http://localhost |
| **Backend API** | http://localhost:8080/api |
| **API Documentation** | http://localhost:8080/swagger-ui.html |
| **PostgreSQL** | localhost:5432 (user: postgres) |

### 5. Register an account

Navigate to http://localhost and click **Get Started** to register your first account. A personal workspace is created automatically.

---

## Docker Deployment

### Build and run

```bash
# Start all services
docker compose up --build -d

# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Stop all services
docker compose down

# Reset database (WARNING: deletes all data)
docker compose down -v
docker compose up --build -d
```

### Environment variables summary

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | **YES** | — | JWT signing secret (min 32 hex chars) |
| `DB_PASSWORD` | No | `changeme` | PostgreSQL password |
| `CORS_ALLOWED_ORIGINS` | No | `*` | Comma-separated allowed CORS origins |
| `APP_BASE_URL` | No | `http://localhost:8080` | Backend public URL |
| `FRONTEND_URL` | No | `http://localhost` | Frontend public URL (used in emails) |
| `MAIL_HOST` | No | `smtp.mailtrap.io` | SMTP host |
| `MAIL_PORT` | No | `587` | SMTP port |
| `MAIL_USERNAME` | No | empty | SMTP username (leave empty = email disabled) |
| `MAIL_PASSWORD` | No | empty | SMTP password |
| `MAIL_SMTP_AUTH` | No | `false` | Enable SMTP AUTH |
| `MAIL_SMTP_STARTTLS` | No | `false` | Enable STARTTLS |
| `SPRING_PROFILES_ACTIVE` | No | `default` | Use `prod` for production/Azure |
| `FAILURE_THRESHOLD` | No | `3` | Consecutive failures before incident opens |
| `PRUNING_DAYS` | No | `30` | Days to retain check results |

---

## Azure Container Apps Deployment

### Prerequisites

- Azure CLI installed: `az login`
- Azure subscription with Container Apps provider registered

### Step 1 — Create resources

```bash
# Variables
RESOURCE_GROUP=pulsewatch-rg
LOCATION=eastus
ACR_NAME=pulsewatchacr
ENVIRONMENT=pulsewatch-env
APP_NAME_BACKEND=pulsewatch-backend
APP_NAME_FRONTEND=pulsewatch-frontend

# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create Container Registry
az acr create --resource-group $RESOURCE_GROUP --name $ACR_NAME --sku Basic --admin-enabled true

# Get ACR credentials
ACR_SERVER=$(az acr show --name $ACR_NAME --query loginServer -o tsv)
ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --query passwords[0].value -o tsv)

# Login to ACR
az acr login --name $ACR_NAME
```

### Step 2 — Build and push images

```bash
# Build and push backend
docker build -t $ACR_SERVER/pulsewatch-backend:1.0.0 ./pulsewatch-backend
docker push $ACR_SERVER/pulsewatch-backend:1.0.0

# Build and push frontend
docker build -t $ACR_SERVER/pulsewatch-frontend:1.0.0 ./pulsewatch-frontend
docker push $ACR_SERVER/pulsewatch-frontend:1.0.0
```

### Step 3 — Create Azure PostgreSQL

```bash
DB_SERVER_NAME=pulsewatch-db
DB_ADMIN=pwadmin
DB_PASSWORD=YourSecurePassword123!

az postgres flexible-server create \
  --resource-group $RESOURCE_GROUP \
  --name $DB_SERVER_NAME \
  --location $LOCATION \
  --admin-user $DB_ADMIN \
  --admin-password "$DB_PASSWORD" \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --version 16 \
  --storage-size 32

# Create database
az postgres flexible-server db create \
  --resource-group $RESOURCE_GROUP \
  --server-name $DB_SERVER_NAME \
  --database-name pulsewatch

# Allow Azure services
az postgres flexible-server firewall-rule create \
  --resource-group $RESOURCE_GROUP \
  --name $DB_SERVER_NAME \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

### Step 4 — Create Container Apps environment

```bash
az containerapp env create \
  --name $ENVIRONMENT \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION
```

### Step 5 — Deploy backend

```bash
JWT_SECRET=$(openssl rand -hex 32)
DB_HOST="$DB_SERVER_NAME.postgres.database.azure.com"
BACKEND_URL="https://pulsewatch-backend.$(az containerapp env show --name $ENVIRONMENT --resource-group $RESOURCE_GROUP --query properties.defaultDomain -o tsv)"

az containerapp create \
  --name $APP_NAME_BACKEND \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT \
  --image $ACR_SERVER/pulsewatch-backend:1.0.0 \
  --registry-server $ACR_SERVER \
  --registry-username $ACR_NAME \
  --registry-password $ACR_PASSWORD \
  --target-port 8080 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 --memory 1.0Gi \
  --env-vars \
    "JWT_SECRET=$JWT_SECRET" \
    "DB_HOST=$DB_HOST" \
    "DB_PORT=5432" \
    "DB_NAME=pulsewatch" \
    "DB_USERNAME=$DB_ADMIN" \
    "DB_PASSWORD=$DB_PASSWORD" \
    "SPRING_PROFILES_ACTIVE=prod" \
    "CORS_ALLOWED_ORIGINS=https://YOUR_FRONTEND_URL" \
    "APP_BASE_URL=$BACKEND_URL" \
    "FRONTEND_URL=https://YOUR_FRONTEND_URL"
```

### Step 6 — Deploy frontend

```bash
az containerapp create \
  --name $APP_NAME_FRONTEND \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT \
  --image $ACR_SERVER/pulsewatch-frontend:1.0.0 \
  --registry-server $ACR_SERVER \
  --registry-username $ACR_NAME \
  --registry-password $ACR_PASSWORD \
  --target-port 8080 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 2 \
  --cpu 0.25 --memory 0.5Gi
```

### Step 7 — Verify deployment

```bash
# Check backend health
BACKEND_URL=$(az containerapp show --name $APP_NAME_BACKEND --resource-group $RESOURCE_GROUP --query properties.configuration.ingress.fqdn -o tsv)
curl https://$BACKEND_URL/actuator/health

# Check frontend
FRONTEND_URL=$(az containerapp show --name $APP_NAME_FRONTEND --resource-group $RESOURCE_GROUP --query properties.configuration.ingress.fqdn -o tsv)
echo "Frontend: https://$FRONTEND_URL"
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Browser / Client                    │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP/SSE
                         ▼
┌─────────────────────────────────────────────────────────┐
│              nginx (port 80/8080)                       │
│  • Serves React SPA (static files)                     │
│  • Proxies /api/* → backend:8080                       │
│  • Proxies /public/* → backend:8080                    │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│           Spring Boot Backend (port 8080)               │
│                                                         │
│  ┌──────────────┐  ┌─────────────┐  ┌───────────────┐  │
│  │  Auth (JWT)  │  │  Monitors   │  │   Incidents   │  │
│  │  API Keys    │  │  Scheduler  │  │   Analytics   │  │
│  │  RBAC        │  │  WebClient  │  │   Alerts      │  │
│  └──────────────┘  └─────────────┘  └───────────────┘  │
│                                                         │
│  ┌──────────────┐  ┌─────────────┐  ┌───────────────┐  │
│  │  Status Pages│  │  Realtime   │  │  Export/Audit │  │
│  │  Maintenance │  │  SSE        │  │  Workspace    │  │
│  └──────────────┘  └─────────────┘  └───────────────┘  │
│                                                         │
│  Flyway Migrations → PostgreSQL 16                      │
└────────────────────────┬────────────────────────────────┘
                         │ JDBC / HikariCP
                         ▼
┌─────────────────────────────────────────────────────────┐
│              PostgreSQL 16 (port 5432)                  │
│  • 19 Flyway migrations                                 │
│  • 20+ tables with workspace isolation                  │
│  • Full cascade delete semantics                        │
└─────────────────────────────────────────────────────────┘
```

---

## Local Development (Without Docker)

### Backend

```bash
cd pulsewatch-backend

# Requires: JDK 21, Maven, local PostgreSQL 16
# Set environment variables:
export JWT_SECRET=your-dev-secret-min-32-chars-long-hex
export DB_HOST=localhost
export DB_PASSWORD=root

mvn spring-boot:run
# or: mvn package && java -jar target/pulsewatch-backend-1.0.0.jar
```

### Frontend

```bash
cd pulsewatch-frontend

npm install
npm run dev
# Opens at http://localhost:5173 with proxy to backend at :8080
```

---

## API Documentation

When running locally:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

> Note: Swagger is disabled in the `prod` profile for security.

---

## Project Structure

```
pulsewatch/
├── pulsewatch-backend/          # Spring Boot 3.4.1 + Java 21
│   ├── src/main/java/           # Application source
│   │   └── com/pulsewatch/backend/
│   │       ├── auth/            # JWT, API keys, RBAC
│   │       ├── monitor/         # Monitor CRUD, scheduler, execution
│   │       ├── incident/        # Incident lifecycle, events
│   │       ├── alert/           # Alert rules, providers, history
│   │       ├── analytics/       # Uptime, latency, stats
│   │       ├── statuspage/      # Public status pages
│   │       ├── realtime/        # SSE streaming
│   │       ├── audit/           # Audit log
│   │       └── scheduler/       # MonitorScheduler, DataPruning
│   ├── src/main/resources/
│   │   ├── application.yml      # Base config
│   │   ├── application-prod.yml # Production/Azure overrides
│   │   └── db/migration/        # 19 Flyway migrations (V1-V19)
│   └── Dockerfile               # Multi-stage, non-root, healthcheck
│
├── pulsewatch-frontend/         # React 18 + Vite
│   ├── src/
│   │   ├── api/axiosConfig.js   # HTTP client (relative path proxy)
│   │   ├── components/          # All UI components
│   │   └── context/AuthContext  # JWT auth state
│   ├── nginx.conf               # Production nginx config
│   └── Dockerfile               # Multi-stage nginx-unprivileged
│
├── docker-compose.yml           # Full stack local deployment
├── .env.example                 # Environment variable reference
└── .gitignore
```

---

## Security

- **JWT RS256**: Stateless authentication, 24h token expiry, 7-day refresh
- **BCrypt**: Password hashing (strength 10)
- **Workspace Isolation**: All queries scoped to workspace — no cross-tenant data leakage
- **API Key Auth**: SHA-256 hashed keys, tracked by creator, live role lookup
- **RBAC**: OWNER / ADMIN / DEVELOPER / VIEWER role enforcement
- **CORS**: Configurable per environment via `CORS_ALLOWED_ORIGINS`
- **Non-root containers**: Both Dockerfiles run as non-root users
- **No secrets in images**: All configuration via environment variables

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">

Built with ❤️ as a production-grade portfolio project demonstrating real-world SaaS engineering.

</div>
