#!/usr/bin/env bash
set -euo pipefail
VERSION=8.11.1
CACHE="${HOME}/.gradle/uth-connect-bootstrap"
GRADLE="$CACHE/gradle-$VERSION/bin/gradle"
if [[ ! -x "$GRADLE" ]]; then
  mkdir -p "$CACHE"
  ZIP="$CACHE/gradle-$VERSION-bin.zip"
  echo "Downloading Gradle $VERSION from services.gradle.org..."
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --retry 2 -o "$ZIP" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  else
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  fi
  unzip -q -o "$ZIP" -d "$CACHE"
  rm "$ZIP"
fi
exec "$GRADLE" "$@"
