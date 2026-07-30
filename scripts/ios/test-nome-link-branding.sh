#!/bin/sh

set -eu

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
test_tmp="$(mktemp -d /tmp/nome-link-branding-tests.XXXXXX)"
trap 'rm -rf "$test_tmp"' EXIT HUP INT TERM

DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}" \
    CLANG_MODULE_CACHE_PATH="$test_tmp/module-cache" \
    /usr/bin/xcrun --sdk macosx swiftc \
    -module-cache-path "$test_tmp/module-cache" \
    "$root_dir/apps/ios/SimpleXChat/NomeLinkBranding.swift" \
    "$root_dir/apps/ios/PolicyTests/NomeLinkBrandingTests.swift" \
    -o "$test_tmp/nome-link-branding-tests"

"$test_tmp/nome-link-branding-tests"
