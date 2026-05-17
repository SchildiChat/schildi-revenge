#!/bin/bash

set -e

for dep in magick flatpak-builder ; do 
  if ! which ${dep} &> /dev/null; then
    echo "Please install ${dep}"
    exit 1
  fi
done

echo "[+] Building bundle"
./gradlew clean
./gradlew packageReleaseAppImage
echo "[+] Building icons"
mkdir -p composeApp/build/graphics
for i in 16 32 64 128 256 ; do
  magick graphics/ic_launcher.ico -resize ${i}x${i} composeApp/build/graphics/ic_launcher_${i}x${i}.png
done
echo "[+] Building and installing flatpak"
flatpak-builder flatpak --install --user chat.schildi.SchildiChatRevenge.yml --force-clean
echo "[+] Done! Run from your desktop environment, or type:"
echo "   flatpak run chat.schildi.SchildiChatRevenge"

