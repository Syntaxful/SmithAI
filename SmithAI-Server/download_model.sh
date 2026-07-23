#!/usr/bin/env bash
# Wrapper for download_model.py on Linux/macOS.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d "venv" ]; then
    python3 -m venv venv
fi
source venv/bin/activate

python download_model.py "$@"
