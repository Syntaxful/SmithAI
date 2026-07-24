#!/usr/bin/env bash
# SmithAI-Server fast start.
# Use ./bootstrap.sh for first-time setup (downloads the model automatically).
# Use ./start.sh when the model is already present.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d "venv" ]; then
    echo "[start] No venv found, falling back to bootstrap.sh..."
    exec bash bootstrap.sh "$@"
fi

# shellcheck disable=SC1091
source venv/bin/activate

# If a configured model path is missing, try the bootstrap download flow.
MODEL_PATH="$(python -c "import yaml,os; cfg=yaml.safe_load(open(os.environ.get('SMITHAI_CONFIG','config.yml'))) or {}; print((cfg.get('model') or {}).get('path',''))" 2>/dev/null || true)"
if [ -n "$MODEL_PATH" ] && [ ! -f "$MODEL_PATH" ]; then
    echo "[start] Model missing at $MODEL_PATH; redirecting to bootstrap.sh"
    exec bash bootstrap.sh "$@"
fi

echo "[start] Starting SmithAI-Server on port ${PORT:-8000}..."
exec python app.py
