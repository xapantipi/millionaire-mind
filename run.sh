#!/usr/bin/env bash
# Runs Millionaire Mind. Optionally pass a path to an alternate question
# bank CSV as the first argument, e.g. ./run.sh resources/questions.csv
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -d out ]; then
  echo "Not built yet -- running build.sh first..."
  ./build.sh
fi

java -cp out millionairemind.ConsoleUI "$@"
