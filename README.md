# SmartHMS — V.1 Foundation

This repository contains the SmartHMS V.1 foundation for Examination & Medical
Record (EMR), Human Resource (HR), and limited common infrastructure.

## Current state

Implemented source/configuration:

- Spring Boot 4.1.2 / Java 21 backend with Flyway, PostgreSQL configuration,
  health endpoints, safe error response foundation and deny-by-default API
  security.
- React/TypeScript/Vite status UI that reads backend health through same-origin
  `/api` proxy.
- PostgreSQL 17 Compose service and IV.2 Flyway baseline plus synthetic seed.

Deferred to V.2–V.6:

- Authentication, RBAC, sessions, business audit behavior, and EMR/HR business
  APIs. No business route is exposed in V.1.

See [the V.1 decision register](docs/V1_DECISIONS_AND_BLOCKERS.md),
[handover](docs/V1_HANDOVER.md), and
[verification record](docs/V1_VERIFICATION.md) before starting V.2–V.6 work.

## Layout

```text
backend/src/common/       Framework-neutral shared-boundary guidance
backend/src/modules/emr/  EMR ownership and integration boundary
backend/src/modules/hr/   HR ownership and integration boundary
frontend/                 React/Vite status application
docs/                     V.1 decisions, handover, and verification evidence
```

## Quick start

Copy `.env.example` to `.env`, choose a local-only password, then run
`docker compose up --build -d`. Open `http://localhost:3000`; backend health is
at `http://localhost:8080/actuator/health`. See [runbook](docs/V1_RUNBOOK.md).
