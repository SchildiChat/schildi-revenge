#!/bin/bash

set -e

if [ "$1" = "-f" ] || [ "$1" = "--force" ]; then
    force=1
    shift
else
    force=0
fi

# Prefer jetbrains JDK 21 if installed
JETBRAINS_JDK_LOCATION="/usr/lib/jvm/java-21-jetbrains"
if [ -x "$JETBRAINS_JDK_LOCATION/bin/java" ]; then
    echo "Using $JETBRAINS_JDK_LOCATION"
    export JAVA_HOME="$JETBRAINS_JDK_LOCATION"
fi

if [ ! -z "$1" ]; then
    app_version="$1"
else
    app_version=`date -u +%y.%m.%d`
fi

linux_root="composeApp/build/compose/binaries/main-release/linux-package-root"
appimage_root="composeApp/build/compose/binaries/main-release/appimage-root"
appimage_outfile="schildichat-revenge-x86_64-$app_version.AppImage"
release_out="release/$app_version"

echo "Building Linux packages for $app_version"

# Clean build of common packaging files
./gradlew clean
./gradlew syncReleaseLinuxPackageRoot

if [ -d "$release_out" ]; then
    if ((force)); then
        rm -rf "$release_out"
    else
        echo "$release_out already exists!"
        exit 1
    fi
fi

mkdir -p "$release_out"

# Build native packages
VERSION="$app_version" nfpm package -f nfpm.yaml -p deb -t "$release_out"
VERSION="$app_version" nfpm package -f nfpm.yaml -p rpm -t "$release_out"
VERSION="$app_version" nfpm package -f nfpm.yaml -p archlinux -t "$release_out"

# AppImage specific files
rm -rf "$appimage_root"
cp -a "$linux_root" "$appimage_root"
install -Dm644 "$linux_root/usr/share/applications/schildichat-revenge.desktop" "$appimage_root/schildichat-revenge.desktop"
install -Dm644 "$linux_root/usr/share/icons/hicolor/192x192/apps/schildichat-revenge.png" "$appimage_root/schildichat-revenge.png"
printf '#!/bin/sh\nexec "$(dirname "$0")/opt/schildichat-revenge/bin/schildichat-revenge" "$@"\n' > "$appimage_root/AppRun"
chmod 755 "$appimage_root/AppRun"

# Build appimage
ARCH=x86_64 appimagetool "$appimage_root" "$release_out"/"$appimage_outfile"
