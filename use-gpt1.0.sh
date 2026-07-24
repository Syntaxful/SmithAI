#!/usr/bin/env bash
# Switch SmithAI-Server to SmithGPT 1.0 (~2.2GB).

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SERVER_DIR="SmithAI-Server"
MODEL_NAME="smithgpt-1.0-2.2gb.gguf"

API_KEY=""
if [ -f "$SERVER_DIR/config.yml" ] && python -c "import yaml" 2>/dev/null; then
    API_KEY="$(python -c "import yaml; print(yaml.safe_load(open('$SERVER_DIR/config.yml')).get('security',{}).get('api_key',''))")" || true
fi
if [ -z "$API_KEY" ]; then
    API_KEY="SMA-$(python -c 'import secrets; print(secrets.token_hex(16))')"
fi

cat > "$SERVER_DIR/config.yml" <<CONF
# SmithAI-Server configuration — SmithGPT 1.0 (3B / Q4_0 / ~2.2GB)
server:
  host: 0.0.0.0
  port: 8000
model:
  context_size: 4096
  max_tokens: 250
  n_threads: 4
  name: SmithGPT 1.0 2.2GB
  path: models/$MODEL_NAME
security:
  api_key: $API_KEY
CONF

echo "✅ Switched to SmithGPT 1.0 (~2.2GB)."
echo "Download the model if missing: ./BuildGPT1.0"
echo "Start the server: cd $SERVER_DIR && source venv/bin/activate && python app.py"
