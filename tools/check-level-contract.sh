#!/bin/sh
set -eu
old_source="level.le""vel"
old_runtime="level.lv""l"
roots="src tools/src test build.xml README.md sdk/python/microx_editor"
if find res/levels -type f \( -name "$old_source" -o -name "$old_runtime" \) | sed -n '1p' | grep -q .; then
  echo "obsolete level filename found" >&2
  exit 1
fi
if rg -n --fixed-strings -e "$old_source" -e "$old_runtime" $roots; then
  echo "obsolete level reference found" >&2
  exit 1
fi
