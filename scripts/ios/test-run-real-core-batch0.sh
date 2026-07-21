#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-run-real-core-batch0-test.XXXXXX")"
restricted_path="/usr/bin:/bin:/usr/sbin:/sbin"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

make_pair() {
  local dir="$1"

  mkdir -p "$dir"
  printf 'fake-aarch64-artifact\n' > "$dir/pkg-ios-aarch64-swift-json.zip"
  printf 'fake-x86_64-artifact\n' > "$dir/pkg-ios-x86_64-swift-json.zip"
}

complete_source="$work_dir/complete-source"
staged_output="$work_dir/staged-output"
make_pair "$complete_source"

"$root_dir/scripts/ios/run-real-core-batch0.sh" \
  --source "$complete_source" \
  --output "$staged_output" \
  --force > "$work_dir/complete.log" 2>&1

[ -f "$staged_output/pkg-ios-aarch64-swift-json.zip" ] || fail "missing staged aarch64 zip"
[ -f "$staged_output/pkg-ios-x86_64-swift-json.zip" ] || fail "missing staged x86_64 zip"
grep -Fq "[PASS] Batch 0 artifact staging is ready" "$work_dir/complete.log" || fail "complete staging did not report pass"

"$root_dir/scripts/ios/run-real-core-batch0.sh" \
  --source "$staged_output" \
  --output "$staged_output" > "$work_dir/same-source-output.log" 2>&1

grep -Fq "Source and output are the same staged artifact directory" "$work_dir/same-source-output.log" || fail "same source/output case did not skip restage"

missing_source="$work_dir/missing-source"
mkdir -p "$missing_source"
if "$root_dir/scripts/ios/run-real-core-batch0.sh" \
  --source "$missing_source" \
  --output "$work_dir/missing-output" > "$work_dir/missing.log" 2>&1; then
  fail "missing artifacts unexpectedly passed"
fi

grep -Fq "No complete artifact pair found" "$work_dir/missing.log" || fail "missing artifact failure did not explain the blocker"

fake_bin="$work_dir/fake-bin"
mkdir -p "$fake_bin"
cat > "$fake_bin/curl" <<'SH'
#!/bin/sh

output=""
for arg do
  if [ "$prev" = "-o" ]; then
    output="$arg"
  fi
  prev="$arg"
done

for url do
  :
done

case "$url" in
  https://hydra.complete/*)
    if [ -n "$output" ]; then
      printf 'fake-hydra-artifact\n' > "$output"
    fi
    exit 0
    ;;
  *)
    exit 22
    ;;
esac
SH
chmod +x "$fake_bin/curl"

hydra_output="$work_dir/hydra-output"
PATH="$fake_bin:$restricted_path" REQUIRED_SIM_ARCH=x86_64 "$root_dir/scripts/ios/run-real-core-batch0.sh" \
  --job-repo "https://hydra.complete/job/stable" \
  --output "$hydra_output" \
  --force > "$work_dir/hydra.log" 2>&1

[ -f "$hydra_output/pkg-ios-aarch64-swift-json.zip" ] || fail "missing Hydra aarch64 zip"
[ -f "$hydra_output/pkg-ios-x86_64-swift-json.zip" ] || fail "missing Hydra x86_64 zip"
grep -Fq "Downloading complete artifact pair from Hydra job repository" "$work_dir/hydra.log" || fail "Hydra path did not run"

echo "[PASS] run-real-core-batch0 helper tests passed"
