#!/usr/bin/env bash
# Runs the separate Swing experiment with the same question bank as ConsoleUI.
# Compiles independently of build.sh/run.sh so ConsoleUI.java is never
# compiled here -- this UI doesn't use it.
set -euo pipefail
cd "$(dirname "$0")"

build_dir="out-swing"
rm -rf "$build_dir"
mkdir -p "$build_dir"

javac --release 17 -d "$build_dir" $(find src -name "*.java" -not -name "ConsoleUI.java")
cp "resources/Question Bank.csv" "$build_dir/questions.csv"

java -cp "$build_dir" millionairemind.SwingUI "$@"