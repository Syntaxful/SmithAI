#!/usr/bin/env bash
# Bump the version across the project.
# Usage: ./bump-version.sh <new_version>
# Example: ./bump-version.sh 2.1.0

set -e

if [ -z "$1" ]; then
    echo "Usage: ./bump-version.sh <new_version>"
    exit 1
fi

NEW_VERSION="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POM="$SCRIPT_DIR/SmithAI/pom.xml"

# Update pom.xml
sed -i -E "s|<version>[0-9]+\.[0-9]+\.[0-9]+</version>|<version>$NEW_VERSION</version>|" "$POM"

# Update plugin.yml version placeholder (commented; filtered at build time from pom.xml)
echo "Version bumped to $NEW_VERSION in $POM"
echo "Remember to update any hardcoded version strings in source files and docs."
