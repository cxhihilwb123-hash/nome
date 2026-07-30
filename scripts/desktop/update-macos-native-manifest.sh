#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
readonly REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd -P)"
readonly RESOURCE_ROOT="$REPO_ROOT/apps/multiplatform/common/src/commonMain/cpp/desktop/libs/mac-aarch64"
readonly MANIFEST="$REPO_ROOT/apps/multiplatform/desktop/native/macos-arm64-native.sha256"
readonly TEMP_MANIFEST="$(mktemp "${TMPDIR:-/tmp}/nome-macos-native-manifest.XXXXXX")"

cleanup() {
  local status=$?
  trap - EXIT HUP INT TERM
  rm -f "$TEMP_MANIFEST"
  exit "$status"
}
trap cleanup EXIT HUP INT TERM

[[ -d "$RESOURCE_ROOT" && ! -L "$RESOURCE_ROOT" ]] || {
  printf 'Missing or unsafe macOS native resource tree: %s\n' "$RESOURCE_ROOT" >&2
  exit 1
}

symlink_candidate="$(find "$RESOURCE_ROOT" -type l -print -quit)"
[[ -z "$symlink_candidate" ]] || {
  printf 'Native resource tree contains a symbolic link: %s\n' "$symlink_candidate" >&2
  exit 1
}

unsupported_node="$(
  find "$RESOURCE_ROOT" ! -type d ! -type f ! -type l -print -quit
)"
[[ -z "$unsupported_node" ]] || {
  printf 'Native resource tree contains an unsupported node: %s\n' "$unsupported_node" >&2
  exit 1
}

while IFS= read -r resource; do
  relative_name="${resource#"$RESOURCE_ROOT"/}"
  [[ "$relative_name" == "libapp-lib.dylib" ]] && continue
  [[ -n "$relative_name" &&
    "$relative_name" != /* &&
    "$relative_name" != *$'\n'* &&
    "$relative_name" != *$'\t'* &&
    "$relative_name" != *\\* ]] || {
    printf 'Unsafe native resource path: %s\n' "$relative_name" >&2
    exit 1
  }
  hash="$(shasum -a 256 "$resource" | awk '{print tolower($1)}')"
  printf '%s  %s\n' "$hash" "$relative_name"
done < <(find "$RESOURCE_ROOT" -type f | LC_ALL=C sort) >"$TEMP_MANIFEST"

grep -Eq '^[0-9a-f]{64}  libsimplex\.dylib$' "$TEMP_MANIFEST" || {
  printf 'Generated manifest does not contain libsimplex.dylib\n' >&2
  exit 1
}
if grep -Fq '  libapp-lib.dylib' "$TEMP_MANIFEST"; then
  printf 'Generated manifest must not contain libapp-lib.dylib\n' >&2
  exit 1
fi

mv "$TEMP_MANIFEST" "$MANIFEST"
trap - EXIT HUP INT TERM
printf 'Updated %s\n' "$MANIFEST"
shasum -a 256 "$MANIFEST"
