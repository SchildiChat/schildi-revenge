#!/bin/bash
# I should be able to figure out how to do this via gradle directly,
# but I need to invest some time how to properly copy the missing files
# over and stuff without clearing anything else.

set -e

appimage_src="composeApp/build/compose/binaries/main-release/app/schildichat-revenge"

app_version=`date -u +%y.%m.%d`
outfile="schildichat-revenge-x86_64-$app_version.AppImage"
echo "Building $outfile"

./gradlew clean
./gradlew packageReleaseAppImage
cp "launcher/schildichat-revenge.desktop" "$appimage_src/schildichat-revenge.desktop"
cp "composeApp/src/jvmMain/composeResources/drawable-hdpi/ic_launcher.png" "$appimage_src/schildichat-revenge.png"
printf '#!/bin/sh\nexec "$(dirname "$0")/bin/schildichat-revenge" "$@"\n' > "$appimage_src/AppRun"
chmod 755 "$appimage_src/AppRun"

mkdir -p release
ARCH=x86_64 appimagetool "$appimage_src" release/"$outfile"

ln -s -r -f release/"$outfile" release/"schildichat-revenge-x86_64-latest.AppImage"
