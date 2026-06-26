#!/usr/bin/env bash
# Restore the staged-but-uncommitted SVG content of each ezgifframeNNN.xml,
# rasterize to PNG via ImageMagick, then replace the XMLs in res/drawable.
set -euo pipefail

DRAWABLE_DIR="androidApp/src/main/res/drawable"
TMPDIR="$(mktemp -d)"
trap 'rm -rf "$TMPDIR"' EXIT

MAGICK='/c/Program Files/ImageMagick-7.1.2-Q16-HDRI/magick.exe'

for f in "$DRAWABLE_DIR"/ezgifframe*.xml; do
  name="$(basename "$f" .xml)"
  svg="$TMPDIR/$name.svg"
  png="$DRAWABLE_DIR/$name.png"
  git show ":$f" > "$svg"
  "$MAGICK" -background none -density 200 "$svg" -resize 256x256 "$png"
  rm "$f"
  git rm --cached -- "$f" >/dev/null 2>&1 || true
  echo "ok $name"
done
