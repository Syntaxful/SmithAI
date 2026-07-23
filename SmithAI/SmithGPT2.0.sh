#!/usr/bin/env bash
# ==============================================================
# SmithGPT 2.0 — One-command server setup & start (7.5GB model)
# ==============================================================
# This script:
#   1. Installs Python dependencies
#   2. Downloads the SmithGPT 2.0 model (7.5GB GGUF) if missing
#   3. Configures server for SmithGPT 2.0
#   4. Starts the server with advanced settings
#
# Usage: ./SmithGPT2.0.sh
# ==============================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/SmithAI-Server"

# --- Config ---
MODEL_NAME="SmithGPT 2.0 7.5GB"
MODEL_FILE="models/smithgpt-2.0-7.5.gguf"
HF_REPO="TheBloke/Mistral-Nemo-Instruct-2407-GGUF"
HF_FILE="mistral-nemo-instruct-2407.Q4_K_M.gguf"
PORT="${PORT:-8000}"

echo "=== SmithGPT 2.0 Server Setup ==="
echo "Target: ${MODEL_NAME} (requires ~8GB RAM)"

# --- Python venv ---
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi
source venv/bin/activate

# --- Dependencies ---
pip install -q --upgrade pip
if ! python -c "import fastapi, uvicorn" 2>/dev/null; then
    echo "Installing dependencies..."
    pip install -r requirements.txt
fi

# --- Write config (higher context, more tokens for GPT 2.0) ---
cat > config.yml <<YAML
# SmithAI-Server configuration
server:
  host: 0.0.0.0
  port: ${PORT}
  rate_limit: 8.0
model:
  name: "${MODEL_NAME}"
  path: "${MODEL_FILE}"
  context_size: 8192
  max_tokens: 400
  n_threads: 4
security:
  api_key: ""
YAML
echo "Config written for ${MODEL_NAME}."

# --- Download model if missing ---
if [ ! -f "${MODEL_FILE}" ]; then
    echo "Model not found at ${MODEL_FILE}."
    echo "Downloading ${HF_REPO}/${HF_FILE} (~7.5GB)..."
    echo "This will take several minutes on a fast connection."
    pip install -q huggingface-hub
    mkdir -p models
    python -c "
from huggingface_hub import hf_hub_download
path = hf_hub_download('${HF_REPO}', '${HF_FILE}')
import shutil, os
os.makedirs('models', exist_ok=True)
shutil.copy(path, '${MODEL_FILE}')
print(f'Model saved to ${MODEL_FILE}')
" || {
    echo "ERROR: Hugging Face download failed."
    echo "Manual download: https://huggingface.co/${HF_REPO}"
    exit 1
}
else
    echo "Model found at ${MODEL_FILE}."
fi

# --- Start ---
echo ""
echo "=== Starting ${MODEL_NAME} on port ${PORT} ==="
echo "Set API key in Minecraft with: /SmithAPI set <key>"
echo ""
exec python app.py
