#!/bin/bash

set -u

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
checklist="$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md"
write_tsv=""

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-nome-manual-qa-status.sh [--file FILE] [--write-tsv FILE]

Summarizes the Nome iOS manual QA checklist. It exits 0 only when every
checklist item is checked. While any item remains unchecked, it exits 1 so the
overall goal audit cannot be mistaken for complete.

Options:
  --file FILE       Checklist file to inspect.
  --write-tsv FILE  Write unchecked items as a machine-readable TSV queue.
  -h, --help        Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --file)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --file requires a path" >&2
        exit 2
      fi
      checklist="$1"
      ;;
    --write-tsv)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --write-tsv requires a path" >&2
        exit 2
      fi
      write_tsv="$1"
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

if [ ! -f "$checklist" ]; then
  echo "[FAIL] Manual QA checklist not found: $checklist" >&2
  exit 2
fi

checked_count="$(grep -Ec '^- \[x\] ' "$checklist" || true)"
unchecked_count="$(grep -Ec '^- \[ \] ' "$checklist" || true)"
total_count=$((checked_count + unchecked_count))

echo "Nome iOS manual QA status"
echo "========================="
echo "[INFO] Checklist: $checklist"
echo "[INFO] Total items: $total_count"
echo "[INFO] Checked items: $checked_count"
echo "[INFO] Unchecked items: $unchecked_count"

if [ "$unchecked_count" -eq 0 ]; then
  if [ -n "$write_tsv" ]; then
    mkdir -p "$(dirname "$write_tsv")"
    printf 'line\tsection\tblocker_group\titem\n' > "$write_tsv"
    echo "[INFO] Wrote unchecked-item queue: $write_tsv"
  fi
  echo "[PASS] Manual QA checklist is fully checked"
  exit 0
fi

echo
echo "[BLOCKED] Manual QA checklist still has unchecked items"
echo
if [ -n "$write_tsv" ]; then
  mkdir -p "$(dirname "$write_tsv")"
  awk -v out="$write_tsv" '
    function blocker_group(section, item) {
      text = tolower(section " " item)

      if (text ~ /final.*screenshot|app store screenshot/) {
        return "app_store_final_screenshots"
      }
      if (text ~ /release-identifiers|release identifier|testflight|bundle identifier|bundle id|app group|keychain|associated domain|background task|extension display/) {
        return "release_identifiers"
      }
      if (text ~ /real ios core|production-sized/) {
        return "real_core_artifacts"
      }
      if (text ~ /physical iphone|camera permission|live qr/) {
        return "physical_device_or_camera"
      }
      if (text ~ /two independent|second account|first account|one-to-one|group conversation/) {
        return "multi_account_real_core"
      }
      if (text ~ /smp|xftp|server|tor|private-routing/) {
        return "network_server_real_core"
      }
      if (text ~ /profile switching|hidden profile|incognito|new-profile/) {
        return "identity_real_data"
      }
      if (text ~ /migration|device-transfer/) {
        return "migration_real_data"
      }
      if (text ~ /clean first-run|network conditions|reaches the nome home/) {
        return "first_run_real_core"
      }
      if (text ~ /group invitation|joining a valid group|request-approval|pending state/) {
        return "group_real_core"
      }
      if (text ~ /sharing|native share|copy copies|share opens/) {
        return "native_share_real_core"
      }
      if (text ~ /failed|expired|already-used|bad|rejected|understandable errors/) {
        return "error_state_real_core"
      }
      if (text ~ /actual reusable|request confirmation|change, disable|settings actions/) {
        return "public_contact_real_core"
      }
      if (text ~ /file sending|voice messages|delivery receipts|disappearing-message/) {
        return "messaging_feature_real_core"
      }
      if (text ~ /safety banner/) {
        return "conversation_safety_real_core"
      }
      if (text ~ /real core|valid one-time|scannable|reusable public/) {
        return "real_core_functional"
      }

      return "manual_followup"
    }

    BEGIN {
      print "line\tsection\tblocker_group\titem" > out
    }
    /^## / {
      section = substr($0, 4)
      next
    }
    /^- \[ \] / {
      item = substr($0, 7)
      print NR "\t" section "\t" blocker_group(section, item) "\t" item >> out
    }
  ' "$checklist"
  echo "[INFO] Wrote unchecked-item queue: $write_tsv"
fi

echo "Unchecked items by blocker group:"
awk '
  function blocker_group(section, item) {
    text = tolower(section " " item)

    if (text ~ /final.*screenshot|app store screenshot/) {
      return "app_store_final_screenshots"
    }
    if (text ~ /release-identifiers|release identifier|testflight|bundle identifier|bundle id|app group|keychain|associated domain|background task|extension display/) {
      return "release_identifiers"
    }
    if (text ~ /real ios core|production-sized/) {
      return "real_core_artifacts"
    }
    if (text ~ /physical iphone|camera permission|live qr/) {
      return "physical_device_or_camera"
    }
    if (text ~ /two independent|second account|first account|one-to-one|group conversation/) {
      return "multi_account_real_core"
    }
    if (text ~ /smp|xftp|server|tor|private-routing/) {
      return "network_server_real_core"
    }
    if (text ~ /profile switching|hidden profile|incognito|new-profile/) {
      return "identity_real_data"
    }
    if (text ~ /migration|device-transfer/) {
      return "migration_real_data"
    }
    if (text ~ /clean first-run|network conditions|reaches the nome home/) {
      return "first_run_real_core"
    }
    if (text ~ /group invitation|joining a valid group|request-approval|pending state/) {
      return "group_real_core"
    }
    if (text ~ /sharing|native share|copy copies|share opens/) {
      return "native_share_real_core"
    }
    if (text ~ /failed|expired|already-used|bad|rejected|understandable errors/) {
      return "error_state_real_core"
    }
    if (text ~ /actual reusable|request confirmation|change, disable|settings actions/) {
      return "public_contact_real_core"
    }
    if (text ~ /file sending|voice messages|delivery receipts|disappearing-message/) {
      return "messaging_feature_real_core"
    }
    if (text ~ /safety banner/) {
      return "conversation_safety_real_core"
    }
    if (text ~ /real core|valid one-time|scannable|reusable public/) {
      return "real_core_functional"
    }

    return "manual_followup"
  }

  /^## / {
    section = substr($0, 4)
    next
  }
  /^- \[ \] / {
    item = substr($0, 7)
    counts[blocker_group(section, item)]++
  }
  END {
    for (name in counts) {
      print "[INFO] " name ": " counts[name]
    }
  }
' "$checklist" | sort

echo
echo "Unchecked items by section:"
awk '
  /^## / {
    section = substr($0, 4)
    next
  }
  /^- \[ \] / {
    if (section == "") {
      section = "Unsectioned"
    }
    counts[section]++
  }
  END {
    for (name in counts) {
      print "[INFO] " name ": " counts[name]
    }
  }
' "$checklist" | sort

echo
echo "Unchecked items:"
awk '
  /^## / {
    section = substr($0, 4)
    next
  }
  /^- \[ \] / {
    item = substr($0, 7)
    printf("[INFO] line %s | %s | %s\n", NR, section, item)
  }
' "$checklist"

exit 1
