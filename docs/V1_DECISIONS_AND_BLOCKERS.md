# V.1 technical decision and blocker register

Date: 2026-11-07  
Scope: V.1 foundation only. This is an implementation record, not an ADR
approval or a replacement for project baseline documents.

## Evidence examined

- Repository preflight on `main`: clean working tree; remote
  `origin` unchanged; the existing root/backend/frontend READMEs and
  `docker-compose.yml` were zero-byte placeholders. No manifest, lockfile,
  runtime source, migration, seed runner, Dockerfile, or local ADR was found.
- WBS `Phan_cong` and the supplied scope baseline: mandatory implementation
  scope is EMR, HR, and limited SYS/common support; the other hospital modules
  are not V.1 runtime modules.
- IV.1/IV.2/IV.3 are team-level design inputs. The supplied baseline identifies
  modular monolith/internal typed interfaces as proposed architecture and
  PostgreSQL as ADR-03 proposed, not accepted runtime choices.
- IV.4 is blocked/not frozen; IV.5 requires rework; IV.6 is review-ready but
  not approved. No later accepted ADR was present in the repository.

The project lead's 2026-11-07 execution authorization supersedes the earlier
preflight technology blockers for V.1 only. Details are in
`V1_TECHNICAL_DECISIONS.md`.

## Decision matrix

| Decision | Status | Source | Approved by | Implementation impact | Action |
| --- | --- | --- | --- | --- | --- |
| Modular monolith with typed in-process interfaces | RESOLVED FOR V1 | ADR-01 execution authorization | Project lead | Spring modular package structure | Keep in-process boundaries. |
| Backend / ORM / migration | RESOLVED FOR V1 | ADR-02 execution authorization | Project lead | Java 21, Spring, JPA and Flyway source | Run Maven wrapper when environment is available. |
| Frontend | RESOLVED FOR V1 | ADR-03 execution authorization | Project lead | React/TypeScript/Vite source | Generate/commit npm lock after npm is available. |
| PostgreSQL / deployment | RESOLVED FOR V1 | ADR-04/05 execution authorization | Project lead | PostgreSQL 17 Compose + Flyway SQL | Start Docker Desktop and execute verification. |
| API runtime/bootstrap | NOT IN V1 IMPLEMENTATION SCOPE | ADR-07 | Project lead | Health endpoints only | Defer business APIs. |
| Session mechanism/security policy | OPEN | IV.6 v0.9.1 review-ready | — | No auth/session implementation | Approve policy; do not assume JWT/Redis. |
| Docker Compose/deployment | RESOLVED FOR V1 | ADR-05 | Project lead | Local Compose configuration | Runtime blocked only by stopped Docker engine. |
| SCP-IV4-001 schema change | BLOCKED | IV.4 | — | Excluded from migrations | Resolve explicit schema decision. |

## Blockers

| ID | Source | Impact | Required decision | Decision owner | Next action |
| --- | --- | --- | --- | --- | --- |
| BLK-01 | ADR-02 | Backend bootstrap, build/test/lint blocked | Language, framework, version, package manager | Owner + Reviewer | Record ACCEPTED ADR. |
| BLK-02 | Frontend TBD / IV.5 rework | Frontend bootstrap blocked | Framework/version and approved UX handoff | Owner + Reviewer | Freeze frontend decision. |
| BLK-03 | ADR-03; ORM/migration TBD | Schema migration and seed execution blocked | DB version plus data access/migration tooling | Owner + Reviewer | Approve strategy before importing IV.2 DDL. |
| BLK-04 | IV.4 / SCP-IV4-001 | HTTP boundary and related schema changes blocked | API catalogue and SCP disposition | Reviewer | Freeze/review contract. |
| BLK-05 | IV.6 OPEN-01..05 | Auth, RBAC, sessions, audit policy blocked | Roles/scopes, session thresholds, retention and audit atomicity | Owner + Reviewer | Resolve policy; implement in V.2. |
| BLK-06 | ADR-07 | Compose services/deployment blocked | Deployment target and local topology | Owner + Reviewer | Approve ADR-07. |

`OPEN-01..05` remain external policy/design issues: role/scope approval,
legal-signature posture, MFA/lockout/TTL/SoD, API/SCP contract, and
retention/privacy/audit atomicity. V.1 does not close or redefine them.
