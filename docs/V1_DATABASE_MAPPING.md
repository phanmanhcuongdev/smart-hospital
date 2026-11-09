# V.1 database mapping

Source: IV.2 `IV2_01_schema.sql`, retrieved from the connected Drive artifact
on 2026-11-07. It is preserved as Flyway `V1__iv2_baseline.sql`; no
SCP-IV4-001 idempotency table was added.

- Schemas: `ref`, `hr`, `sys`, `emr`.
- Tables: 27, statically counted from the source migration.
- Migration versions: V1 baseline, V2 deterministic synthetic seed.
- Seed: one synthetic department, employee, patient fixture and medication
  fixture; each `INSERT` has a natural-key/primary-key conflict guard.
- The source retains its PK, FK, UNIQUE and CHECK constraints and any baseline
  triggers. PostgreSQL execution/constraint verification remains blocked only
  because Docker Desktop is not running on this host.
