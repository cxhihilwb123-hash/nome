#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
restricted_path="/usr/bin:/bin:/usr/sbin:/sbin"

if [ -n "${OUTPUT_DIR:-}" ]; then
  output_dir="$OUTPUT_DIR"
else
  output_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-prepare-real-core-safety-test.XXXXXX")"
fi

mkdir -p "$output_dir"

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

snapshot_libraries() {
  local sandbox="$1"

  (
    cd "$sandbox"
    find apps/ios/Libraries -type f -print | sort | while IFS= read -r file; do
      shasum -a 256 "$file"
    done
  ) | shasum -a 256 | awk '{print $1}'
}

make_sandbox() {
  local sandbox="$1"

  mkdir -p "$sandbox/scripts/ios"
  mkdir -p "$sandbox/apps/ios/Libraries/sim"
  mkdir -p "$sandbox/home"
  cp "$root_dir/scripts/ios/prepare-real-core.sh" "$sandbox/scripts/ios/"
  cp "$root_dir/scripts/ios/prepare-x86_64.sh" "$sandbox/scripts/ios/"
  cp "$root_dir/scripts/ios/check-real-core.sh" "$sandbox/scripts/ios/"
  cp "$root_dir/scripts/ios/check-real-core-sources.sh" "$sandbox/scripts/ios/"
  cp "$root_dir/scripts/ios/download-libs.sh" "$sandbox/scripts/ios/"
  cp "$root_dir/scripts/ios/build-mac2ios.sh" "$sandbox/scripts/ios/"
  printf 'preview-library-sentinel\n' > "$sandbox/apps/ios/Libraries/sim/libSimpleXChat.dylib"
}

make_fake_mac2ios() {
  local path="$1"

  cat > "$path" <<'SH'
#!/bin/sh
exit 0
SH
  chmod +x "$path"
}

make_fake_lipo() {
  local path="$1"

  cat > "$path" <<'SH'
#!/bin/sh

for arg do
  lib="$arg"
done

case "$lib" in
  *pkg-ios-x86_64-swift-json*)
    echo "Non-fat file: $lib is architecture: x86_64"
    ;;
  *)
    echo "Non-fat file: $lib is architecture: arm64"
    ;;
esac
SH
  chmod +x "$path"
}

run_failure_case() {
  local name="$1"
  local expected_text="$2"
  local sandbox="$output_dir/$name-sandbox"
  local artifacts="$output_dir/$name-artifacts"
  local fake_mac2ios="$output_dir/$name-mac2ios"
  local fake_bin="$output_dir/$name-bin"
  local log="$output_dir/$name.log"
  local before_hash
  local after_hash
  local rc

  make_sandbox "$sandbox"
  mkdir -p "$artifacts"
  mkdir -p "$fake_bin"
  make_fake_mac2ios "$fake_mac2ios"
  make_fake_lipo "$fake_bin/lipo"

  case "$name" in
    missing)
      ;;
    partial)
      mkdir -p "$artifacts/pkg-ios-aarch64-swift-json"
      printf 'fake-aarch64-lib\n' > "$artifacts/pkg-ios-aarch64-swift-json/libSimpleXChat.dylib"
      ;;
    empty-second-arch)
      mkdir -p "$artifacts/pkg-ios-aarch64-swift-json"
      mkdir -p "$artifacts/pkg-ios-x86_64-swift-json"
      printf 'fake-aarch64-lib\n' > "$artifacts/pkg-ios-aarch64-swift-json/libSimpleXChat.dylib"
      ;;
    sim-arch-mismatch)
      mkdir -p "$artifacts/pkg-ios-aarch64-swift-json"
      mkdir -p "$artifacts/pkg-ios-x86_64-swift-json"
      printf 'fake-aarch64-lib\n' > "$artifacts/pkg-ios-aarch64-swift-json/libHSsimplex-chat-test.a"
      printf 'fake-aarch64-dependency\n' > "$artifacts/pkg-ios-aarch64-swift-json/libffi.a"
      printf 'fake-x86_64-lib\n' > "$artifacts/pkg-ios-x86_64-swift-json/libHSsimplex-chat-test.a"
      printf 'fake-x86_64-dependency\n' > "$artifacts/pkg-ios-x86_64-swift-json/libffi.a"
      ;;
    *)
      fail "Unknown test case: $name"
      ;;
  esac

  before_hash="$(snapshot_libraries "$sandbox")"

  set +e
  HOME="$sandbox/home" \
    PATH="$fake_bin:$restricted_path" \
    MAC2IOS="$fake_mac2ios" \
    MIN_REAL_CORE_LIB_BYTES=1 \
    REQUIRED_SIM_ARCH=arm64 \
    "$sandbox/scripts/ios/prepare-real-core.sh" --downloads-dir "$artifacts" --downloads > "$log" 2>&1
  rc=$?
  set -e

  after_hash="$(snapshot_libraries "$sandbox")"

  if [ "$rc" -eq 0 ]; then
    sed -n '1,160p' "$log" >&2
    fail "$name unexpectedly succeeded"
  fi

  if [ "$before_hash" != "$after_hash" ]; then
    sed -n '1,160p' "$log" >&2
    fail "$name changed sandbox libraries before validation completed"
  fi

  if ! grep -Fq "$expected_text" "$log"; then
    sed -n '1,160p' "$log" >&2
    fail "$name missing expected text: $expected_text"
  fi

  pass "$name"
}

run_failure_case "missing" "Expected extracted artifact directories or .zip files"
run_failure_case "partial" "Missing"
run_failure_case "empty-second-arch" "Artifact for x86_64 did not contain library files after staging"
run_failure_case "sim-arch-mismatch" "Simulator library architecture mismatch"

run_job_repo_case() {
  local sandbox="$output_dir/job-repo-sandbox"
  local fake_mac2ios="$output_dir/job-repo-mac2ios"
  local log="$output_dir/job-repo.log"
  local rc

  make_sandbox "$sandbox"
  make_fake_mac2ios "$fake_mac2ios"

  cat > "$sandbox/scripts/ios/check-real-core-sources.sh" <<'SH'
#!/bin/sh
echo "[FAKE] source audit $*"
if [ "${1:-}" != "--job-repo" ] || [ -z "${2:-}" ]; then
  exit 1
fi
exit 0
SH
  chmod +x "$sandbox/scripts/ios/check-real-core-sources.sh"

  cat > "$sandbox/scripts/ios/download-libs.sh" <<'SH'
#!/bin/sh
echo "[FAKE] download libs $*"
if [ -z "${1:-}" ]; then
  exit 1
fi
exit 0
SH
  chmod +x "$sandbox/scripts/ios/download-libs.sh"

  cat > "$sandbox/scripts/ios/check-real-core.sh" <<'SH'
#!/bin/sh
echo "[FAKE] final preflight $*"
if [ "${1:-}" = "--job-repo" ]; then
  echo "[FAIL] final installed-core preflight ran before download"
  exit 17
fi
exit 0
SH
  chmod +x "$sandbox/scripts/ios/check-real-core.sh"

  set +e
  HOME="$sandbox/home" MAC2IOS="$fake_mac2ios" "$sandbox/scripts/ios/prepare-real-core.sh" --job-repo "https://hydra.example/job/stable" > "$log" 2>&1
  rc=$?
  set -e

  if [ "$rc" -ne 0 ]; then
    sed -n '1,160p' "$log" >&2
    fail "job-repo expected exit 0, got $rc"
  fi

  for expected in \
    "[FAKE] source audit --job-repo https://hydra.example/job/stable" \
    "[FAKE] download libs https://hydra.example/job/stable" \
    "[FAKE] final preflight"
  do
    if ! grep -Fq "$expected" "$log"; then
      sed -n '1,160p' "$log" >&2
      fail "job-repo missing expected text: $expected"
    fi
  done

  pass "job-repo"
}

run_job_repo_case

echo "[PASS] prepare-real-core safety test logs: $output_dir"
