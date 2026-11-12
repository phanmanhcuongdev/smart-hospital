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
# Stop any previous backend/frontend processes so every run restarts
# from scratch instead of skipping what is already running.
# Each step is safe when nothing is listening: failures are ignored
# (|| true) so `set -e` never aborts the cleanup.
free_port() {
  local port="$1"
  local pids=""
  if command -v lsof >/dev/null 2>&1; then
    pids=$(lsof -t -i ":${port}" -sTCP:LISTEN 2>/dev/null || true)
  fi
  if [ -z "$pids" ] && command -v ss >/dev/null 2>&1; then
    pids=$(ss -ltnp 2>/dev/null | grep ":${port} " | grep -o 'pid=[0-9]*' | cut -d= -f2 | sort -u || true)
  fi
  if [ -z "$pids" ] && command -v fuser >/dev/null 2>&1; then
    pids=$(fuser "${port}/tcp" 2>&1 | grep -o '[0-9][0-9]*' | grep -v "^${port}$" | sort -u || true)
  fi
  if [ -n "$pids" ]; then
    echo "  Stopping process(es) listening on port ${port}: $pids"
    kill $pids 2>/dev/null || true
    sleep 2
    kill -9 $pids 2>/dev/null || true
  else
    echo "  Port ${port} is free."
  fi
}

echo "Stopping any previous backend/frontend processes..."
free_port 8080
free_port 5173
pkill -f smarthms-backend 2>/dev/null || true
pkill -f vite 2>/dev/null || true
sleep 2

echo "===== Smart Hospital - starting all services ====="
echo "Repo root: $ROOT"
echo "[1/3] Database: starting PostgreSQL container $DB_CONTAINER..."
if docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
  echo "  Database container $DB_CONTAINER already running; restarting it (data is preserved, no volume is removed)..."
  docker restart "$DB_CONTAINER" >/dev/null
elif docker ps -a --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
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
echo "[2/3] Backend: starting a fresh backend (any previous backend was stopped above)..."
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
echo "[3/3] Frontend: starting a fresh frontend (any previous frontend was stopped above)..."
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
echo "===== Smart Hospital is up ====="
echo "  Frontend (local):  http://localhost:5173/"
echo "  Backend  (local):  http://localhost:8080/"
echo "  Backend health:    http://localhost:8080/actuator/health"
echo "  Database:          PostgreSQL localhost:5439 (container $DB_CONTAINER)"
echo "  Forwarded URLs (GitHub Codespaces):"
echo "  Frontend: https://ominous-space-computing-machine-jjr7v46vq74xh5j57-5173.app.github.dev/"
echo "  Backend health: https://ominous-space-computing-machine-jjr7v46vq74xh5j57-8080.app.github.dev/actuator/health"
echo "  Accounts: admin (Admin@2026!), doctor1 (Doctor@2026!), demo users with password 'demo' (demo-hr, EMP-DEMO-001, etc.)"
echo "  Logs: logs/backend.log, logs/frontend.log"
echo "  To stop: pkill -f smarthms-backend; pkill -f vite; docker stop $DB_CONTAINER"
echo "================================"
