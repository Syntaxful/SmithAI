#!/usr/bin/env bash
# Download and configure BOTH SmithGPT models.
# Usage: ./BuildBoth.sh
# SmithGPT 1.0 = ~2.2GB (3B Q4_0)  |  SmithGPT 2.0 = ~4.4GB (8B Q4_K_M)
# Total download: ~6.6GB.
# Re-runs are fast because the scripts reuse the existing virtual environment and skip already-downloaded model files.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

START_TIME=$(date +%s)

echo "=== Building both SmithGPT models ==="
echo "Total download: ~6.6GB. Re-runs skip already-present files."
echo ""

echo "--- Step 1: SmithGPT 1.0 (~2.2GB) ---"
./BuildGPT1.0

echo ""
echo "--- Step 2: SmithGPT 2.0 (~4.4GB) ---"
./BuildGPT2.0

echo ""
echo "✅ Both models ready!"
echo ""
echo "Switch between them with:"
echo "  ./use-gpt1.0.sh    → SmithGPT 1.0 (3B, ~2.2GB)"
echo "  ./use-gpt2.0.sh    → SmithGPT 2.0 (8B, ~4.4GB)"
echo ""
echo "Or edit SmithAI-Server/config.yml manually and restart."

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))
echo ""
echo "Total elapsed time: ${ELAPSED}s"
