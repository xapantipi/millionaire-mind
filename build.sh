#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")"

rm -rf out

mkdir -p out

javac -d out $(find src -name "*.java")

cp resources/questions_collected.csv out/

echo "Build complete."
echo "Run with: ./run.sh"s