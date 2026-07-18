#!/usr/bin/env bash
# Regenerates the Phase-0 compatibility fixtures (vassal-app/src/test/resources/fixtures)
# from whatever module/save/log files are found in example-artifacts/.
#
# example-artifacts/ is intentionally gitignored (large, possibly-licensed game files);
# the fixtures/ directory this script writes to IS committed, so the fixtures serve as
# a stable golden corpus even though the raw inputs aren't checked in.
#
# Layout expected under example-artifacts/: one subdirectory per module, each
# containing exactly one .vmod (or .vmdx) plus any number of .vsav/.vlog files
# belonging to that module.
#
# Requires: a real or virtual X display (xvfb-run is used automatically if present
# and DISPLAY is unset), and vassal-app already test-compiled (this script will
# test-compile it if needed).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$ROOT/vassal-app"
ARTIFACTS_DIR="$ROOT/example-artifacts"
FIXTURES_DIR="$APP_DIR/src/test/resources/fixtures"
CP_FILE="$ROOT/.fixtures-classpath.txt"

if [ ! -d "$ARTIFACTS_DIR" ]; then
  echo "No $ARTIFACTS_DIR directory found; nothing to do." >&2
  exit 0
fi

cd "$APP_DIR"

echo "== test-compiling vassal-app =="
"$ROOT/mvnw" -q test-compile

echo "== resolving classpath =="
"$ROOT/mvnw" -q dependency:build-classpath -Dmdep.outputFile="$CP_FILE"
CP="target/classes:target/test-classes:$(cat "$CP_FILE")"

RUNNER=(java -cp "$CP" VASSAL.tools.FixtureCaptureTool)
if [ -z "${DISPLAY:-}" ] && command -v xvfb-run >/dev/null 2>&1; then
  RUNNER=(xvfb-run -a "${RUNNER[@]}")
fi

shopt -s nullglob nocaseglob

for dir in "$ARTIFACTS_DIR"/*/; do
  name="$(basename "$dir")"

  vmods=("$dir"*.vmod "$dir"*.vmdx)
  if [ ${#vmods[@]} -eq 0 ]; then
    echo "skip $name: no .vmod or .vmdx found"
    continue
  fi
  if [ ${#vmods[@]} -gt 1 ]; then
    echo "skip $name: multiple module files found, expected one (${vmods[*]})"
    continue
  fi
  vmod="${vmods[0]}"

  saves=("$dir"*.vsav "$dir"*.vlog)

  outdir="$FIXTURES_DIR/$name"
  mkdir -p "$outdir"

  echo "== capturing fixtures for $name =="
  "${RUNNER[@]}" "$vmod" "$outdir" "${saves[@]}"
done

rm -f "$CP_FILE"
echo "Done. Fixtures written under $FIXTURES_DIR"
