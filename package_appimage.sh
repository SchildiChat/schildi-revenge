#!/bin/bash
# I should be able to figure out how to do this via gradle directly,
# but I need to invest some time how to properly copy the missing files
# over and stuff without clearing anything else.

set -e

appimage_src="composeApp/build/compose/binaries/main/app/SchildiChatRevenge"

app_version=`date -u +%y.%m.%d`
outfile="SchildiChatRevenge-x86_64-$app_version.AppImage"
echo "Building $outfile"

./gradlew clean
./gradlew packageAppImage
cp "launcher/SchildiChatRevenge.desktop" "$appimage_src/"
cp "composeApp/src/jvmMain/composeResources/drawable-hdpi/ic_launcher.png" "$appimage_src/"
ln -s -r "$appimage_src/bin/SchildiChatRevenge" "$appimage_src/AppRun"

mkdir -p release
ARCH=x86_64 appimagetool "$appimage_src" release/"$outfile"

ln -s -r -f release/"$outfile" release/"SchildiChatRevenge-x86_64-latest.AppImage"
