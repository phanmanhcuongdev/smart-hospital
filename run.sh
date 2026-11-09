#!/usr/bin/env bash
# run.sh - Start the whole Smart Hospital app
# Usage: ./run.sh
# Starts: PostgreSQL (docker), Spring Boot backend, Vite frontend.
# Logs: logs/backend.log, logs/frontend.log
# Stop: pkill -f smarthms-backend; pkill -f vite; docker stop smart-hospital-db-1
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
mkdir -p logs
DB_CONTAINER="smart-hospital-db-1"
BACKEND_JAR="backend/target/smarthms-backend-0.0.1-SNAPSHOT.jar"
BACKEND_URL="http://localhost:8080/actuator/health"
FRONTEND_URL="http://localhost:5173/"
echo "===== Smart Hospital - starting all services ====="
echo "Repo root: $ROOT"
echo "[1/3] Database: starting PostgreSQL container $DB_CONTAINER..."
if docker ps -a --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
  docker start "$DB_CONTAINER" >/dev/null
else
  POSTGRES_PASSWORD=smarthms_local_only docker compose up -d db
fi
echo "  Waiting for the database to become healthy (up to ~90s)..."
DB_OK=0
for _ in $(seq 1 18); do
  STATUS="$(docker inspect --format '{{.State.Health.Status}}' "$DB_CONTAINER" 2>/dev/null || echo unknown)"
  if [ "$STATUS" = "healthy" ]; then DB_OK=1; break; fi
  sleep 5
done
if [ "$DB_OK" -ne 1 ]; then echo "ERROR: database did not become healthy"; docker logs --tail 50 "$DB_CONTAINER" || true; exit 1; fi
echo "  Database is healthy (PostgreSQL on localhost:5439)."
echo "[2/3] Backend: checking $BACKEND_URL ..."
if curl -fsS "$BACKEND_URL" 2>/dev/null | grep -q '"status":"UP"'; then
  echo "  Backend already running, skipping."
else
  if [ ! -f "$BACKEND_JAR" ]; then (cd backend && mvn -q -DskipTests package); fi
  echo "  Starting backend in the background (logs/backend.log)..."
  nohup java -jar "$BACKEND_JAR" > logs/backend.log 2>&1 &
  echo "  Waiting for backend health UP (up to ~120s)..."
  BACKEND_OK=0
  for _ in $(seq 1 24); do
    if curl -fsS "$BACKEND_URL" 2>/dev/null | grep -q '"status":"UP"'; then BACKEND_OK=1; break; fi
    sleep 5
  done
  if [ "$BACKEND_OK" -ne 1 ]; then echo "ERROR: backend did not become healthy"; tail -n 50 logs/backend.log || true; exit 1; fi
  echo "  Backend is UP."
fi
echo "[3/3] Frontend: checking $FRONTEND_URL ..."
if [ "$(curl -s -o /dev/null -w '%{http_code}' "$FRONTEND_URL" 2>/dev/null || echo 000)" = "200" ]; then
  echo "  Frontend already running, skipping."
else
  if [ ! -d frontend/node_modules ]; then (cd frontend && npm install); fi
  echo "  Starting frontend in the background (logs/frontend.log)..."
  (cd frontend && nohup npm run dev > ../logs/frontend.log 2>&1 &)
  echo "  Waiting for frontend to return HTTP 200 (up to ~90s)..."
  FRONTEND_OK=0
  for _ in $(seq 1 18); do
    if [ "$(curl -s -o /dev/null -w '%{http_code}' "$FRONTEND_URL" 2>/dev/null || echo 000)" = "200" ]; then FRONTEND_OK=1; break; fi
    sleep 5
  done
  if [ "$FRONTEND_OK" -ne 1 ]; then echo "ERROR: frontend did not return HTTP 200"; tail -n 50 logs/frontend.log || true; exit 1; fi
  echo "  Frontend is up."
fi
echo "===== Smart Hospital is up ====="
echo "  Frontend (local):  http://localhost:5173/"
echo "  Backend  (local):  http://localhost:8080/"
echo "  Backend health:    http://localhost:8080/actuator/health"
echo "  Database:          PostgreSQL localhost:5439 (container $DB_CONTAINER)"
echo "  Forwarded URLs (GitHub Codespaces):"
echo "  Frontend: https://ominous-space-computing-machine-jjr7v46vq74xh5j57-5173.app.github.dev/"
echo "  Backend health: https://ominous-space-computing-machine-jjr7v46vq74xh5j57-8080.app.github.dev/actuator/health"
echo "  Demo accounts (password: demo): EMP-DEMO-001, demo-hr, demo-hr-approver, ..."
echo "  Logs: logs/backend.log, logs/frontend.log"
echo "  To stop: pkill -f smarthms-backend; pkill -f vite; docker stop $DB_CONTAINER"
echo "================================"
