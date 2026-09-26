#!/usr/bin/env bash
# Runs the separate Swing experiment with the same question bank as ConsoleUI.
# Compiles independently of build.sh/run.sh so ConsoleUI.java is never
# compiled here -- this UI doesn't use it.
set -euo pipefail
cd "$(dirname "$0")"

build_dir="out-swing"
rm -rf "$build_dir"
mkdir -p "$build_dir"

javac --release 17 -d "$build_dir" $(find src/main/java -name "*.java" -not -name "ConsoleUI.java")
cp "resources/Question Bank.csv" "$build_dir/questions.csv"
cp "resources/bg.wav" "$build_dir/bg.wav"
cp "resources/splash.gif" "$build_dir/splash.gif"
cp "resources/splash.wav" "$build_dir/splash.wav"
mkdir -p "$build_dir/sfx"
cp "resources/sfx/niera_sound_5.wav" "$build_dir/sfx/niera_sound_5.wav"
cp "resources/sfx/niera_sound_2.wav" "$build_dir/sfx/niera_sound_2.wav"
cp "resources/sfx/select_005.wav" "$build_dir/sfx/select_005.wav"
cp "resources/sfx/confirmation_004.wav" "$build_dir/sfx/confirmation_004.wav"
cp "resources/sfx/error_006.wav" "$build_dir/sfx/error_006.wav"

java -cp "$build_dir" millionairemind.SwingUI "$@"