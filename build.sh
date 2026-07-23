#!/usr/bin/env bash
# Build the SmithAI plugin.
# Usage: ./build.sh [version]
# If Maven is not on PATH, set MAVEN=/path/to/mvn

set -e

VERSION="${1:-2.0.0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -z "$MAVEN" ]; then
    MAVEN="mvn"
fi

if ! command -v "$MAVEN" >/dev/null 2>&1; then
    echo "ERROR: Maven not found. Please install Maven or set MAVEN=/path/to/mvn"
    exit 1
fi

echo "Building SmithAI plugin v$VERSION..."
"$MAVEN" -f "$SCRIPT_DIR/SmithAI/pom.xml" clean test package

echo ""
echo "Build complete: $SCRIPT_DIR/SmithAI/target/SmithAI-$VERSION.jar"
