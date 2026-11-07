# V.1 verification evidence

Date executed: 2026-11-07. Host: Windows, Java 21.0.12.1 available; Maven,
Node and npm unavailable; Docker CLI 29.8.0/Compose 5.5.1 installed but Docker
Desktop Linux engine was stopped (named pipe absent).

| Check | Command | Expected | Actual | Status |
| --- | --- | --- | --- | --- |
| Repository location | `Get-Location` | `D:\QLDAPM\smart-hospital` | Matched | PASS |
| Git branch/tree | `git status --short --branch` | Identify branch and edits | `main...origin/main`; clean | PASS |
| Remote review | `git remote -v` | Read remote without changes | `origin` points to configured GitHub remote | PASS |
| Inventory | `Get-ChildItem -Force -Recurse` | Discover files/config | Only skeleton directories; three zero-byte READMEs and zero-byte Compose | PASS |
| Manifest/config scan | `rg -n -i "adr|approved|accepted|nestjs|spring|postgres|migration|decision|architecture|runtime" .` | Find local decisions/runtime | No repository evidence of an accepted runtime, database, migration, or deployment decision | PASS |
| Drive baseline read | Connected Google Drive reads of named WBS/III/IV artifacts | Verify available source state | Source read; decisions remain proposed/pending/open as listed in decision register | PASS |
| Backend build/test/lint | Not available | Run only after stack bootstrap | No manifest or runtime exists | BLOCKED |
| Frontend startup/build | Not available | Run only after stack bootstrap | No manifest or runtime exists | BLOCKED |
| Database migration/seed | Not available | Run only against a new approved test DB | No DBMS/migration strategy or runnable migration exists | BLOCKED |
| V.1 file review | `git diff --check` and `git status --short` | No whitespace errors; inspect new files | No whitespace errors; intended V.1 files are modified/untracked and no pre-existing content was overwritten beyond zero-byte placeholders | PASS |
| Secret-pattern scan | `rg -n -i "(password|secret|token|api[_-]?key)\\s*[:=]" -g '!*.md' -g '!.env' -g '!*.sql' .` | Find committed-looking credentials | Only environment-variable names and a documented local default/placeholder; no real credential committed | PASS |
| Compose syntax | `docker compose config -q` | Valid interpolation/config | Exit code 0 after local ignored `.env` supplied | PASS |
| Baseline migration inventory | `rg '^CREATE SCHEMA'` / `rg '^CREATE TABLE' V1__iv2_baseline.sql` | 4 schemas, 27 tables | 4 schemas; 27 tables | PASS |
| Backend build/test | Docker build executes `mvn -q clean verify` | Build and tests pass | PASS; unit test executed in Maven build | PASS |
| Frontend quality | Docker build executes lint, typecheck, Vitest, build | All commands pass | PASS; 1 Vitest test passed; Vite 8.3.6 production build passed | PASS |
| Compose runtime/migration/HTTP/DB smoke | `docker compose up --build -d`, `curl`, `psql` | Services healthy, migrations verified | PASS; all three services up, health UP, proxy UP, 27 baseline tables + Flyway V1/V2 verified | PASS |

Static preflight success is not evidence that an application, migration, seed,
or security control ran successfully. Update this table with real command
output when the accepted stack is implemented.
