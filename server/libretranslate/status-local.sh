#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
PORT="$(grep '^LIBRETRANSLATE_PORT=' .env 2>/dev/null | cut -d'=' -f2)"
PORT="${PORT:-5001}"

if curl -fsS "http://127.0.0.1:${PORT}/health" >/dev/null; then
  echo "Argos translation server: RUNNING on http://127.0.0.1:${PORT}"
  curl -s "http://127.0.0.1:${PORT}/languages"
  echo
else
  echo "Argos translation server: STOPPED"
  exit 1
fi
