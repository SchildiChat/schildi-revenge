#!/usr/bin/bash

mydir="$(dirname "$(realpath "$0")")"
cd "$mydir"

set -e
#set -x

upstream_base="$HOME/schildichat-dev/next"
downstream_subtree_base="matrix"
downstream_subtree_common="src/main/java"
downstream_base="$downstream_subtree_base/$downstream_subtree_common"
elex_imports_file="$mydir/elex_imports.txt"
old_head=6aeb9742ef008ae815760c025c8f2d6045cb1c57
new_head=4f810430d92ac128a6b0ea7a104f6a6009f2a5cb

rm "$elex_imports_file" || true
echo LICENSE > "$elex_imports_file"

patch_package() {
    local upstream="$1"
    local relative="$2"
    echo "$upstream/$relative/" >> "$elex_imports_file"
    echo "$upstream/$relative/==>$downstream_subtree_common/$relative/" >> "$elex_imports_file"
}

patch_partial() {
    local upstream="$1"
    local relative="$2"
    local patch_name="$(echo "$relative" | sed 's|/src/.*||;s|/|_|g').patch"
    local patch_file="$patch_dir/$patch_name"

    pushd "$downstream_base/$relative" > /dev/null
    find -type f | sed "s|\\./\\(.*\\)|$upstream/$relative/\\1\\n$upstream/$relative/\\1==>$downstream_subtree_common/$relative/\\1|" >> "$elex_imports_file"
    popd > /dev/null
}

patch_partial appconfig/src/main/kotlin io/element/android/appconfig
patch_package appnav/src/main/kotlin io/element/android/appnav/di

patch_partial features/messages/impl/src/main/kotlin io/element/android/features/messages/impl/timeline

patch_partial libraries/androidutils/src/main/kotlin io/element/android/libraries/androidutils
patch_partial libraries/architecture/src/main/kotlin io/element/android/libraries/architecture
patch_package libraries/core/src/main/kotlin io/element/android/libraries/core
patch_package libraries/di/src/main/kotlin io/element/android/libraries/di
#patch_package libraries/featureflag/api/src/main/kotlin io/element/android/libraries/featureflag/api
patch_package libraries/matrix/api/src/main/kotlin io/element/android/libraries/matrix/api
patch_package libraries/matrix/impl/src/main/kotlin io/element/android/libraries/matrix/impl
patch_package libraries/session-storage/api/src/main/kotlin io/element/android/libraries/sessionstorage/api

patch_partial app/src/main/kotlin io/element/android/x/di
