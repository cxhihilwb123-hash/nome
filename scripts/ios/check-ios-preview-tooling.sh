#!/usr/bin/env bash

set -euo pipefail

developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
xcode_select_path="${XCODE_SELECT_PATH:-}"
xcodebuild_version_cmd="${XCODEBUILD_VERSION_CMD:-$developer_dir/usr/bin/xcodebuild -version}"
default_simctl_cmd="${DEFAULT_SIMCTL_CMD:-xcrun simctl list devices booted}"
full_simctl_cmd="${FULL_SIMCTL_CMD:-DEVELOPER_DIR=$developer_dir xcrun simctl list devices booted}"
status=0

ok() {
  printf '[OK] %s\n' "$1"
}

warn() {
  printf '[WARN] %s\n' "$1"
}

fail() {
  printf '[FAIL] %s\n' "$1"
  status=1
}

info() {
  printf '[INFO] %s\n' "$1"
}

run_probe() {
  local label="$1"
  local command="$2"
  local required="$3"
  local output

  if output="$(bash -c "$command" 2>&1)"; then
    ok "$label"
    if [ -n "$output" ]; then
      printf '%s\n' "$output" | sed 's/^/[INFO] /'
    fi
  elif [ "$required" = "required" ]; then
    fail "$label"
    if [ -n "$output" ]; then
      printf '%s\n' "$output" | sed 's/^/[INFO] /'
    fi
  else
    warn "$label"
    if [ -n "$output" ]; then
      printf '%s\n' "$output" | sed 's/^/[INFO] /'
    fi
  fi
}

cat <<'HEADER'
Nome iOS preview tooling
========================
HEADER

if [ -z "$xcode_select_path" ]; then
  xcode_select_path="$(xcode-select -p 2>/dev/null || true)"
fi

info "xcode-select path: ${xcode_select_path:-unknown}"
info "full Xcode developer dir: $developer_dir"

case "$xcode_select_path" in
  "$developer_dir")
    ok "Default xcode-select points at full Xcode"
    ;;
  /Library/Developer/CommandLineTools)
    warn "Default xcode-select points at Command Line Tools; XcodeBuildMCP simulator tools may fail unless the system developer directory is switched to full Xcode"
    ;;
  "")
    warn "Could not read xcode-select path"
    ;;
  *)
    warn "Default xcode-select points at a different developer directory: $xcode_select_path"
    ;;
esac

run_probe "Full Xcode xcodebuild is available" "$xcodebuild_version_cmd" "required"
run_probe "Default xcrun simctl can list booted devices" "$default_simctl_cmd" "optional"
run_probe "Full-Xcode DEVELOPER_DIR xcrun simctl can list booted devices" "$full_simctl_cmd" "required"

cat <<'NEXT'

Interpretation:
- If default xcrun simctl warns but the full-Xcode DEVELOPER_DIR probe passes,
  Codex/XcodeBuildMCP may be blocked by the system xcode-select path while the
  repo's shell smoke scripts can still build, install, launch, and screenshot
  with DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer.
- Switching the system developer directory requires administrator privileges:
  sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
NEXT

exit "$status"
