#!/usr/bin/env bash
# SmithAI-Server one-command bootstrap.
# Usage:
#   ./bootstrap.sh              # use SmithGPT 1.0 (2.2GB)
#   ./bootstrap.sh gpt1         # same as above
#   ./bootstrap.sh gpt2         # use SmithGPT 2.0 (4.4GB)
#   ./bootstrap.sh mini         # rule-based fallback only (no model download)
#   MODEL_TOKEN=... ./bootstrap.sh gpt2
#
# Required environment:
#   SMITHAI_API_TOKEN   optional; if set, becomes the access key local Minecraft uses.
#                       Otherwise the server prints a generated SMA- key on first boot.
#   MODEL_TOKEN         optional; passed through to downstream llama.cpp call sites.
#   PORT                optional; override the listen port (default 8000).
#
# This script:
#   1. creates ./venv if missing and installs requirements
#   2. downloads the chosen model into ./models/ if missing
#   3. starts the FastAPI server with the configured model
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

TIER="${1:-${SMITHAI_MODEL_TIER:-gpt1}}"
case "$TIER" in
    mini) MODEL_FILE="models/smith-mini.json"; MODEL_URL=""; MODEL_NAME="Smith-Mini 1.0" ;;
    gpt1|smithgpt-1.0|1.0|2.2gb)
        MODEL_FILE="models/smithgpt-1.0-2.2gb.gguf"
        MODEL_URL="https://huggingface.co/Syntaxful/SmithGPT-1.0-GGUF/resolve/main/smithgpt-1.0-2.2gb.gguf"
        MODEL_NAME="SmithGPT 1.0 2.2GB"
        ;;
    gpt2|smithgpt-2.0|2.0|4.4gb)
        MODEL_FILE="models/smithgpt-2.0-4.4gb.gguf"
        MODEL_URL="https://huggingface.co/Syntaxful/SmithGPT-2.0-GGUF/resolve/main/smithgpt-2.0-4.4gb.gguf"
        MODEL_NAME="SmithGPT 2.0 4.4GB"
        ;;
    *) echo "Unknown tier: $TIER (try: mini, gpt1, gpt2)"; exit 1 ;;
esac

if [ ! -d "venv" ]; then
    echo "[bootstrap] Creating Python virtual environment..."
    python3 -m venv venv
fi
# shellcheck disable=SC1091
source venv/bin/activate

if ! python -c "import fastapi, uvicorn, yaml" 2>/dev/null; then
    echo "[bootstrap] Installing server dependencies..."
    pip install --upgrade pip >/dev/null
    pip install -r requirements.txt
fi

case "$TIER" in
    mini)
        echo "[bootstrap] Using rule-based fallback (no model download)."
        ;;
    *)
        mkdir -p models
        if [ ! -f "$MODEL_FILE" ]; then
            if [ -n "$MODEL_URL" ] && command -v curl >/dev/null 2>&1; then
                echo "[bootstrap] Downloading $MODEL_NAME from $MODEL_URL"
                curl -fL --retry 3 -o "$MODEL_FILE" "$MODEL_URL"
            elif [ -n "$MODEL_URL" ]; then
                echo "[bootstrap] curl not found; falling back to download_model.sh"
                bash download_model.sh "$TIER"
            else
                echo "[bootstrap] No URL configured for tier $TIER"
                exit 1
            fi
        else
            echo "[bootstrap] $MODEL_NAME already present at $MODEL_FILE"
        fi
        # Optional: only install llama-cpp-python if a model is selected.
        if ! python -c "import llama_cpp" 2>/dev/null; then
            echo "[bootstrap] Installing llama-cpp-python (this can take a few minutes)..."
            pip install llama-cpp-python || {
                echo "[bootstrap] WARNING: llama-cpp-python install failed; running with rule-based fallback."
            }
        fi
        ;;
esac

# Persist the chosen tier so app.py can read it.
python - <<PY
import os, yaml
cfg_path = os.environ.get("SMITHAI_CONFIG", "config.yml")
cfg = {}
try:
    with open(cfg_path, "r") as f:
        cfg = yaml.safe_load(f) or {}
except FileNotFoundError:
    pass
model = cfg.setdefault("model", {})
model["path"] = "${MODEL_FILE}"
model["name"] = "${MODEL_NAME}"
# API token: env wins, otherwise leave empty so server auto-generates one.
model["tier"] = "${TIER}"
if os.environ.get("SMITHAI_API_TOKEN"):
    cfg.setdefault("security", {})["api_key"] = os.environ["SMITHAI_API_TOKEN"]
with open(cfg_path, "w") as f:
    yaml.safe_dump(cfg, f, default_flow_style=False)
print(f"[bootstrap] Wrote {cfg_path} (model={model['name']}, tier={TIER})")
PY

echo "[bootstrap] Starting SmithAI-Server on port ${PORT:-8000}..."
exec python app.py
