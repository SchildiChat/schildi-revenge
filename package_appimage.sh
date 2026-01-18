#!/bin/bash
# I should be able to figure out how to do this via gradle directly,
# but I need to invest some time how to properly copy the missing files
# over and stuff without clearing anything else.

set -e

appimage_src="composeApp/build/compose/binaries/main/app/SchildiChatRevenge"

app_version=`cat composeApp/build.gradle.kts|grep packageVersion|sed 's|.*= "\(.*\)"|\1|'`

./gradlew clean
./gradlew packageAppImage
cp "launcher/SchildiChatRevenge.desktop" "$appimage_src/"
cp "composeApp/src/jvmMain/composeResources/drawable-hdpi/ic_launcher.png" "$appimage_src/"
ln -s -r "$appimage_src/bin/SchildiChatRevenge" "$appimage_src/AppRun"

mkdir -p release
ARCH=x86_64 appimagetool "$appimage_src" release/"SchildiChatRevenge-x86_64-$app_version.AppImage"
