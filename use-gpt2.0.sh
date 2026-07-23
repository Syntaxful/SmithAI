#!/usr/bin/env bash
# Switch the AI server to use SmithGPT 2.0 (7.5GB).
# Simply updates config.yml — no re-download needed.
# Usage: ./use-gpt2.0.sh

set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f "SmithAI-Server/models/smithgpt-2.0-7.gguf" ]; then
    echo "ERROR: SmithGPT 2.0 model not found. Run ./BuildGPT2.0 first."
    exit 1
fi

echo "Switching to SmithGPT 2.0 (7.5GB)..."
cat > SmithAI-Server/config.yml << 'CONF'
model:
  context_size: 8192
  max_tokens: 300
  n_threads: 4
  name: SmithGPT 2.0 7.5GB
  path: models/smithgpt-2.0-7.gguf
security:
  api_key: SMA-dQdZM9NrHjSr2KEJc8mOkgY4NOEzVBoS
server:
  host: 0.0.0.0
  port: 8000
CONF

echo "✅ Switched to SmithGPT 2.0 — Restart the server to apply!"
echo "   docker-compose restart    (Docker users)"
echo "   cd SmithAI-Server && python3 app.py    (direct users)"
