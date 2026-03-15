#!/bin/bash
# I should be able to figure out how to do this via gradle directly,
# but I need to invest some time how to properly copy the missing files
# over and stuff without clearing anything else.

set -e

appimage_src="composeApp/build/compose/binaries/main/app/SchildiChatRevenge"
rust_target="${RUST_TARGET:-}"

if [ -n "$rust_target" ]; then
    case "$rust_target" in
        x86_64-unknown-linux-gnu) appimage_arch="x86_64" ;;
        aarch64-unknown-linux-gnu)
            appimage_arch="aarch64"
            export CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER=aarch64-linux-gnu-gcc
            export CC_aarch64_unknown_linux_gnu=aarch64-linux-gnu-gcc
            export AR_aarch64_unknown_linux_gnu=aarch64-linux-gnu-ar
            ;;
        *)
            echo "Unsupported AppImage rust target: $rust_target" >&2
            exit 1
            ;;
    esac
else
    appimage_arch=$(uname -m)
    case "$appimage_arch" in
        arm64) appimage_arch="aarch64" ;;
        amd64) appimage_arch="x86_64" ;;
    esac
fi

gradle_args=()
if [ -n "$rust_target" ]; then
    gradle_args+=("-PrustTarget=$rust_target")
fi

app_version=`date -u +%y.%m.%d`
outfile="SchildiChatRevenge-$appimage_arch-$app_version.AppImage"
echo "Building $outfile"

./gradlew clean
./gradlew packageAppImage "${gradle_args[@]}"
cp "launcher/SchildiChatRevenge.desktop" "$appimage_src/"
cp "composeApp/src/jvmMain/composeResources/drawable-hdpi/ic_launcher.png" "$appimage_src/"
ln -s -r "$appimage_src/bin/SchildiChatRevenge" "$appimage_src/AppRun"

mkdir -p release
ARCH="$appimage_arch" appimagetool "$appimage_src" release/"$outfile"

ln -s -r -f release/"$outfile" release/"SchildiChatRevenge-$appimage_arch-latest.AppImage"
