#!/bin/sh
# Self-bootstrapping Maven Wrapper.
# Uses `mvn` from PATH if present, otherwise downloads Apache Maven 3.9.9
# into .mvn/wrapper and uses that. No global Maven install required.
set -e
MVNW_DIR=$(cd "$(dirname "$0")" && pwd)
WRAP="$MVNW_DIR/.mvn/wrapper"
MVN_DIR="$WRAP/apache-maven-3.9.9"
MVN_EXE="$MVN_DIR/bin/mvn"

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

if [ ! -x "$MVN_EXE" ]; then
  DIST="https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
  echo "Downloading Maven 3.9.9 into $WRAP ..."
  mkdir -p "$WRAP"
  curl -fsSL "$DIST" -o "$WRAP/mvn.zip"
  (cd "$WRAP" && unzip -q mvn.zip && rm -f mvn.zip)
fi

exec "$MVN_EXE" "$@"
