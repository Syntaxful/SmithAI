#!/usr/bin/env bash
# SmithAI-Server startup script for Linux and macOS.
# Creates a virtual environment if it doesn't exist, installs dependencies, and starts the server.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

source venv/bin/activate

if ! python -c "import fastapi, uvicorn, llama_cpp" 2>/dev/null; then
    echo "Installing dependencies..."
    pip install -r requirements.txt
fi

echo "Starting SmithAI-Server..."
python app.py
