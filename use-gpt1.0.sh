#!/usr/bin/env bash
# Switch the AI server to use SmithGPT 1.0 (4GB).
# Simply updates config.yml — no re-download needed.
# Usage: ./use-gpt1.0.sh

set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f "SmithAI-Server/models/smithgpt-1.0-4.gguf" ]; then
    echo "ERROR: SmithGPT 1.0 model not found. Run ./BuildGPT1.0 first."
    exit 1
fi

echo "Switching to SmithGPT 1.0 (4GB)..."
cat > SmithAI-Server/config.yml << 'CONF'
model:
  context_size: 4096
  max_tokens: 200
  n_threads: 2
  name: SmithGPT 1.0 4GB
  path: models/smithgpt-1.0-4.gguf
security:
  api_key: SMA-dQdZM9NrHjSr2KEJc8mOkgY4NOEzVBoS
server:
  host: 0.0.0.0
  port: 8000
CONF

echo "✅ Switched to SmithGPT 1.0 — Restart the server to apply!"
echo "   docker-compose restart    (Docker users)"
echo "   cd SmithAI-Server && python3 app.py    (direct users)"
