#!/usr/bin/env bash

set -Eeuo pipefail

readonly lib="${1:-libapp-lib.dylib}"

if [[ ! -f "$lib" || -L "$lib" ]]; then
  printf 'Unsafe or missing macOS native library: %s\n' "$lib" >&2
  exit 1
fi

list_rpaths() {
  /usr/bin/otool -l "$1" | /usr/bin/awk '
    $1 == "cmd" && $2 == "LC_RPATH" { inside_rpath = 1; next }
    inside_rpath && $1 == "path" {
      rpath = $0
      sub(/^[[:space:]]*path[[:space:]]+/, "", rpath)
      sub(/[[:space:]]+\(offset[[:space:]]+[0-9]+\)[[:space:]]*$/, "", rpath)
      print rpath
      inside_rpath = 0
    }
  '
}

while IFS= read -r rpath; do
  [[ "$rpath" == /* ]] || continue
  /usr/bin/install_name_tool -delete_rpath "$rpath" "$lib"
done < <(list_rpaths "$lib")

remaining_absolute_rpaths="$(list_rpaths "$lib" | /usr/bin/awk '/^\//')"
if [[ -n "$remaining_absolute_rpaths" ]]; then
  printf 'Absolute LC_RPATH entries remain in %s:\n%s\n' \
    "$lib" "$remaining_absolute_rpaths" >&2
  exit 1
fi
