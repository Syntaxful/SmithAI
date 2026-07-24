#!/usr/bin/env bash
# Download and configure BOTH SmithGPT models.
# Usage: ./BuildBoth.sh
# SmithGPT 1.0 = ~600MB (1B Q4_0)  |  SmithGPT 2.0 = ~1.5GB (3B Q4_0)
# Total download: ~2.1GB.
# Re-runs are fast because existing files and the venv are reused.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

START_TIME=$(date +%s)

echo "=== Building both SmithGPT models ==="
echo "Total download: ~2.1GB. Re-runs skip already-present files."
echo ""

echo "--- Step 1: SmithGPT 1.0 (~600MB) ---"
./BuildGPT1.0

echo ""
echo "--- Step 2: SmithGPT 2.0 (~1.5GB) ---"
./BuildGPT2.0

echo ""
echo "✅ Both models ready!"
echo ""
echo "Switch between them with:"
echo "  ./use-gpt1.0.sh    → SmithGPT 1.0 (1B, fastest, ~600MB)"
echo "  ./use-gpt2.0.sh    → SmithGPT 2.0 (3B, smarter, ~1.5GB)"
echo ""
echo "Or edit SmithAI-Server/config.yml manually and restart."

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))
echo ""
echo "Total elapsed time: ${ELAPSED}s"
