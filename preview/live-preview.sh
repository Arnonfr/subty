#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT="${PORT:-7890}"

echo "Starting Subty live preview on http://localhost:${PORT}"
echo "Auto-reload is enabled (save index.html/server.js to refresh)."

cleanup() {
  if [[ -n "${SERVER_PID:-}" ]] && kill -0 "${SERVER_PID}" 2>/dev/null; then
    kill "${SERVER_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

PORT="${PORT}" LIVE_RELOAD=1 node "${ROOT_DIR}/server.js" &
SERVER_PID=$!

sleep 1
if command -v open >/dev/null 2>&1; then
  open "http://localhost:${PORT}" >/dev/null 2>&1 || true
fi

wait "${SERVER_PID}"
