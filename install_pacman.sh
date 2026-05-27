#!/bin/bash

set -e

app_version="git"

./package_linux_releases.sh -f "$app_version"

file=`ls -Art "release/$app_version/"*.pkg.tar.zst | tail -n 1`

echo "Installing $file"

sudo pacman -U "$file"
