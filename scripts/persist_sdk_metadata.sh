#!/bin/bash

set -e

mydir="$(dirname "$(realpath "$0")")"
element_path="$1"

get_prop() {
    local prop="$1"
    local file="$2"
    if [ -z "$file" ]; then
      file="$build_gradle"
    fi
    cat "$file" | grep "$prop = " | sed "s|.*$prop = ||"
}

version_kt="$element_path/plugins/src/main/kotlin/Versions.kt"
elVersionYear=`get_prop versionYear "$version_kt"`
elVersionMonth=`get_prop versionMonth "$version_kt"`
elVersionRelNumber=`get_prop versionReleaseNumber "$version_kt"`
elVersion="${elVersionYear}.${elVersionMonth}.${elVersionRelNumber}"

pushd "$element_path" > /dev/null
revision_commit=`git rev-parse HEAD`
popd > /dev/null

echo "package chat.schildi.revenge

object MatrixSdkMetadata {
    const val ELEMENT_VERSION = \"$elVersion\"
    const val SCHILDI_NEXT_REVISION = \"$revision_commit\"
}
" > "$mydir/../matrix/src/main/java/chat/schildi/revenge/MatrixSdkMetadata.kt"
