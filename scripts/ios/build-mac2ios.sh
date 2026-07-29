#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
source_repo="${MAC2IOS_REPO:-https://github.com/zw3rk/mobile-core-tools.git}"
work_dir="${TMPDIR:-/tmp}/mobile-core-tools"
output_dir="$root_dir/tools/bin"

rm -rf "$work_dir"
git clone --depth 1 "$source_repo" "$work_dir"
make -C "$work_dir" mac2ios

mkdir -p "$output_dir"
cp "$work_dir/mac2ios" "$output_dir/mac2ios"
chmod +x "$output_dir/mac2ios"

"$output_dir/mac2ios" -h >/dev/null
echo "Built $output_dir/mac2ios"
