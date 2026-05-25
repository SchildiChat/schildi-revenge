#!/bin/bash
# Small wrapper to copy to release dir.
# Maybe should figure out how to do this directly from gradle some day.

set -e

out_dir="composeApp/build/compose/binaries/main-release/pacman"

./gradlew clean
./gradlew packageReleasePacman

mkdir -p release

file=`ls -Art "$out_dir/"*.pkg.tar.zst | tail -n 1`

echo "Built $file"

cp "$file" release/
