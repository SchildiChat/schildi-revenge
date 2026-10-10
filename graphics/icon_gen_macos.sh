#!/bin/bash

set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "error: macOS icon generation requires macOS" >&2
    exit 1
fi

for tool in resvg iconutil; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "error: required tool not found: $tool" >&2
        exit 1
    fi
done

mydir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source_svg="$mydir/ic_launcher.svg"
output_icns="$mydir/ic_launcher.icns"

if [[ ! -f "$source_svg" ]]; then
    echo "error: source SVG not found: $source_svg" >&2
    exit 1
fi

temporary_directory="$(mktemp -d "${TMPDIR:-/tmp}/schildichat-icon.XXXXXX")"
iconset="$temporary_directory/ic_launcher.iconset"
trap 'rm -rf "$temporary_directory"' EXIT
mkdir "$iconset"

render_icon() {
    local points="$1"
    local scale="$2"
    local pixels=$((points * scale))
    local suffix=""

    if (( scale == 2 )); then
        suffix="@2x"
    fi

    resvg \
        --width "$pixels" \
        --height "$pixels" \
        "$source_svg" \
        "$iconset/icon_${points}x${points}${suffix}.png"
}

for points in 16 32 128 256 512; do
    render_icon "$points" 1
    render_icon "$points" 2
done

iconutil --convert icns --output "$output_icns" "$iconset"
echo "Generated $output_icns from $source_svg"
