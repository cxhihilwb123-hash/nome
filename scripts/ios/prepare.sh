#!/bin/sh

set -eu

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
mac2ios_bin="${MAC2IOS:-}"

cd "$root_dir"

if [ -z "$mac2ios_bin" ]; then
  if command -v mac2ios >/dev/null 2>&1; then
    mac2ios_bin="$(command -v mac2ios)"
  elif [ -x "$root_dir/tools/bin/mac2ios" ]; then
    mac2ios_bin="$root_dir/tools/bin/mac2ios"
  fi
fi

if [ -z "$mac2ios_bin" ] || [ ! -x "$mac2ios_bin" ]; then
  echo "mac2ios is required before preparing iOS libraries" >&2
  exit 1
fi

if [ ! -f "$HOME/Downloads/pkg-ios-aarch64-swift-json.zip" ]; then
  echo "Missing $HOME/Downloads/pkg-ios-aarch64-swift-json.zip" >&2
  exit 1
fi

# the binaries folder should be in ~/Downloads folder
rm -rf ./apps/ios/Libraries/mac ./apps/ios/Libraries/ios ./apps/ios/Libraries/sim
mkdir -p ./apps/ios/Libraries/mac ./apps/ios/Libraries/ios ./apps/ios/Libraries/sim
unzip -o ~/Downloads/pkg-ios-aarch64-swift-json.zip -d ./apps/ios/Libraries/mac
chmod +w ./apps/ios/Libraries/mac/*
cp ./apps/ios/Libraries/mac/* ./apps/ios/Libraries/ios
cp ./apps/ios/Libraries/mac/* ./apps/ios/Libraries/sim
for f in ./apps/ios/Libraries/ios/*; do "$mac2ios_bin" "$f"; done | wc -l
for f in ./apps/ios/Libraries/sim/*; do "$mac2ios_bin" -s "$f"; done | wc -l
