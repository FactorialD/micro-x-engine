#!/bin/sh
set -eu
jar_file=$1
entries=$(jar tf "$jar_file")
for level in res/levels/*; do
  name=${level##*/}
  for file in level.txt geometry.mesh textures.tex; do
    echo "$entries" | grep -qx "levels/$name/$file" || {
      echo "$jar_file: missing runtime resource levels/$name/$file" >&2
      exit 1
    }
  done
done
