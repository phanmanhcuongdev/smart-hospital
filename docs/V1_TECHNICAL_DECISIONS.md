# V.1 technical decisions

Date: 2026-11-07. Status for every entry: **DECIDED FOR V1 BY PROJECT LEAD**.

| ID | Decision | Rationale and consequence |
| --- | --- | --- |
| ADR-01 | Spring Boot modular monolith; in-process interfaces | Keeps EMR, HR and common in one deployable; no internal REST/broker. |
| ADR-02 | Java 21, Spring Boot 4.1.2, Maven, MVC, JPA/Hibernate, Flyway | Approved V.1 backend stack; JPA validates only and Flyway owns schema. |
| ADR-03 | React 19, TypeScript, Vite 8, Node 24, npm | Small V.1 status UI with `fetch`; no router/state/UI library. |
| ADR-04 | PostgreSQL 17 / UTC / UUID / four schemas | Baseline DDL runs through Flyway; synthetic data only. |
| ADR-05 | Docker Compose local demo | `db`, `backend`, and Nginx-served `frontend`; DB binds localhost only. |
| ADR-06 | Health-only public boundary, deny all other API paths | Authentication/RBAC/session/audit business implementation is V.2 work. |
| ADR-07 | Only infrastructure HTTP endpoints in V.1 | No invented EMR/HR CRUD contract. |
| ADR-08 | Exclude `emr.encounter_open_idempotency` | Deferred to V.3 change control. |
| ADR-09 | Environment config, safe errors, correlation-ready logs | No secret or sensitive clinical content is logged by this foundation. |
| ADR-10 | Domain packages and no cross-module writes | Preserves EMR/HR ownership for parallel work. |
