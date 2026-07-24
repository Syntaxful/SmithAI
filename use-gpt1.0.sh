#!/usr/bin/env bash
# Switch to SmithGPT 1.0 (1B / Q4_0 / ~600MB).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/SmithAI-Server"
MODEL_FILE="models/smithgpt-1.0-1b.gguf"
MODEL_NAME="SmithGPT 1.0 1B"
PORT="${PORT:-8000}"
PID_FILE="/tmp/smithai-server.pid"

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
    echo "ERROR: SmithGPT 1.0 model not found at $MODEL_FILE"
    echo "Run ./BuildGPT1.0 first to download the model."
    exit 1
fi

API_KEY=""
if [ -f "config.yml" ] && python -c "import yaml" 2>/dev/null; then
    API_KEY="$(python -c "import yaml; print(yaml.safe_load(open('config.yml')).get('security',{}).get('api_key',''))")" || true
fi
if [ -z "$API_KEY" ]; then
    API_KEY="SMA-$(python -c 'import secrets; print(secrets.token_hex(16))')"
fi

cat > config.yml <<YAML
# SmithAI-Server configuration — SmithGPT 1.0
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
  api_key: "${API_KEY}"
YAML

echo "Switched config to ${MODEL_NAME}."
echo "API key: ${API_KEY}"
echo ""

if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi
source venv/bin/activate
if ! python -c "import fastapi, uvicorn" 2>/dev/null; then
    echo "Installing server dependencies..."
    pip install -r requirements.txt
fi

echo ""
echo "=== Starting ${MODEL_NAME} on port ${PORT} ==="
nohup python app.py > /tmp/smithai-server.log 2>&1 &
SERVER_PID=$!
echo "$SERVER_PID" > "$PID_FILE"
echo "Server started (PID $SERVER_PID). Logs: /tmp/smithai-server.log"
echo ""
echo "Set API key in Minecraft: /SmithAPI set ${API_KEY}"
echo "Stop the server:  kill \$(cat $PID_FILE)"
