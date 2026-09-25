#!/usr/bin/env bash
# Launch the Java Spring Boot voice-browser application
set -euo pipefail
cd "$(dirname "$0")"

if [ -z "${TYPESAFE_API_KEY:-}" ] && [ -z "${JEV_API_KEY:-}" ] && [ -f .env ]; then
  set -a; . ./.env; set +a
fi
if [ "${TYPESAFE_API_KEY:-}" = "your_typesafe_api_key_here" ]; then
  unset TYPESAFE_API_KEY
fi
if [ "${JEV_API_KEY:-}" = "your_typesafe_api_key_here" ]; then
  unset JEV_API_KEY
fi

exec ./apache-maven-3.9.9/bin/mvn spring-boot:run "$@"
