#!/usr/bin/env bash
# Download and configure BOTH SmithGPT models.
# Usage: ./BuildBoth.sh
# SmithGPT 1.0 = 4GB model  |  SmithGPT 2.0 = 7.5GB model
# Total download: ~11.5GB. Run scripts individually for one model only.

set -euo pipefail
cd "$(dirname "$0")"

echo "=== Building both SmithGPT models ==="
echo "This will download ~11.5GB total."
echo ""

echo "--- Step 1: SmithGPT 1.0 (4GB) ---"
./BuildGPT1.0

echo ""
echo "--- Step 2: SmithGPT 2.0 (7.5GB) ---"
./BuildGPT2.0

echo ""
echo "✅ Both models ready!"
echo ""
echo "Switch between them with:"
echo "  ./use-gpt1.0.sh    → SmithGPT 1.0 (4GB, uses less RAM)"
echo "  ./use-gpt2.0.sh    → SmithGPT 2.0 (7.5GB, smarter)"
echo ""
echo "Or edit SmithAI-Server/config.yml manually and restart."
