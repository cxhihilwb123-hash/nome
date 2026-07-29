#!/bin/bash

set -u

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
repo="${GITHUB_REPO:-simplex-chat/simplex-chat}"
downloads_dir="${DOWNLOADS_DIR:-$HOME/Downloads}"
release_limit="${RELEASE_LIMIT:-12}"
actions_artifact_pages="${ACTIONS_ARTIFACT_PAGES:-3}"
artifact_regex='pkg-ios-(aarch64|x86_64)-swift-json'
required_sim_arch="${REQUIRED_SIM_ARCH:-$(uname -m)}"
source_target="${SOURCE_TARGET:-simulator}"
standard_sim_artifact_arch="x86_64"
developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
project_path="${PROJECT_PATH:-$root_dir/apps/ios/SimpleX.xcodeproj}"
scheme="${SCHEME:-SimpleX (iOS)}"
destinations_file="${DESTINATIONS_FILE:-}"
check_xcode_destinations="${CHECK_XCODE_DESTINATIONS:-1}"
include_known_hydra_repos="${INCLUDE_KNOWN_HYDRA_REPOS:-1}"
known_hydra_job_repos="${KNOWN_HYDRA_JOB_REPOS:-https://ci.zw3rk.com/job/simplex-chat-simplex-chat/master
https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v7-0-0-beta-3
https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v6-5-5}"
job_repo=""
hydra_job_repos="${HYDRA_JOB_REPOS:-}"
status=1

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-real-core-sources.sh [--job-repo URL]

Read-only audit for possible real iOS core artifact sources. It checks:
  - local DOWNLOADS_DIR / ~/Downloads directories and zips
  - recent GitHub release assets for simplex-chat/simplex-chat
  - GitHub Actions artifacts for simplex-chat/simplex-chat
  - optional Hydra job repository URL

Environment:
  DOWNLOADS_DIR   Override local artifact directory. Default: ~/Downloads
  GITHUB_REPO     Override GitHub repo. Default: simplex-chat/simplex-chat
  RELEASE_LIMIT   Number of recent GitHub releases to inspect. Default: 12
  ACTIONS_ARTIFACT_PAGES
                  Number of GitHub Actions artifact pages to inspect.
                  Default: 3 pages, 100 artifacts per page.
  INCLUDE_KNOWN_HYDRA_REPOS
                  Include known ci.zw3rk.com SimpleX iOS job repositories.
                  Default: 1. Set to 0 for offline fixture tests.
  KNOWN_HYDRA_JOB_REPOS
                  Override the built-in known ci.zw3rk.com SimpleX job
                  repositories. Default includes master, v7-0-0-beta-3,
                  and v6-5-5, which expose both iOS architectures as of
                  2026-07-09.
  HYDRA_JOB_REPOS Additional newline-separated Hydra job repository URLs.
                  --job-repo appends one URL to this list.
  REQUIRED_SIM_ARCH
                  Required simulator architecture. Default: uname -m.
                  The standard SimpleX pair contains x86_64 simulator
                  artifacts, so it is not enough for an arm64 simulator.
  SOURCE_TARGET   Which route should make this audit pass:
                  simulator, physical-device, or any. Default: simulator.
  CHECK_XCODE_DESTINATIONS
                  Check Xcode iOS Simulator destination architectures.
                  Default: 1.
  PROJECT_PATH    Xcode project used for destination inspection.
                  Default: apps/ios/SimpleX.xcodeproj.
  SCHEME          Xcode scheme used for destination inspection.
                  Default: SimpleX (iOS).
  DESTINATIONS_FILE
                  Fixture file with xcodebuild -showdestinations output.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --job-repo)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --job-repo requires a URL" >&2
        exit 2
      fi
      job_repo="$1"
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[FAIL] Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

have_cmd() {
  command -v "$1" >/dev/null 2>&1
}

pass() {
  echo "[PASS] $1"
  status=0
}

warn() {
  echo "[WARN] $1"
}

info() {
  echo "[INFO] $1"
}

report_standard_pair() {
  local source="$1"

  report_aarch64_device_artifact "$source"

  if [ "$source_target" = "physical-device" ]; then
    return
  fi

  if [ "$required_sim_arch" = "$standard_sim_artifact_arch" ]; then
    pass "$source: standard iOS core artifact pair is compatible with simulator architecture $required_sim_arch"
  else
    warn "$source: device arm64 + x86_64 simulator artifacts found, but the current simulator requires $required_sim_arch"
  fi
}

report_aarch64_device_artifact() {
  local source="$1"

  case "$source_target" in
    physical-device|any)
      pass "$source: arm64 device artifact is available for physical-device testing"
      ;;
    simulator)
      info "$source: arm64 device artifact is available for physical-device testing, but this audit target is simulator"
      ;;
  esac
}

load_destinations() {
  if [ -n "$destinations_file" ]; then
    if [ -f "$destinations_file" ]; then
      cat "$destinations_file"
      return 0
    fi

    warn "DESTINATIONS_FILE does not exist: $destinations_file"
    return 1
  fi

  if [ ! -d "$project_path" ]; then
    warn "Xcode project not found for destination inspection: $project_path"
    return 1
  fi

  DEVELOPER_DIR="$developer_dir" /usr/bin/xcodebuild \
    -showdestinations \
    -project "$project_path" \
    -scheme "$scheme" 2>/dev/null || return 1
}

check_xcode_simulator_destinations() {
  local destinations
  local sim_arches

  if [ "$check_xcode_destinations" != "1" ]; then
    info "Skipping Xcode simulator destination architecture audit"
    return
  fi

  info "Checking Xcode iOS Simulator destination architectures for $scheme"
  if ! destinations="$(load_destinations)" || [ -z "$destinations" ]; then
    warn "Could not read Xcode destinations; source audit will use required simulator architecture $required_sim_arch"
    return
  fi

  sim_arches="$(printf '%s\n' "$destinations" | sed -nE 's/.*platform:iOS Simulator, arch:([^,}]+).*/\1/p' | sort -u | tr '\n' ' ' | sed 's/[[:space:]]*$//')"
  if [ -z "$sim_arches" ]; then
    warn "No concrete iOS Simulator destination architectures were found"
    return
  fi

  info "Xcode iOS Simulator destination architecture(s): $sim_arches"
  if printf ' %s ' "$sim_arches" | grep -Fq " $required_sim_arch "; then
    info "Required simulator architecture is available as an Xcode destination: $required_sim_arch"
  else
    warn "Required simulator architecture is not available as an Xcode destination: $required_sim_arch"
  fi

  if ! printf ' %s ' "$sim_arches" | grep -Fq " $standard_sim_artifact_arch "; then
    warn "Standard x86_64 simulator artifacts cannot run on this machine until an x86_64 iOS Simulator destination is available"
  fi
}

check_local_artifacts() {
  local arch
  local has_aarch64=0
  local has_x86_64=0

  info "Checking local artifacts in $downloads_dir"
  for arch in aarch64 x86_64; do
    if [ -d "$downloads_dir/pkg-ios-$arch-swift-json" ]; then
      info "Found local extracted artifact: $downloads_dir/pkg-ios-$arch-swift-json"
      if [ "$arch" = "aarch64" ]; then
        has_aarch64=1
      else
        has_x86_64=1
      fi
    elif [ -f "$downloads_dir/pkg-ios-$arch-swift-json.zip" ]; then
      info "Found local artifact zip: $downloads_dir/pkg-ios-$arch-swift-json.zip"
      if [ "$arch" = "aarch64" ]; then
        has_aarch64=1
      else
        has_x86_64=1
      fi
    else
      warn "Missing local pkg-ios-$arch-swift-json directory or zip"
    fi
  done

  if [ "$has_aarch64" -eq 1 ] && [ "$has_x86_64" -eq 1 ]; then
    report_standard_pair "Local artifacts"
  elif [ "$has_aarch64" -eq 1 ]; then
    report_aarch64_device_artifact "Local artifacts"
    warn "Only one local iOS core architecture is available; a device artifact and a simulator-compatible artifact are required"
  elif [ "$has_x86_64" -eq 1 ]; then
    warn "Only one local iOS core architecture is available; a device artifact and a simulator-compatible artifact are required"
  else
    warn "No local iOS core artifacts found"
  fi
}

check_github_release_assets() {
  local releases_tsv
  local release_json
  local line
  local names
  local tag
  local title
  local kind
  local published
  local url
  local checked=0
  local matched=0

  if ! have_cmd gh; then
    warn "gh is not installed; skipping GitHub release asset audit"
    return
  fi

  if ! have_cmd jq; then
    warn "jq is not installed; skipping GitHub release asset audit"
    return
  fi

  info "Checking recent GitHub release assets for $repo (limit: $release_limit)"
  if ! releases_tsv="$(gh release list --repo "$repo" --limit "$release_limit" 2>/dev/null)" || [ -z "$releases_tsv" ]; then
    warn "Could not query GitHub release list for $repo"
    return
  fi

  while IFS= read -r line; do
    title="$(printf '%s\n' "$line" | cut -f1)"
    kind="$(printf '%s\n' "$line" | cut -f2)"
    tag="$title"
    published="$(printf '%s\n' "$line" | cut -f4)"
    if [ -z "${tag:-}" ]; then
      continue
    fi

    if ! release_json="$(gh release view "$tag" --repo "$repo" --json tagName,publishedAt,url,assets 2>/dev/null)"; then
      warn "Could not query GitHub release assets for $tag"
      continue
    fi

    checked=1
    tag="$(printf '%s' "$release_json" | jq -r '.tagName // "unknown"')"
    published="$(printf '%s' "$release_json" | jq -r '.publishedAt // ""')"
    url="$(printf '%s' "$release_json" | jq -r '.url // ""')"
    info "Release: $tag ${kind:+[$kind]} ${published:+published $published} ${url:+($url)}"

    names="$(printf '%s' "$release_json" | jq -r '.assets[]?.name // empty')"
    has_aarch64=0
    has_x86_64=0
    if printf '%s\n' "$names" | grep -Eiq 'pkg-ios-aarch64-swift-json'; then
      has_aarch64=1
    fi
    if printf '%s\n' "$names" | grep -Eiq 'pkg-ios-x86_64-swift-json'; then
      has_x86_64=1
    fi

    if [ "$has_aarch64" -eq 1 ] && [ "$has_x86_64" -eq 1 ]; then
      report_standard_pair "GitHub release $tag"
      matched=1
      printf '%s\n' "$names" | grep -Ei "$artifact_regex" | sed "s/^/[INFO] release-asset $tag /"
    elif [ "$has_aarch64" -eq 1 ]; then
      report_aarch64_device_artifact "GitHub release $tag"
      matched=1
      warn "GitHub release $tag exposes a physical-device artifact but no x86_64 simulator artifact"
      printf '%s\n' "$names" | grep -Ei "$artifact_regex" | sed "s/^/[INFO] release-asset $tag /"
    elif [ "$has_x86_64" -eq 1 ]; then
      warn "GitHub release $tag exposes only a partial iOS core asset set"
      printf '%s\n' "$names" | grep -Ei "$artifact_regex" | sed "s/^/[INFO] release-asset $tag /"
    else
      warn "GitHub release $tag does not expose pkg-ios-*-swift-json assets"
      printf '%s\n' "$names" | sed -n "1,20{s/^/[INFO] release-asset $tag /;p;}"
    fi
  done <<EOF
$releases_tsv
EOF

  if [ "$checked" -eq 0 ]; then
    warn "No GitHub releases could be inspected"
  elif [ "$matched" -eq 0 ]; then
    warn "No inspected GitHub release exposes a simulator-compatible pkg-ios-*-swift-json asset pair"
  fi
}

check_github_actions_artifacts() {
  local page
  local page_tsv
  local artifacts_tsv
  local checked_pages=0

  if ! have_cmd gh; then
    warn "gh is not installed; skipping GitHub Actions artifact audit"
    return
  fi

  if ! printf '%s' "$actions_artifact_pages" | grep -Eq '^[1-9][0-9]*$'; then
    warn "ACTIONS_ARTIFACT_PAGES must be a positive integer; got '$actions_artifact_pages'"
    return
  fi

  info "Checking recent GitHub Actions artifacts for $repo (pages: $actions_artifact_pages, 100 per page)"
  artifacts_tsv=""
  page=1
  while [ "$page" -le "$actions_artifact_pages" ]; do
    if ! page_tsv="$(gh api "repos/$repo/actions/artifacts?per_page=100&page=$page" --jq '.artifacts[] | [.name, .expired, .size_in_bytes, .created_at] | @tsv' 2>/dev/null)"; then
      warn "Could not query GitHub Actions artifacts for $repo page $page"
      page=$((page + 1))
      continue
    fi

    if [ -z "$page_tsv" ]; then
      info "No GitHub Actions artifacts returned on page $page"
      break
    fi

    checked_pages=$((checked_pages + 1))
    artifacts_tsv="${artifacts_tsv}${artifacts_tsv:+
}${page_tsv}"
    page=$((page + 1))
  done

  if [ "$checked_pages" -eq 0 ]; then
    warn "Could not query any GitHub Actions artifact pages for $repo"
    return
  fi

  info "Inspected $checked_pages GitHub Actions artifact page(s)"
  local has_aarch64=0
  local has_x86_64=0
  if printf '%s\n' "$artifacts_tsv" | grep -Eiq 'pkg-ios-aarch64-swift-json'; then
    has_aarch64=1
  fi
  if printf '%s\n' "$artifacts_tsv" | grep -Eiq 'pkg-ios-x86_64-swift-json'; then
    has_x86_64=1
  fi

  if [ "$has_aarch64" -eq 1 ] && [ "$has_x86_64" -eq 1 ]; then
    report_standard_pair "GitHub Actions"
    printf '%s\n' "$artifacts_tsv" | grep -Ei "$artifact_regex" | sed 's/^/[INFO] actions-artifact /'
  elif [ "$has_aarch64" -eq 1 ]; then
    report_aarch64_device_artifact "GitHub Actions"
    warn "GitHub Actions exposes a physical-device artifact but no x86_64 simulator artifact"
    printf '%s\n' "$artifacts_tsv" | grep -Ei "$artifact_regex" | sed 's/^/[INFO] actions-artifact /'
  elif [ "$has_x86_64" -eq 1 ]; then
    warn "GitHub Actions exposes only a partial iOS core artifact set"
    printf '%s\n' "$artifacts_tsv" | grep -Ei "$artifact_regex" | sed 's/^/[INFO] actions-artifact /'
  else
    warn "Recent GitHub Actions artifacts do not expose pkg-ios-*-swift-json"
    printf '%s\n' "$artifacts_tsv" | sed -n '1,20{s/^/[INFO] actions-artifact /;p;}'
  fi
}

check_hydra_job_repo() {
  local repos
  local candidate
  local arch
  local url
  local has_aarch64=0
  local has_x86_64=0
  local checked_count=0
  local complete_count=0

  repos=""
  if [ "$include_known_hydra_repos" = "1" ] && [ -n "$known_hydra_job_repos" ]; then
    repos="$known_hydra_job_repos"
  fi
  if [ -n "$hydra_job_repos" ]; then
    repos="${repos}${repos:+
}${hydra_job_repos}"
  fi
  if [ -n "$job_repo" ]; then
    repos="${repos}${repos:+
}${job_repo}"
  fi

  if [ -z "$repos" ]; then
    warn "No Hydra job repository URL supplied; skipping Hydra artifact audit"
    return
  fi

  while IFS= read -r candidate; do
    candidate="$(printf '%s' "$candidate" | sed 's/[[:space:]]*$//')"
    if [ -z "$candidate" ]; then
      continue
    fi

    checked_count=$((checked_count + 1))
    has_aarch64=0
    has_x86_64=0
    info "Checking Hydra job repository: $candidate"
    for arch in aarch64 x86_64; do
      url="$candidate/$arch-darwin.$arch-darwin-ios:lib:simplex-chat/latest/download/1"
      if curl --tlsv1.2 --fail --silent --show-error --location --head "$url" >/dev/null; then
        info "Hydra artifact reachable for $arch: $url"
        if [ "$arch" = "aarch64" ]; then
          has_aarch64=1
        else
          has_x86_64=1
        fi
      else
        warn "Hydra artifact not reachable for $arch: $url"
      fi
    done

    if [ "$has_aarch64" -eq 1 ] && [ "$has_x86_64" -eq 1 ]; then
      report_standard_pair "Hydra job repository $candidate"
      complete_count=$((complete_count + 1))
    elif [ "$has_aarch64" -eq 1 ]; then
      report_aarch64_device_artifact "Hydra job repository $candidate"
      warn "Hydra job repository exposes a physical-device artifact but no x86_64 simulator artifact: $candidate"
    elif [ "$has_x86_64" -eq 1 ]; then
      warn "Hydra job repository exposes only one required iOS core architecture: $candidate"
    else
      warn "Hydra job repository did not expose required iOS core artifacts: $candidate"
    fi
  done <<EOF
$repos
EOF

  if [ "$checked_count" -eq 0 ]; then
    warn "No non-empty Hydra job repository URL supplied; skipping Hydra artifact audit"
  elif [ "$complete_count" -eq 0 ] || [ "$required_sim_arch" != "$standard_sim_artifact_arch" ]; then
    warn "No supplied Hydra job repository exposes a simulator-compatible iOS core artifact pair for $required_sim_arch"
  fi
}

cat <<'HEADER'
Nome iOS real-core source audit
===============================
HEADER

info "Required simulator architecture: $required_sim_arch"
info "Standard SimpleX simulator artifact architecture: $standard_sim_artifact_arch"
case "$source_target" in
  simulator|physical-device|any)
    info "Source target: $source_target"
    ;;
  *)
    echo "[FAIL] SOURCE_TARGET must be simulator, physical-device, or any; got '$source_target'" >&2
    exit 2
    ;;
esac

check_xcode_simulator_destinations
check_local_artifacts
check_github_release_assets
check_github_actions_artifacts
check_hydra_job_repo

cat <<'NEXT'

If this script reports a simulator-compatible source, prepare it with:
  scripts/ios/run-real-core-batch0.sh --source /path/to/artifacts --output /tmp/nome-ios-real-core-batch0 --force
  scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-real-core-batch0 --prepare

For a Hydra job repository, download and stage first:
  scripts/ios/run-real-core-batch0.sh --job-repo <hydra-job-repo> --output /tmp/nome-ios-real-core-batch0 --force

If SOURCE_TARGET=physical-device reports an arm64 device candidate, keep the
simulator libraries untouched and audit/install only the iphoneos path with:
  scripts/ios/prepare-device-real-core.sh
  scripts/ios/prepare-device-real-core.sh --prepare

This script is read-only. It does not download, unzip, or replace libraries.
NEXT

exit "$status"
