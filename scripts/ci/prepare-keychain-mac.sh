#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

die() {
  printf 'Nome keychain preparation error: %s\n' "$*" >&2
  exit 1
}

require_env() {
  local name="$1"
  local value="${!name:-}"
  [[ -n "$value" ]] || die "required environment variable $name is not set"
  [[ "$value" != *$'\n'* && "$value" != *$'\r'* ]] ||
    die "$name must not contain newline characters"
}

for required_name in \
  NOME_MACOS_SIGNING_IDENTITY \
  NOME_MACOS_TEAM_ID \
  NOME_MACOS_SIGNING_CERTIFICATE_P12 \
  NOME_MACOS_SIGNING_CERTIFICATE_PASSWORD \
  NOME_MACOS_SIGNING_KEYCHAIN_PASSWORD \
  NOME_MACOS_SIGNING_KEYCHAIN_OUTPUT
do
  require_env "$required_name"
done

[[ "$NOME_MACOS_TEAM_ID" =~ ^[A-Z0-9]{10}$ ]] ||
  die "NOME_MACOS_TEAM_ID must be a 10-character Apple Team ID"
[[ "$NOME_MACOS_SIGNING_IDENTITY" == "Developer ID Application: "* &&
  "$NOME_MACOS_SIGNING_IDENTITY" == *"($NOME_MACOS_TEAM_ID)" ]] ||
  die "signing identity must be a Developer ID Application identity for NOME_MACOS_TEAM_ID"
[[ "$NOME_MACOS_SIGNING_KEYCHAIN_OUTPUT" == /* ]] ||
  die "NOME_MACOS_SIGNING_KEYCHAIN_OUTPUT must be an absolute path"
[[ -f "$NOME_MACOS_SIGNING_CERTIFICATE_P12" &&
  ! -L "$NOME_MACOS_SIGNING_CERTIFICATE_P12" ]] ||
  die "signing certificate is missing or unsafe"
[[ ! -e "$NOME_MACOS_SIGNING_KEYCHAIN_OUTPUT" ]] ||
  die "refusing to overwrite existing keychain output"

readonly OUTPUT_DIR="$(dirname "$NOME_MACOS_SIGNING_KEYCHAIN_OUTPUT")"
[[ -d "$OUTPUT_DIR" && ! -L "$OUTPUT_DIR" ]] ||
  die "keychain output directory is missing or unsafe"

readonly WORK_DIR="$(mktemp -d "$OUTPUT_DIR/.nome-signing.XXXXXX")"
readonly TEMP_KEYCHAIN="$WORK_DIR/nome-signing.keychain-db"
keychain_created=0
completed=0
keychain_list_captured=0
original_keychains=()

cleanup() {
  local status=$?
  trap - EXIT HUP INT TERM
  set +e

  if ((keychain_list_captured)); then
    security list-keychains -d user -s "${original_keychains[@]}" >/dev/null 2>&1
  fi
  if ((keychain_created)); then
    security lock-keychain "$TEMP_KEYCHAIN" >/dev/null 2>&1
  fi
  if ((!completed)); then
    security delete-keychain "$TEMP_KEYCHAIN" >/dev/null 2>&1
    rm -f "$TEMP_KEYCHAIN"
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

security create-keychain \
  -p "$NOME_MACOS_SIGNING_KEYCHAIN_PASSWORD" \
  "$TEMP_KEYCHAIN"
keychain_created=1
chmod 600 "$TEMP_KEYCHAIN"
security set-keychain-settings -lut 21600 "$TEMP_KEYCHAIN"
security unlock-keychain \
  -p "$NOME_MACOS_SIGNING_KEYCHAIN_PASSWORD" \
  "$TEMP_KEYCHAIN"
security import "$NOME_MACOS_SIGNING_CERTIFICATE_P12" \
  -P "$NOME_MACOS_SIGNING_CERTIFICATE_PASSWORD" \
  -k "$TEMP_KEYCHAIN" \
  -T /usr/bin/codesign \
  -T /usr/bin/security >/dev/null
security set-key-partition-list \
  -S apple-tool:,apple: \
  -s \
  -k "$NOME_MACOS_SIGNING_KEYCHAIN_PASSWORD" \
  "$TEMP_KEYCHAIN" >/dev/null

available_identities="$(
  security find-identity -v -p codesigning "$TEMP_KEYCHAIN"
)"
if ! grep -Fq -- "$NOME_MACOS_SIGNING_IDENTITY" <<<"$available_identities"; then
  die "requested Nome signing identity was not imported"
fi

security lock-keychain "$TEMP_KEYCHAIN"
mv "$TEMP_KEYCHAIN" "$NOME_MACOS_SIGNING_KEYCHAIN_OUTPUT"
chmod 600 "$NOME_MACOS_SIGNING_KEYCHAIN_OUTPUT"
completed=1

printf 'Prepared Nome signing keychain: %s\n' \
  "$NOME_MACOS_SIGNING_KEYCHAIN_OUTPUT"
