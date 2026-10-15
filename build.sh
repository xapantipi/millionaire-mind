#!/usr/bin/env bash
# Compiles Millionaire Mind into ./out and copies the question bank alongside
# the compiled classes so it loads as a classpath resource.
set -euo pipefail
cd "$(dirname "$0")"

rm -rf out
mkdir -p out
javac --release 17 -d out $(find src -name "*.java")
cp "resources/Question Bank.csv" out/questions.csv

echo "Build complete. Run with: ./run.sh or ./run-swing.sh"
