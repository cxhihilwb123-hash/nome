#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
readonly REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd -P)"
readonly PATCH_LIBAPP="$REPO_ROOT/apps/multiplatform/common/src/commonMain/cpp/desktop/patch-libapp-mac.sh"
readonly TEMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/nome-patch-libapp-test.XXXXXX")"

cleanup() {
  local status=$?
  trap - EXIT HUP INT TERM
  rm -rf "$TEMP_DIR"
  exit "$status"
}
trap cleanup EXIT HUP INT TERM

cat >"$TEMP_DIR/lib.c" <<'EOF'
int nome_patch_libapp_fixture(void) { return 0; }
EOF

/usr/bin/xcrun clang \
  -dynamiclib \
  -arch arm64 \
  -Wl,-rpath,"$TEMP_DIR/absolute build rpath" \
  -Wl,-rpath,@loader_path \
  "$TEMP_DIR/lib.c" \
  -o "$TEMP_DIR/libapp-lib.dylib"

bash "$PATCH_LIBAPP" "$TEMP_DIR/libapp-lib.dylib"

rpaths="$(
  /usr/bin/otool -l "$TEMP_DIR/libapp-lib.dylib" | /usr/bin/awk '
    $1 == "cmd" && $2 == "LC_RPATH" { inside_rpath = 1; next }
    inside_rpath && $1 == "path" {
      rpath = $0
      sub(/^[[:space:]]*path[[:space:]]+/, "", rpath)
      sub(/[[:space:]]+\(offset[[:space:]]+[0-9]+\)[[:space:]]*$/, "", rpath)
      print rpath
      inside_rpath = 0
    }
  '
)"
[[ "$rpaths" == "@loader_path" ]] || {
  printf 'Unexpected patched RPATH set: %s\n' "$rpaths" >&2
  exit 1
}

printf 'patch-libapp-mac.sh removed arbitrary absolute RPATHs and kept @loader_path\n'
