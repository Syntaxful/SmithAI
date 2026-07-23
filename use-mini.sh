#!/usr/bin/env bash
# ==============================================================
# SmithAI — Switch to Smith-Mini 1.0 (built-in, no server needed)
# Stops any running SmithAI-Server — Mini runs inside the plugin.
# Usage: ./use-mini.sh
# ==============================================================
set -euo pipefail
PID_FILE="/tmp/smithai-server.pid"

# --- Stop any running external server ---
if [ -f "$PID_FILE" ]; then
    OLD_PID="$(cat "$PID_FILE")"
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo "Stopping SmithAI-Server (PID $OLD_PID)..."
        kill "$OLD_PID" && sleep 1
        echo "Stopped."
    fi
    rm -f "$PID_FILE"
fi
pkill -f "python.*app\.py" 2>/dev/null || true

echo ""
echo "✅ Switched to Smith-Mini 1.0 (built-in — no external server needed)."
echo ""
echo "   Drop SmithAI-2.0.0.jar in your server's plugins/ folder and start."
echo "   Smith_AI will use Smith-Mini 1.0 automatically."
echo ""
echo "   To switch to an external model later:"
echo "     ./use-gpt1.0.sh   (SmithGPT 1.0 — 4GB)"
echo "     ./use-gpt2.0.sh   (SmithGPT 2.0 — 7.5GB)"
