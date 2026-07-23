#!/usr/bin/env bash
# Switch the plugin to use Smith-Mini 1.0 (built-in, no server needed).
# No download required — this is the default.
# Usage: ./use-mini.sh
# Just ensure the plugin's config uses Smith-Mini (default).

set -euo pipefail

echo "✅ Smith-Mini 1.0 is the built-in default — no setup needed."
echo ""
echo "Just drop SmithAI-2.0.0.jar in your server's plugins/ folder."
echo "The plugin automatically uses Smith-Mini 1.0."
echo ""
echo "To use an external server (SmithGPT 1.0/2.0):"
echo "  1. Host SmithAI-Server on any machine"
echo "  2. Set the URL and API key in-game:"
echo "     /SmithAPI set <your-api-key>"
echo "     /SmithAPI url http://your-server:8000"
