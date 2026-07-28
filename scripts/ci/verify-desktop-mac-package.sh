#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

die() {
  printf 'Nome macOS package verification error: %s\n' "$*" >&2
  exit 1
}

usage() {
  cat >&2 <<'EOF'
Usage:
  verify-desktop-mac-package.sh (--app PATH | --dmg PATH) \
    --source-resources PATH --manifest PATH --manifest-sha256 SHA256 \
    --team-id TEAM_ID
EOF
  exit 2
}

app_path=""
dmg_path=""
manifest_path=""
reviewed_manifest_sha256=""
source_resources=""
team_id=""

while (($#)); do
  case "$1" in
    --app)
      (($# >= 2)) || usage
      app_path="$2"
      shift 2
      ;;
    --dmg)
      (($# >= 2)) || usage
      dmg_path="$2"
      shift 2
      ;;
    --manifest)
      (($# >= 2)) || usage
      manifest_path="$2"
      shift 2
      ;;
    --source-resources)
      (($# >= 2)) || usage
      source_resources="$2"
      shift 2
      ;;
    --manifest-sha256)
      (($# >= 2)) || usage
      reviewed_manifest_sha256="$2"
      shift 2
      ;;
    --team-id)
      (($# >= 2)) || usage
      team_id="$2"
      shift 2
      ;;
    *)
      usage
      ;;
  esac
done

[[ -n "$app_path" || -n "$dmg_path" ]] || usage
[[ -z "$app_path" || -z "$dmg_path" ]] || usage
[[ -n "$manifest_path" &&
  -n "$reviewed_manifest_sha256" &&
  -n "$source_resources" &&
  -n "$team_id" ]] || usage
[[ "$reviewed_manifest_sha256" =~ ^[0-9a-fA-F]{64}$ ]] ||
  die "reviewed manifest SHA-256 is invalid"
[[ "$team_id" =~ ^[A-Z0-9]{10}$ ]] ||
  die "Apple Team ID is invalid"
[[ -f "$manifest_path" && ! -L "$manifest_path" ]] ||
  die "native manifest is missing or unsafe"
[[ -d "$source_resources" && ! -L "$source_resources" ]] ||
  die "reviewed native resource tree is missing or unsafe"

actual_manifest_sha256="$(
  shasum -a 256 "$manifest_path" | awk '{print tolower($1)}'
)"
readonly REVIEWED_MANIFEST_SHA256="$(
  printf '%s' "$reviewed_manifest_sha256" |
    tr '[:upper:]' '[:lower:]'
)"
[[ "$actual_manifest_sha256" == "$REVIEWED_MANIFEST_SHA256" ]] ||
  die "native manifest does not match the reviewed SHA-256"

readonly TEMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/nome-package-verify.XXXXXX")"
mount_point=""

cleanup() {
  local status=$?
  trap - EXIT HUP INT TERM
  set +e
  if [[ -n "$mount_point" && -d "$mount_point" ]]; then
    hdiutil detach "$mount_point" -quiet >/dev/null 2>&1
  fi
  rm -rf "$TEMP_DIR"
  exit "$status"
}

trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

if [[ -n "$dmg_path" ]]; then
  [[ -f "$dmg_path" && ! -L "$dmg_path" ]] ||
    die "DMG is missing or unsafe: $dmg_path"
  hdiutil verify "$dmg_path" >/dev/null
  xcrun stapler validate "$dmg_path" >/dev/null
  mount_point="$TEMP_DIR/mount"
  mkdir "$mount_point"
  hdiutil attach \
    -readonly \
    -nobrowse \
    -mountpoint "$mount_point" \
    "$dmg_path" >/dev/null

  app_candidates=()
  while IFS= read -r -d '' candidate; do
    app_candidates+=("$candidate")
  done < <(find "$mount_point" -maxdepth 3 -type d -name 'Nome.app' -print0)
  ((${#app_candidates[@]} == 1)) ||
    die "DMG must contain exactly one Nome.app"
  app_path="${app_candidates[0]}"
fi

[[ -d "$app_path" && ! -L "$app_path" ]] ||
  die "Nome.app is missing or unsafe: $app_path"
[[ "$(basename "$app_path")" == "Nome.app" ]] ||
  die "application bundle must be named Nome.app"

readonly INFO_PLIST="$app_path/Contents/Info.plist"
readonly RESOURCES="$app_path/Contents/app/resources"
[[ -f "$INFO_PLIST" && ! -L "$INFO_PLIST" ]] ||
  die "Nome.app Info.plist is missing or unsafe"
[[ -d "$RESOURCES" && ! -L "$RESOURCES" ]] ||
  die "Nome.app native resources are missing or unsafe"

bundle_id="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$INFO_PLIST")"
[[ "$bundle_id" == "chat.nome.app" ]] ||
  die "unexpected macOS bundle identifier: $bundle_id"

codesign --verify --deep --strict --verbose=2 "$app_path"
signing_details="$(codesign -dv --verbose=4 "$app_path" 2>&1)"
actual_team_id="$(
  awk -F= '$1 == "TeamIdentifier" { print $2; exit }' <<<"$signing_details"
)"
[[ "$actual_team_id" == "$team_id" ]] ||
  die "Nome.app is not signed by the configured Apple Team ID"

symlink_candidate="$(find "$RESOURCES" -type l -print -quit)"
if [[ -n "$symlink_candidate" ]]; then
  die "packaged native resources must not contain symbolic links"
fi
source_symlink_candidate="$(find "$source_resources" -type l -print -quit)"
if [[ -n "$source_symlink_candidate" ]]; then
  die "reviewed native resources must not contain symbolic links"
fi
unsupported_packaged_node="$(
  find "$RESOURCES" ! -type d ! -type f ! -type l -print -quit
)"
[[ -z "$unsupported_packaged_node" ]] ||
  die "packaged native resources contain an unsupported filesystem node"
unsupported_source_node="$(
  find "$source_resources" ! -type d ! -type f ! -type l -print -quit
)"
[[ -z "$unsupported_source_node" ]] ||
  die "reviewed native resources contain an unsupported filesystem node"

readonly EXPECTED_LIST="$TEMP_DIR/expected-files"
readonly ACTUAL_LIST="$TEMP_DIR/actual-files"
readonly SOURCE_LIST="$TEMP_DIR/source-files"
readonly RECORDS="$TEMP_DIR/manifest-records"
: >"$EXPECTED_LIST"
: >"$ACTUAL_LIST"
: >"$SOURCE_LIST"
: >"$RECORDS"

line_number=0
while IFS= read -r line || [[ -n "$line" ]]; do
  line_number=$((line_number + 1))
  [[ -z "$line" || "$line" == \#* ]] && continue
  ((${#line} > 66)) ||
    die "invalid native manifest line $line_number"
  hash="${line:0:64}"
  separator="${line:64:2}"
  relative_name="${line:66}"
  [[ "$hash" =~ ^[0-9a-fA-F]{64}$ && "$separator" == "  " ]] ||
    die "invalid native manifest line $line_number"
  [[ -n "$relative_name" &&
    "$relative_name" != /* &&
    "$relative_name" != */ &&
    "$relative_name" != *\\* &&
    "$relative_name" != *$'\t'* ]] ||
    die "unsafe native manifest path at line $line_number"

  IFS='/' read -r -a path_parts <<<"$relative_name"
  for path_part in "${path_parts[@]}"; do
    [[ -n "$path_part" && "$path_part" != "." && "$path_part" != ".." ]] ||
      die "unsafe native manifest path at line $line_number"
  done

  if grep -Fxq -- "$relative_name" "$EXPECTED_LIST"; then
    die "duplicate native manifest entry: $relative_name"
  fi
  normalized_hash="$(printf '%s' "$hash" | tr '[:upper:]' '[:lower:]')"
  printf '%s\n' "$relative_name" >>"$EXPECTED_LIST"
  printf '%s\t%s\n' "$normalized_hash" "$relative_name" >>"$RECORDS"
done <"$manifest_path"

grep -Fxq 'libsimplex.dylib' "$EXPECTED_LIST" ||
  die "native manifest does not contain libsimplex.dylib"
grep -Fxq 'libapp-lib.dylib' "$EXPECTED_LIST" ||
  die "native manifest does not contain libapp-lib.dylib"

while IFS= read -r -d '' resource; do
  relative_name="${resource#"$RESOURCES"/}"
  [[ "$relative_name" != *$'\n'* && "$relative_name" != *$'\t'* ]] ||
    die "packaged resource path contains unsupported characters"
  printf '%s\n' "$relative_name" >>"$ACTUAL_LIST"
done < <(find "$RESOURCES" -type f -print0)

while IFS= read -r -d '' resource; do
  relative_name="${resource#"$source_resources"/}"
  [[ "$relative_name" != *$'\n'* && "$relative_name" != *$'\t'* ]] ||
    die "reviewed resource path contains unsupported characters"
  printf '%s\n' "$relative_name" >>"$SOURCE_LIST"
done < <(find "$source_resources" -type f -print0)

LC_ALL=C sort -o "$EXPECTED_LIST" "$EXPECTED_LIST"
LC_ALL=C sort -o "$ACTUAL_LIST" "$ACTUAL_LIST"
LC_ALL=C sort -o "$SOURCE_LIST" "$SOURCE_LIST"
if ! cmp -s "$EXPECTED_LIST" "$ACTUAL_LIST"; then
  comm -3 "$EXPECTED_LIST" "$ACTUAL_LIST" >&2 || true
  die "packaged native resource set does not exactly match the manifest"
fi
if ! cmp -s "$EXPECTED_LIST" "$SOURCE_LIST"; then
  comm -3 "$EXPECTED_LIST" "$SOURCE_LIST" >&2 || true
  die "reviewed native resource set does not exactly match the manifest"
fi

compare_macho_payload() {
  python3 - "$1" "$2" <<'PY'
import struct
import sys

LC_SEGMENT_64 = 0x19
LC_CODE_SIGNATURE = 0x1D
MH_MAGIC_64 = 0xFEEDFACF
CPU_TYPE_ARM64 = 0x0100000C


def normalized_code(path):
    with open(path, "rb") as file:
        data = bytearray(file.read())
    if len(data) < 32:
        raise ValueError("truncated Mach-O")
    magic, cpu_type = struct.unpack_from("<Ii", data, 0)
    if magic != MH_MAGIC_64 or cpu_type != CPU_TYPE_ARM64:
        raise ValueError("not a thin ARM64 Mach-O")
    command_count = struct.unpack_from("<I", data, 16)[0]
    command_offset = 32
    signature_offset = None
    signature_command_offset = None
    for _ in range(command_count):
        if command_offset + 8 > len(data):
            raise ValueError("truncated Mach-O load commands")
        command, command_size = struct.unpack_from("<II", data, command_offset)
        if command_size < 8 or command_offset + command_size > len(data):
            raise ValueError("invalid Mach-O load command")
        if command == LC_SEGMENT_64 and command_size >= 72:
            segment_name = bytes(data[command_offset + 8:command_offset + 24])
            if segment_name.rstrip(b"\0") == b"__LINKEDIT":
                data[command_offset + 32:command_offset + 40] = b"\0" * 8
                data[command_offset + 48:command_offset + 56] = b"\0" * 8
        if command == LC_CODE_SIGNATURE:
            if command_size != 16 or signature_offset is not None:
                raise ValueError("invalid Mach-O code signature command")
            signature_offset = struct.unpack_from("<I", data, command_offset + 8)[0]
            signature_command_offset = command_offset
        command_offset += command_size
    if signature_offset is None or signature_command_offset is None:
        raise ValueError("Mach-O has no code signature command")
    if signature_offset < command_offset or signature_offset > len(data):
        raise ValueError("invalid Mach-O code signature offset")
    data[signature_command_offset:signature_command_offset + 16] = b"\0" * 16
    return bytes(data[:signature_offset]), signature_offset


source_payload, source_signature_offset = normalized_code(sys.argv[1])
packaged_payload, packaged_signature_offset = normalized_code(sys.argv[2])
if source_signature_offset != packaged_signature_offset:
    raise SystemExit("code signature changed the Mach-O payload boundary")
if source_payload != packaged_payload:
    raise SystemExit("packaged Mach-O payload differs from reviewed source")
PY
}

while IFS=$'\t' read -r expected_hash relative_name; do
  source_resource="$source_resources/$relative_name"
  resource="$RESOURCES/$relative_name"
  [[ -f "$source_resource" && ! -L "$source_resource" ]] ||
    die "manifest entry is not a regular reviewed file: $relative_name"
  [[ -f "$resource" && ! -L "$resource" ]] ||
    die "manifest entry is not a regular packaged file: $relative_name"
  source_hash="$(shasum -a 256 "$source_resource" | awk '{print tolower($1)}')"
  [[ "$source_hash" == "$expected_hash" ]] ||
    die "reviewed resource hash mismatch: $relative_name"

  if [[ "$relative_name" == *.dylib ]]; then
    codesign --verify --strict --verbose=2 "$resource"
    architectures="$(lipo -archs "$resource")"
    [[ "$architectures" == "arm64" ]] ||
      die "dylib is not thin ARM64: $relative_name ($architectures)"
    file_description="$(file -b "$resource")"
    [[ "$file_description" == *"Mach-O 64-bit dynamically linked shared library arm64"* ]] ||
      die "file is not an ARM64 Mach-O dylib: $relative_name"
    compare_macho_payload "$source_resource" "$resource" ||
      die "packaged dylib payload mismatch: $relative_name"
  else
    actual_hash="$(shasum -a 256 "$resource" | awk '{print tolower($1)}')"
    [[ "$actual_hash" == "$expected_hash" ]] ||
      die "packaged resource hash mismatch: $relative_name"
  fi
done <"$RECORDS"

printf 'Verified Nome.app native resources against reviewed manifest %s\n' \
  "$REVIEWED_MANIFEST_SHA256"
