#!/usr/bin/env python3

"""Regression tests for the Darwin archive parser used by the simulator converter."""

from __future__ import annotations

import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest


SCRIPT_PATH = Path(__file__).with_name("convert-darwin-archive-to-ios-simulator.py")
sys.dont_write_bytecode = True
SPEC = importlib.util.spec_from_file_location("darwin_archive_converter", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"could not load converter module: {SCRIPT_PATH}")
CONVERTER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CONVERTER)


def archive_member(name: str, payload: bytes, *, extended: bool = False) -> bytes:
    if extended:
        encoded_name = name.encode("utf-8")
        raw_name = f"#1/{len(encoded_name)}"
        body = encoded_name + payload
    else:
        raw_name = f"{name}/"
        body = payload

    header = (
        f"{raw_name:<16}"
        f"{0:<12}"
        f"{0:<6}"
        f"{0:<6}"
        f"{0o100644:<8o}"
        f"{len(body):<10}"
        "`\n"
    ).encode("ascii")
    return header + body + (b"\n" if len(body) % 2 else b"")


class ArchiveMembersTests(unittest.TestCase):
    def write_archive(self, body: bytes) -> Path:
        temp_dir = tempfile.TemporaryDirectory(prefix="nome-archive-parser-test-")
        self.addCleanup(temp_dir.cleanup)
        archive = Path(temp_dir.name) / "fixture.a"
        archive.write_bytes(body)
        return archive

    def test_preserves_duplicate_members_and_extended_names(self) -> None:
        archive = self.write_archive(
            CONVERTER.AR_MAGIC
            + archive_member("__.SYMDEF", b"index")
            + archive_member("duplicate.o", b"first")
            + archive_member("duplicate.o", b"second")
            + archive_member("a-very-long-object-member-name.o", b"third", extended=True)
        )

        self.assertEqual(
            CONVERTER.archive_members(archive),
            [
                ("duplicate.o", b"first"),
                ("duplicate.o", b"second"),
                ("a-very-long-object-member-name.o", b"third"),
            ],
        )

    def test_rejects_non_archive_input(self) -> None:
        archive = self.write_archive(b"not an archive")
        with self.assertRaisesRegex(ValueError, "not a Darwin static archive"):
            CONVERTER.archive_members(archive)

    def test_rejects_truncated_header(self) -> None:
        archive = self.write_archive(CONVERTER.AR_MAGIC + b"truncated")
        with self.assertRaisesRegex(ValueError, "truncated archive header"):
            CONVERTER.archive_members(archive)

    def test_rejects_archive_without_object_members(self) -> None:
        archive = self.write_archive(
            CONVERTER.AR_MAGIC + archive_member("__.SYMDEF", b"index")
        )
        with self.assertRaisesRegex(ValueError, "archive has no object members"):
            CONVERTER.archive_members(archive)


class ConvertMemberTests(unittest.TestCase):
    def test_rejects_non_macho_before_running_xcode_tools(self) -> None:
        with tempfile.TemporaryDirectory(prefix="nome-convert-member-test-") as temp_name:
            source = Path(temp_name) / "input.o"
            destination = Path(temp_name) / "output.o"
            source.write_bytes(b"not-mach-o")

            with self.assertRaisesRegex(ValueError, "archive member is not Mach-O"):
                CONVERTER.convert_member(
                    source,
                    destination,
                    minimum_os="15.0",
                    sdk="26.5",
                    env={},
                )
            self.assertFalse(destination.exists())


if __name__ == "__main__":
    unittest.main()
