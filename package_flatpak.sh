#!/bin/bash

set -e

if ! which magick &> /dev/null; then
  echo "Please install magick"
  exit 1
fi

echo "[+] Building bundle"
./gradlew clean
./gradlew packageReleaseAppImage
echo "[+] Building icons"
for i in 16 32 64 128 256 ; do
  magick graphics/ic_launcher.ico -resize ${i}x${i} graphics/ic_launcher_${i}x${i}.png
done
echo "[+] Building and installing flatpak"
flatpak-builder flatpak --install --user chat.schildi.SchildiChatRevenge.yml --force-clean
echo "[+] Done! Run from your desktop environment, or type:"
echo "   flatpak run chat.schildi.SchildiChatRevenge"

