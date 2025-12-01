#!/bin/bash

# Dependencies:
# inkscape for svg -> png conversions
# imagemagick for additional png operations

mydir="$(dirname "$(realpath "$0")")"

extension=".png"

export_files() {
    newfile="$(basename "$file" .svg)$extension"
    export_files_custom "$newfile" -C "$@"
}
export_files_custom() {
    newfile="$1"
    shift
    mkdir -p $base_folder-mdpi
    mkdir -p $base_folder-hdpi
    mkdir -p $base_folder-xhdpi
    mkdir -p $base_folder-xxhdpi
    mkdir -p $base_folder-xxxhdpi
    inkscape "$file" --export-filename="$base_folder-mdpi/$newfile" --export-dpi=$dpi "$@"
    inkscape "$file" --export-filename="$base_folder-hdpi/$newfile" --export-dpi=$(($dpi*3/2)) "$@"
    inkscape "$file" --export-filename="$base_folder-xhdpi/$newfile" --export-dpi=$(($dpi*2)) "$@"
    inkscape "$file" --export-filename="$base_folder-xxhdpi/$newfile" --export-dpi=$(($dpi*3)) "$@"
    inkscape "$file" --export-filename="$base_folder-xxxhdpi/$newfile" --export-dpi=$(($dpi*4)) "$@"
}


# Launcher icon

dpi=192

base_folder="$mydir/../composeApp/src/jvmMain/composeResources/drawable"
file="$mydir/ic_launcher_foreground.svg"
export_files_custom "ic_launcher.png"
