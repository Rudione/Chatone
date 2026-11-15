#!/bin/sh
set -eu
PROPS="$SRCROOT/../gradle.properties"
VERSION=$(sed -n 's/^app\.version=//p' "$PROPS" | tr -d '[:space:]')
CODE=$(sed -n 's/^app\.versionCode=//p' "$PROPS" | tr -d '[:space:]')
if ! printf '%s' "$VERSION" | /usr/bin/grep -Eq '^[0-9]+(\.[0-9]+){1,2}$'; then
    echo "error: invalid app.version in gradle.properties"
    exit 1
fi
if ! printf '%s' "$CODE" | /usr/bin/grep -Eq '^[0-9]+$'; then
    echo "error: invalid app.versionCode in gradle.properties"
    exit 1
fi
mkdir -p "$DERIVED_FILE_DIR"
printf '#define CHATONE_MARKETING_VERSION %s\n#define CHATONE_BUILD_NUMBER %s\n' "$VERSION" "$CODE" > "$DERIVED_FILE_DIR/ChatoneAppVersion.h"
