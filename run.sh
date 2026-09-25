#!/usr/bin/env bash
# Launch the Java Spring Boot voice-browser application
set -euo pipefail
cd "$(dirname "$0")"

exec ./run-java.sh "$@"
