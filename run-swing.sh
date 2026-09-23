#!/usr/bin/env bash
# Runs the separate Swing experiment with the same question bank as ConsoleUI.
set -euo pipefail
cd "$(dirname "$0")"

./build.sh
java -cp out millionairemind.SwingUI "$@"
