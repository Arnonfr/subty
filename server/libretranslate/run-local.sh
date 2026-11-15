#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  cp .env.example .env
fi

PORT="$(grep '^LIBRETRANSLATE_PORT=' .env | cut -d'=' -f2)"
PORT="${PORT:-5001}"
PID_FILE=".argos_server.pid"
LOG_FILE=".argos_server.log"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "Argos server already running on http://localhost:${PORT}"
  exit 0
fi

PY="/Users/hyh/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3"

nohup env ARGOS_SERVER_PORT="$PORT" "$PY" ./argos_server.py > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"
sleep 2

echo "Argos translation server is running on http://localhost:${PORT}"
curl -s "http://localhost:${PORT}/health" || true
echo
