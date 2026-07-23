#!/usr/bin/env bash
# Package a SmithAI release bundle.
# Usage: ./package-release.sh [version]
# Output: release/SmithAI-v<version>.zip and .sha256 checksums

set -e

VERSION="${1:-2.0.0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_DIR="$SCRIPT_DIR/release"
BUILD_DIR="$RELEASE_DIR/SmithAI-v$VERSION"

if [ -z "$MAVEN" ]; then
    MAVEN="mvn"
fi

if ! command -v "$MAVEN" >/dev/null 2>&1; then
    echo "ERROR: Maven not found. Please install Maven or set MAVEN=/path/to/mvn"
    exit 1
fi

if ! command -v zip >/dev/null 2>&1; then
    echo "ERROR: zip not found. Please install zip."
    exit 1
fi

if ! command -v sha256sum >/dev/null 2>&1; then
    echo "WARNING: sha256sum not found. Checksums will not be generated."
    CHECKSUM=false
else
    CHECKSUM=true
fi

rm -rf "$RELEASE_DIR"
mkdir -p "$BUILD_DIR"

echo "Building SmithAI plugin..."
"$MAVEN" -f "$SCRIPT_DIR/SmithAI/pom.xml" clean package

echo "Copying files..."
cp "$SCRIPT_DIR/SmithAI/target/SmithAI-$VERSION.jar" "$BUILD_DIR/"
cp -r "$SCRIPT_DIR/SmithAI-Server" "$BUILD_DIR/"
cp "$SCRIPT_DIR/README.md" "$BUILD_DIR/"
cp "$SCRIPT_DIR/SKILLS.md" "$BUILD_DIR/"
cp "$SCRIPT_DIR/HOSTING.md" "$BUILD_DIR/"
cp "$SCRIPT_DIR/FAQ.md" "$BUILD_DIR/"
cp "$SCRIPT_DIR/LICENSE" "$BUILD_DIR/"
cp "$SCRIPT_DIR/REPORT_TEMPLATE.md" "$BUILD_DIR/"
cp "$SCRIPT_DIR/build.sh" "$BUILD_DIR/"
cp "$SCRIPT_DIR/package-release.sh" "$BUILD_DIR/"
cp "$SCRIPT_DIR/bump-version.sh" "$BUILD_DIR/"
mkdir -p "$BUILD_DIR/models"
cp "$SCRIPT_DIR/models/README.md" "$BUILD_DIR/models/"

if [ "$CHECKSUM" = true ]; then
    echo "Generating checksums..."
    cd "$BUILD_DIR"
    sha256sum "SmithAI-$VERSION.jar" > "SmithAI-$VERSION.jar.sha256"
    cd "$SCRIPT_DIR"
fi

echo "Packaging release..."
cd "$RELEASE_DIR"
zip -r "SmithAI-v$VERSION.zip" "SmithAI-v$VERSION"

if [ "$CHECKSUM" = true ]; then
    sha256sum "SmithAI-v$VERSION.zip" > "SmithAI-v$VERSION.zip.sha256"
fi

echo ""
echo "Release packaged: $RELEASE_DIR/SmithAI-v$VERSION.zip"
