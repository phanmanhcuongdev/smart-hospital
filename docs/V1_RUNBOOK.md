# V.1 runbook

1. Copy `.env.example` to `.env` and replace the local-only database password.
2. Start: `docker compose up --build -d`.
3. Inspect: `docker compose ps` and `docker compose logs --follow backend`.
4. Health: `curl http://localhost:8080/actuator/health`, `curl http://localhost:8080/api/v1/system/health`, and open `http://localhost:3000`.
5. Inspect Flyway safely: `docker compose exec db psql -U smarthms -d smarthms -c "select version, success from flyway_schema_history order by installed_rank"`.
6. Stop without deleting data: `docker compose down`. Do not remove the named volume as routine cleanup.

On Windows, start Docker Desktop and wait until `docker info` succeeds before step 2. If ports conflict, change `*_PORT` in the ignored `.env`.
