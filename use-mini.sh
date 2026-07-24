#!/usr/bin/env bash
# Switch SmithAI-Server to the built-in Smith-Mini 1.0 brain (~700MB parameters/skill data).
# No external model download is required for Smith-Mini; the plugin runs it locally.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SERVER_DIR="SmithAI-Server"

API_KEY=""
if [ -f "$SERVER_DIR/config.yml" ] && python -c "import yaml" 2>/dev/null; then
    API_KEY="$(python -c "import yaml; print(yaml.safe_load(open('$SERVER_DIR/config.yml')).get('security',{}).get('api_key',''))")" || true
fi
if [ -z "$API_KEY" ]; then
    API_KEY="SMA-$(python -c 'import secrets; print(secrets.token_hex(16))')"
fi

cat > "$SERVER_DIR/config.yml" <<CONF
# SmithAI-Server configuration — Smith-Mini 1.0 (built-in, ~700MB)
server:
  host: 0.0.0.0
  port: 8000
model:
  context_size: 2048
  max_tokens: 150
  n_threads: 2
  name: Smith-Mini 1.0 700MB
  path: models/smith-mini-1.0.gguf
security:
  api_key: $API_KEY
CONF

echo "✅ Switched to Smith-Mini 1.0 (~700MB). No external model needed."
echo "Start the server if you want an external endpoint: cd $SERVER_DIR && source venv/bin/activate && python app.py"
