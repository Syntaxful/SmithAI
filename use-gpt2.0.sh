#!/usr/bin/env bash
# ==============================================================
# SmithAI — Switch to SmithGPT 2.0 (7.5GB)
# Stops any running SmithAI-Server, then starts GPT 2.0.
# Usage: ./use-gpt2.0.sh
# ==============================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/SmithAI-Server"
MODEL_FILE="models/smithgpt-2.0-7.5.gguf"
MODEL_NAME="SmithGPT 2.0 7.5GB"
PORT="${PORT:-8000}"
PID_FILE="/tmp/smithai-server.pid"

# --- Stop any running server ---
if [ -f "$PID_FILE" ]; then
    OLD_PID="$(cat "$PID_FILE")"
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo "Stopping current SmithAI-Server (PID $OLD_PID)..."
        kill "$OLD_PID" && sleep 1
        echo "Stopped."
    fi
    rm -f "$PID_FILE"
fi
pkill -f "python.*app\.py" 2>/dev/null || true

cd "$SERVER_DIR"

if [ ! -f "$MODEL_FILE" ]; then
    echo "ERROR: SmithGPT 2.0 model not found at $MODEL_FILE"
    echo "Run ./BuildGPT2.0 first to download the model."
    exit 1
fi

# --- Write config ---
cat > config.yml <<YAML
# SmithAI-Server configuration — SmithGPT 2.0
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

echo "Switched config to ${MODEL_NAME}."

# --- Activate venv if present ---
if [ -d "venv" ]; then
    source venv/bin/activate
fi

# --- Start server in background, save PID ---
echo ""
echo "=== Starting ${MODEL_NAME} on port ${PORT} ==="
nohup python app.py > /tmp/smithai-server.log 2>&1 &
SERVER_PID=$!
echo "$SERVER_PID" > "$PID_FILE"
echo "Server started (PID $SERVER_PID). Logs: /tmp/smithai-server.log"
echo ""
echo "Set API key in Minecraft: /SmithAPI set <key-from-log>"
echo "Stop the server:  kill \$(cat $PID_FILE)"
