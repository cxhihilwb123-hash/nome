#!/usr/bin/env python3

"""Convert every Mach-O member in a Darwin static archive to iOS Simulator.

Apple's vtool understands both legacy LC_VERSION_MIN_IPHONEOS commands and
modern LC_BUILD_VERSION commands. Static archives must be unpacked first, and
archive members need unique temporary names because Haskell archives commonly
contain repeated basenames from different modules.
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


AR_MAGIC = b"!<arch>\n"
MACHO_MAGICS = {
    b"\xcf\xfa\xed\xfe",  # MH_MAGIC_64, little endian
    b"\xfe\xed\xfa\xcf",  # MH_CIGAM_64
    b"\xce\xfa\xed\xfe",  # MH_MAGIC, little endian
    b"\xfe\xed\xfa\xce",  # MH_CIGAM
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert a Darwin .a archive to arm64 iOS Simulator platform metadata."
    )
    parser.add_argument("input", type=Path, help="Input Darwin static archive")
    parser.add_argument("output", type=Path, help="Output iOS Simulator archive")
    parser.add_argument("--minimum-os", default="15.0", help="Minimum iOS Simulator version")
    parser.add_argument("--sdk", required=True, help="iOS Simulator SDK version")
    parser.add_argument(
        "--developer-dir",
        type=Path,
        default=Path("/Applications/Xcode.app/Contents/Developer"),
        help="Full Xcode developer directory",
    )
    return parser.parse_args()


def run(command: list[str], *, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        command,
        check=True,
        env=env,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )


def archive_members(archive: Path) -> list[tuple[str, bytes]]:
    data = archive.read_bytes()
    if not data.startswith(AR_MAGIC):
        raise ValueError(f"not a Darwin static archive: {archive}")

    members: list[tuple[str, bytes]] = []
    offset = len(AR_MAGIC)
    while offset < len(data):
        if offset + 60 > len(data):
            raise ValueError(f"truncated archive header at byte {offset}: {archive}")

        header = data[offset : offset + 60]
        if header[58:60] != b"`\n":
            raise ValueError(f"invalid archive member header at byte {offset}: {archive}")

        try:
            payload_size = int(header[48:58].decode("ascii").strip())
        except ValueError as error:
            raise ValueError(f"invalid archive member size at byte {offset}: {archive}") from error

        payload_start = offset + 60
        payload_end = payload_start + payload_size
        if payload_end > len(data):
            raise ValueError(f"truncated archive member at byte {offset}: {archive}")

        raw_name = header[0:16].decode("utf-8", errors="replace").rstrip()
        payload = data[payload_start:payload_end]
        if raw_name.startswith("#1/"):
            name_length = int(raw_name[3:])
            if name_length > len(payload):
                raise ValueError(f"invalid extended member name at byte {offset}: {archive}")
            name = payload[:name_length].rstrip(b"\0").decode("utf-8", errors="replace")
            payload = payload[name_length:]
        else:
            name = raw_name.rstrip("/")

        if name and not name.startswith("__.SYMDEF") and name not in {"/", "//"}:
            members.append((name, payload))

        offset = payload_end + (payload_size % 2)

    if not members:
        raise ValueError(f"archive has no object members: {archive}")
    return members


def convert_member(
    source: Path,
    destination: Path,
    *,
    minimum_os: str,
    sdk: str,
    env: dict[str, str],
) -> None:
    with source.open("rb") as source_file:
        magic = source_file.read(4)
    if magic not in MACHO_MAGICS:
        raise ValueError(f"archive member is not Mach-O: {source.name}")

    run(
        [
            "/usr/bin/xcrun",
            "vtool",
            "-set-build-version",
            "iossim",
            minimum_os,
            sdk,
            "-replace",
            "-output",
            str(destination),
            str(source),
        ],
        env=env,
    )
    inspection = run(["/usr/bin/xcrun", "vtool", "-show-build", str(destination)], env=env)
    if "platform IOSSIMULATOR" not in inspection.stdout:
        raise ValueError(f"vtool did not set IOSSIMULATOR for {source.name}")


def main() -> int:
    args = parse_args()
    input_archive = args.input.resolve()
    output_archive = args.output.resolve()
    if not input_archive.is_file():
        raise SystemExit(f"[FAIL] input archive does not exist: {input_archive}")
    if output_archive.exists():
        raise SystemExit(f"[FAIL] output already exists: {output_archive}")
    if input_archive == output_archive:
        raise SystemExit("[FAIL] input and output must be different paths")
    if not args.developer_dir.is_dir():
        raise SystemExit(f"[FAIL] Xcode developer directory does not exist: {args.developer_dir}")

    env = os.environ.copy()
    env["DEVELOPER_DIR"] = str(args.developer_dir)
    output_archive.parent.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory(prefix="nome-ios-archive-convert-") as temp_name:
        temp_dir = Path(temp_name)
        converted_files: list[Path] = []
        members = archive_members(input_archive)

        for index, (member_name, payload) in enumerate(members):
            safe_suffix = Path(member_name).suffix or ".o"
            source = temp_dir / f"member-{index:06d}-source{safe_suffix}"
            destination = temp_dir / f"member-{index:06d}-sim{safe_suffix}"
            source.write_bytes(payload)
            convert_member(
                source,
                destination,
                minimum_os=args.minimum_os,
                sdk=args.sdk,
                env=env,
            )
            converted_files.append(destination)

        file_list = temp_dir / "members.txt"
        file_list.write_text("".join(f"{path}\n" for path in converted_files), encoding="utf-8")
        staged_output = temp_dir / output_archive.name
        run(
            [
                "/usr/bin/libtool",
                "-static",
                "-D",
                "-filelist",
                str(file_list),
                "-o",
                str(staged_output),
            ],
            env=env,
        )
        shutil.move(staged_output, output_archive)

    print(f"[PASS] Converted {len(members)} members to IOSSIMULATOR: {output_archive}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
