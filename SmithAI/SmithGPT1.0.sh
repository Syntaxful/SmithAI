#!/usr/bin/env bash
# ==============================================================
# SmithGPT 1.0 — One-command server setup & start (4GB model)
# ==============================================================
# This script:
#   1. Installs Python dependencies
#   2. Downloads the SmithGPT 1.0 model (4GB GGUF) if missing
#   3. Configures server for SmithGPT 1.0
#   4. Starts the server
#
# Usage: ./SmithGPT1.0.sh
# ==============================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/SmithAI-Server"

# --- Config ---
MODEL_NAME="SmithGPT 1.0 4GB"
MODEL_FILE="models/smithgpt-1.0-4.gguf"
HF_REPO="TheBloke/Mistral-7B-Instruct-v0.3-GGUF"
HF_FILE="mistral-7b-instruct-v0.3.Q4_K_M.gguf"
PORT="${PORT:-8000}"

echo "=== SmithGPT 1.0 Server Setup ==="
echo "Target: ${MODEL_NAME}"

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

# --- Write config ---
cat > config.yml <<YAML
# SmithAI-Server configuration
server:
  host: 0.0.0.0
  port: ${PORT}
  rate_limit: 10.0
model:
  name: "${MODEL_NAME}"
  path: "${MODEL_FILE}"
  context_size: 4096
  max_tokens: 200
  n_threads: 2
security:
  api_key: ""
YAML
echo "Config written for ${MODEL_NAME}."

# --- Download model if missing ---
if [ ! -f "${MODEL_FILE}" ]; then
    echo "Model not found at ${MODEL_FILE}."
    echo "Downloading ${HF_REPO}/${HF_FILE} (~4GB)..."
    echo "This will take a few minutes on a fast connection."
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
