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

if [ ! -d "$HOME/Downloads/pkg-ios-aarch64-swift-json" ]; then
  echo "Missing $HOME/Downloads/pkg-ios-aarch64-swift-json" >&2
  exit 1
fi

if [ ! -d "$HOME/Downloads/pkg-ios-x86_64-swift-json" ]; then
  echo "Missing $HOME/Downloads/pkg-ios-x86_64-swift-json" >&2
  exit 1
fi

# the binaries folders should be in ~/Downloads folder
rm -rf ./apps/ios/Libraries/mac-aarch64 ./apps/ios/Libraries/mac-x86_64 ./apps/ios/Libraries/ios ./apps/ios/Libraries/sim
mkdir -p ./apps/ios/Libraries/mac-aarch64 ./apps/ios/Libraries/mac-x86_64 ./apps/ios/Libraries/ios ./apps/ios/Libraries/sim
cp ~/Downloads/pkg-ios-aarch64-swift-json/* ./apps/ios/Libraries/mac-aarch64
cp ~/Downloads/pkg-ios-x86_64-swift-json/* ./apps/ios/Libraries/mac-x86_64
chmod +w ./apps/ios/Libraries/mac-aarch64/*
chmod +w ./apps/ios/Libraries/mac-x86_64/*
cp ./apps/ios/Libraries/mac-aarch64/* ./apps/ios/Libraries/ios
cp ./apps/ios/Libraries/mac-x86_64/* ./apps/ios/Libraries/sim
for f in ./apps/ios/Libraries/ios/*; do "$mac2ios_bin" "$f"; done | wc -l
for f in ./apps/ios/Libraries/sim/*; do "$mac2ios_bin" -s "$f"; done | wc -l
