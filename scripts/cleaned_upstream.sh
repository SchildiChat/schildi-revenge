#!/usr/bin/bash

set -e

mydir="$(dirname "$(realpath "$0")")"
cd "$mydir"

rm -rf skeleton
git clone https://github.com/SchildiChat/schildichat-android-next.git skeleton
cd skeleton
git filter-repo --paths-from-file ../../elex_imports.txt
