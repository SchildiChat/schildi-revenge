#!/bin/bash

set -e

./package_pacman.sh

file=`ls -Art "release/"*.pkg.tar.zst | tail -n 1`

echo "Installing $file"

sudo pacman -U "$file"
