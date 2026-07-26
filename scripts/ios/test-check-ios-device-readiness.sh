#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/check-ios-device-readiness.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-device-readiness-parser-test.XXXXXX")"
fixture="$work_dir/xctrace.txt"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

assert_output() {
  local expected="$1"
  local actual="$2"
  local label="$3"

  if [ "$actual" != "$expected" ]; then
    printf '[FAIL] %s\nexpected: <%s>\nactual:   <%s>\n' "$label" "$expected" "$actual" >&2
    exit 1
  fi
}

cat > "$fixture" <<'EOF'
== Devices ==
Developer's Mac mini (5) (D66468AA-3320-5C5F-A2FF-8482FB40449A)
Alice's iPhone Pro Max (26.5) (00008110-AAAAAAAAAAAAAAAA)

== Devices Offline ==
CryHandsome (26.5.2) (00008120-BBBBBBBBBBBBBBBB)

== Simulators ==
iPhone 17 Simulator (26.5) (CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC)
EOF
mixed_output="$("$script" --parse-xctrace-devices < "$fixture")"
assert_output "Alice's iPhone Pro Max (26.5) (00008110-AAAAAAAAAAAAAAAA)" "$mixed_output" "mixed output must include only the connected physical device"

cat > "$fixture" <<'EOF'
== Devices ==
Developer's MacBook Pro (5) (D66468AA-3320-5C5F-A2FF-8482FB40449A)

== Devices Offline ==
CryHandsome (26.5.2) (00008120-BBBBBBBBBBBBBBBB)

== Simulators ==
iPhone 17 Simulator (26.5) (CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC)
EOF
offline_output="$("$script" --parse-xctrace-devices < "$fixture")"
assert_output "" "$offline_output" "offline devices must not be reported as connected"

cat > "$fixture" <<'EOF'
== Devices ==
Mac Pro QA iPad (26.5) (00008130-DDDDDDDDDDDDDDDD)

== Simulators ==
iPad Pro Simulator (26.5) (EEEEEEEE-EEEE-EEEE-EEEE-EEEEEEEEEEEE)
EOF
connected_output="$("$script" --parse-xctrace-devices < "$fixture")"
assert_output "Mac Pro QA iPad (26.5) (00008130-DDDDDDDDDDDDDDDD)" "$connected_output" "connected device names must be preserved without model-name filtering"

cat > "$fixture" <<'EOF'
== Devices ==
Nome QA iPhone (26.5) (00008140-FFFFFFFFFFFFFFFF)

== Paired Watches ==
Nome Watch (26.5) (00008150-1111111111111111)

== Simulators ==
iPhone 17 Simulator (26.5) (22222222-2222-2222-2222-222222222222)
EOF
future_section_output="$("$script" --parse-xctrace-devices < "$fixture")"
assert_output "Nome QA iPhone (26.5) (00008140-FFFFFFFFFFFFFFFF)" "$future_section_output" "any subsequent xctrace section must end connected-device parsing"

[ -x "$script" ] || fail "device readiness script is not executable"

printf '[PASS] iOS device readiness xctrace parser fixtures\n'
