#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
readonly REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd -P)"
readonly LOCAL_PROPERTIES="$REPO_ROOT/apps/multiplatform/local.properties"
readonly NATIVE_MANIFEST="$REPO_ROOT/apps/multiplatform/desktop/native/macos-arm64-native.sha256"
readonly NATIVE_RESOURCES="$REPO_ROOT/apps/multiplatform/common/src/commonMain/cpp/desktop/libs/mac-aarch64"
readonly GENERATED_LIBAPP="$REPO_ROOT/apps/multiplatform/desktop/build/cmake/main/mac-aarch64/libapp-lib.dylib"
readonly TEMP_ROOT="${RUNNER_TEMP:-${TMPDIR:-/tmp}}"

die() {
  printf 'Nome macOS release error: %s\n' "$*" >&2
  exit 1
}

require_env() {
  local name="$1"
  local value="${!name:-}"
  [[ -n "$value" ]] || die "required environment variable $name is not set"
  [[ "$value" != *$'\n'* && "$value" != *$'\r'* ]] ||
    die "$name must not contain newline characters"
}

properties_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//$'\t'/\\t}"
  value="${value// /\\ }"
  value="${value//:/\\:}"
  value="${value//=/\\=}"
  value="${value//#/\\#}"
  value="${value//!/\\!}"
  printf '%s' "$value"
}

for required_name in \
  NOME_MACOS_SIGNING_IDENTITY \
  NOME_MACOS_TEAM_ID \
  NOME_MACOS_SIGNING_KEYCHAIN_BASE64 \
  NOME_MACOS_SIGNING_KEYCHAIN_PASSWORD \
  NOME_MACOS_NOTARIZATION_APPLE_ID \
  NOME_MACOS_NOTARIZATION_PASSWORD \
  NOME_MACOS_NATIVE_MANIFEST_SHA256
do
  require_env "$required_name"
done

[[ "$NOME_MACOS_TEAM_ID" =~ ^[A-Z0-9]{10}$ ]] ||
  die "NOME_MACOS_TEAM_ID must be a 10-character Apple Team ID"
[[ "$NOME_MACOS_SIGNING_IDENTITY" == "Developer ID Application: "* &&
  "$NOME_MACOS_SIGNING_IDENTITY" == *"($NOME_MACOS_TEAM_ID)" ]] ||
  die "signing identity must be a Developer ID Application identity for NOME_MACOS_TEAM_ID"
[[ "$NOME_MACOS_NATIVE_MANIFEST_SHA256" =~ ^[0-9a-fA-F]{64}$ ]] ||
  die "NOME_MACOS_NATIVE_MANIFEST_SHA256 must be a reviewed SHA-256"
[[ -d "$TEMP_ROOT" ]] || die "temporary directory does not exist: $TEMP_ROOT"
[[ -f "$NATIVE_MANIFEST" && ! -L "$NATIVE_MANIFEST" ]] ||
  die "reviewed native manifest is missing or unsafe: $NATIVE_MANIFEST"

readonly REVIEWED_MANIFEST_SHA256="$(
  printf '%s' "$NOME_MACOS_NATIVE_MANIFEST_SHA256" |
    tr '[:upper:]' '[:lower:]'
)"
readonly WORK_DIR="$(mktemp -d "$TEMP_ROOT/nome-macos-release.XXXXXX")"
readonly KEYCHAIN_PATH="$WORK_DIR/nome-signing.keychain-db"
readonly LOCAL_PROPERTIES_BACKUP="$WORK_DIR/local.properties.backup"
: >"$KEYCHAIN_PATH"
: >"$LOCAL_PROPERTIES_BACKUP"
local_properties_existed=0
local_properties_touched=0
keychain_loaded=0
keychain_list_captured=0
original_keychains=()

cleanup() {
  local status=$?
  trap - EXIT HUP INT TERM
  set +e

  if ((keychain_list_captured)); then
    security list-keychains -d user -s "${original_keychains[@]}" >/dev/null 2>&1
  fi
  if ((keychain_loaded)); then
    security lock-keychain "$KEYCHAIN_PATH" >/dev/null 2>&1
    security delete-keychain "$KEYCHAIN_PATH" >/dev/null 2>&1
  fi
  rm -f "$KEYCHAIN_PATH"

  if ((local_properties_touched)); then
    if ((local_properties_existed)); then
      mv -f "$LOCAL_PROPERTIES_BACKUP" "$LOCAL_PROPERTIES"
    else
      rm -f "$LOCAL_PROPERTIES" "$LOCAL_PROPERTIES_BACKUP"
    fi
  else
    rm -f "$LOCAL_PROPERTIES_BACKUP"
  fi
  rmdir "$WORK_DIR" >/dev/null 2>&1

  exit "$status"
}

trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

original_keychain_output="$(
  security list-keychains -d user
)" || die "could not read the current user keychain search list"
while IFS= read -r keychain; do
  keychain="${keychain#"${keychain%%[![:space:]]*}"}"
  keychain="${keychain#\"}"
  keychain="${keychain%\"}"
  [[ -n "$keychain" ]] && original_keychains+=("$keychain")
done <<<"$original_keychain_output"
keychain_list_captured=1

if [[ -e "$LOCAL_PROPERTIES" ]]; then
  [[ -f "$LOCAL_PROPERTIES" && ! -L "$LOCAL_PROPERTIES" ]] ||
    die "refusing to replace unsafe local.properties: $LOCAL_PROPERTIES"
  cp -p "$LOCAL_PROPERTIES" "$LOCAL_PROPERTIES_BACKUP"
  local_properties_existed=1
  local_properties_touched=1
else
  local_properties_touched=1
  : >"$LOCAL_PROPERTIES"
fi

chmod 600 "$LOCAL_PROPERTIES" "$LOCAL_PROPERTIES_BACKUP" "$KEYCHAIN_PATH"
{
  printf '\n'
  printf 'desktop.mac.signing.identity=%s\n' \
    "$(properties_escape "$NOME_MACOS_SIGNING_IDENTITY")"
  printf 'desktop.mac.signing.keychain=%s\n' \
    "$(properties_escape "$KEYCHAIN_PATH")"
  printf 'desktop.mac.notarization.apple_id=%s\n' \
    "$(properties_escape "$NOME_MACOS_NOTARIZATION_APPLE_ID")"
  printf 'desktop.mac.notarization.password=%s\n' \
    "$(properties_escape "$NOME_MACOS_NOTARIZATION_PASSWORD")"
  printf 'desktop.mac.notarization.team_id=%s\n' \
    "$(properties_escape "$NOME_MACOS_TEAM_ID")"
} >>"$LOCAL_PROPERTIES"
chmod 600 "$LOCAL_PROPERTIES"

printf '%s' "$NOME_MACOS_SIGNING_KEYCHAIN_BASE64" |
  /usr/bin/base64 --decode >"$KEYCHAIN_PATH"
chmod 600 "$KEYCHAIN_PATH"
keychain_loaded=1

security unlock-keychain -p "$NOME_MACOS_SIGNING_KEYCHAIN_PASSWORD" "$KEYCHAIN_PATH"
security set-keychain-settings -lut 21600 "$KEYCHAIN_PATH"
security list-keychains -d user -s "${original_keychains[@]}" "$KEYCHAIN_PATH"
security set-key-partition-list \
  -S apple-tool:,apple: \
  -s \
  -k "$NOME_MACOS_SIGNING_KEYCHAIN_PASSWORD" \
  "$KEYCHAIN_PATH" >/dev/null

available_identities="$(
  security find-identity -v -p codesigning "$KEYCHAIN_PATH"
)"
if ! grep -Fq -- "$NOME_MACOS_SIGNING_IDENTITY" <<<"$available_identities"; then
  die "configured Nome signing identity is not present in the supplied keychain"
fi

actual_manifest_sha256="$(
  shasum -a 256 "$NATIVE_MANIFEST" | awk '{print tolower($1)}'
)"
[[ "$actual_manifest_sha256" == "$REVIEWED_MANIFEST_SHA256" ]] ||
  die "native manifest does not match NOME_MACOS_NATIVE_MANIFEST_SHA256"

cd "$REPO_ROOT"
scripts/desktop/build-lib-mac.sh arm64

gradle_args=(
  --no-daemon
  "-Pnome.nativeManifestSha256=$REVIEWED_MANIFEST_SHA256"
)
if [[ -n "${ASSETS_DIR:-}" ]]; then
  gradle_args+=("-Psimplex.assets.dir=$ASSETS_DIR")
fi

cd "$REPO_ROOT/apps/multiplatform"
./gradlew "${gradle_args[@]}" packageDmg

readonly APP_PATH="$REPO_ROOT/apps/multiplatform/release/main/app/Nome.app"
[[ -d "$APP_PATH" && ! -L "$APP_PATH" ]] ||
  die "expected Nome.app was not produced at $APP_PATH"

shopt -s nullglob
dmg_candidates=("$REPO_ROOT"/apps/multiplatform/release/main/dmg/Nome-*.dmg)
shopt -u nullglob
((${#dmg_candidates[@]} == 1)) ||
  die "expected exactly one Nome DMG, found ${#dmg_candidates[@]}"
readonly DMG_PATH="${dmg_candidates[0]}"

bash "$REPO_ROOT/scripts/ci/verify-desktop-mac-package.sh" \
  --app "$APP_PATH" \
  --source-resources "$NATIVE_RESOURCES" \
  --generated-libapp "$GENERATED_LIBAPP" \
  --manifest "$NATIVE_MANIFEST" \
  --manifest-sha256 "$REVIEWED_MANIFEST_SHA256" \
  --team-id "$NOME_MACOS_TEAM_ID"

./gradlew "${gradle_args[@]}" notarizeDmg

bash "$REPO_ROOT/scripts/ci/verify-desktop-mac-package.sh" \
  --dmg "$DMG_PATH" \
  --source-resources "$NATIVE_RESOURCES" \
  --generated-libapp "$GENERATED_LIBAPP" \
  --manifest "$NATIVE_MANIFEST" \
  --manifest-sha256 "$REVIEWED_MANIFEST_SHA256" \
  --team-id "$NOME_MACOS_TEAM_ID"

printf 'Verified Nome macOS release package: %s\n' "$DMG_PATH"
