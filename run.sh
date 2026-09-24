#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build
javac -encoding UTF-8 -d build ./*.java
exec java -cp build ExitPlan
